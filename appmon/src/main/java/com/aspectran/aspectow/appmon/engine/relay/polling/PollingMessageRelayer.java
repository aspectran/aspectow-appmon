/*
 * Copyright (c) 2020-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aspectran.aspectow.appmon.engine.relay.polling;

import com.aspectran.aspectow.appmon.engine.config.PollingConfig;
import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.aspectow.appmon.engine.relay.CommandOptions;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayer;
import com.aspectran.aspectow.appmon.engine.relay.RelaySession;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.core.component.bean.annotation.Qualifier;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_ESTABLISHED;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_FOCUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_LOAD_PREVIOUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_REFRESH;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * An {@link MessageRelayer} implementation based on HTTP long-polling.
 * Clients connect to subscribe, then periodically pull for new messages.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
@Component(NODES_BASE_PATH + "/${nodeId}/appmon")
public class PollingMessageRelayer implements MessageRelayer {

    private static final Logger logger = LoggerFactory.getLogger(PollingMessageRelayer.class);

    private final AppMonManager appMonManager;

    private final MessageRelayManager messageRelayManager;

    private final PollingSessionManager pollingSessionManager;

    @Autowired
    public PollingMessageRelayer(@NonNull AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
        this.messageRelayManager = appMonManager.getMessageRelayManager();
        this.pollingSessionManager = new PollingSessionManager(appMonManager);
    }

    /**
     * Initializes the service by registering it with the {@link MessageRelayManager}.
     */
    @Initialize
    public void initialize() throws Exception {
        pollingSessionManager.initialize();
    }

    /**
     * Destroys the service, cleaning up resources and unregistering from the manager.
     */
    @Destroy
    public void destroy() throws Exception {
        pollingSessionManager.destroy();
    }

    /**
     * Allows a client to subscribe and start a polling session.
     * @param translet the current translet
     * @return a map containing the app info, and initial messages
     * @throws IOException if an I/O error occurs
     */
    @Request("/polling/subscribe")
    @Transform(FormatType.JSON)
    public Map<String, Object> subscribe(@NonNull Translet translet) throws IOException {
        String nodeId = translet.getParameter("nodeId");
        Assert.hasText(nodeId, "Node ID cannot be empty");
        String nodeToSubscribe = translet.getParameter("nodeToSubscribe");
        if (messageRelayManager.isSameNode(nodeId) || StringUtils.hasText(nodeToSubscribe)) {
            String appsToSubscribe = translet.getParameter("appsToSubscribe");
            String[] appIds = StringUtils.splitWithComma(appsToSubscribe);
            appIds = appMonManager.getVerifiedAppIds(appIds, appMonManager.getClusterAppInfoList());

            PollingRelaySession relaySession = pollingSessionManager.createSession(translet, appIds);
            String timeZone = translet.getParameter("timeZone");
            if (StringUtils.hasText(timeZone)) {
                relaySession.setTimeZone(timeZone);
            }
            messageRelayManager.registerSession(relaySession.getId(), this);
            return Map.of(
                    "appsToSubscribe", StringUtils.join(appIds, ","),
                    "pollingInterval", relaySession.getPollingInterval(),
                    "nodeId", nodeId,
                    "primary", true,
                    "alive", true
            );
        } else if (messageRelayManager.isGatewayMode()) {
            return Map.of(
                    "nodeId", nodeId,
                    "primary", false,
                    "alive", messageRelayManager.getNodeRegistry().isFound(nodeId)
            );
        } else {
            return null;
        }
    }

    /**
     * Allows a client to pull new messages from the server.
     * @param translet the current translet
     * @param commands an array of commands from the client
     * @return a map containing the new token and any new messages
     * @throws IOException if an I/O error occurs
     */
    @Request("/polling/pull")
    @Transform(FormatType.JSON)
    public Map<String, Object> pull(
            @NonNull Translet translet, @Qualifier("commands[]") String[] commands) throws IOException {
        PollingRelaySession relaySession = pollingSessionManager.getSession(translet);
        if (relaySession == null || !relaySession.isValid()) {
            return null;
        }

        if (commands != null) {
            for (String command : commands) {
                handleCommand(relaySession, command);
            }
        }

        String[] messages = pollingSessionManager.pull(relaySession);
        return Map.of(
                "messages", (messages != null ? messages : new String[0])
        );
    }

    private void handleCommand(PollingRelaySession relaySession, String command) {
        CommandOptions commandOptions = new CommandOptions();
        try {
            commandOptions.parseCommand(command);
        } catch (Exception e) {
            logger.error("Failed to parse command: {}", command, e);
            return;
        }
        switch (commandOptions.getCommand()) {
            case COMMAND_ESTABLISHED:
                established(relaySession, commandOptions);
                break;
            case COMMAND_REFRESH:
            case COMMAND_LOAD_PREVIOUS:
                refreshData(relaySession, commandOptions);
                break;
            case COMMAND_FOCUS:
                focus(relaySession, commandOptions);
                break;
        }
    }

    private void established(@NonNull PollingRelaySession relaySession, @NonNull CommandOptions commandOptions) {
        String nodeId = commandOptions.getNodeId();
        Assert.hasText(nodeId, "Node ID cannot be empty");
        String nodeToSubscribe = commandOptions.getNodeToSubscribe();
        if (messageRelayManager.isSameNode(nodeId) || StringUtils.hasText(nodeToSubscribe)) {
            boolean singleNodeDesignated = (!messageRelayManager.isSameNode(nodeId) && StringUtils.hasText(nodeToSubscribe));
            if (messageRelayManager.subscribe(relaySession, nodeId, singleNodeDesignated)) {
                if (messageRelayManager.isSameNode(nodeId)) {
                    List<String> messages = messageRelayManager.getLastMessages(relaySession);
                    for (String message : messages) {
                        pollingSessionManager.push(relaySession, message);
                    }
                }
            }
        }
    }

    private void focus(@NonNull PollingRelaySession relaySession, @NonNull CommandOptions commandOptions) {
        String nodeId = commandOptions.getNodeId();
        if (messageRelayManager.isSameNode(nodeId)) {
            String focusedAppId = commandOptions.getAppId();
            relaySession.setFocusedAppId(focusedAppId);
        }
    }

    private void refreshData(@NonNull PollingRelaySession relaySession, @NonNull CommandOptions commandOptions) {
        List<String> newMessages = messageRelayManager.refreshData(relaySession, commandOptions);
        if (newMessages != null) {
            for (String message : newMessages) {
                pollingSessionManager.push(relaySession, message);
            }
        }
    }

    /**
     * Adjusts the polling interval for a client session.
     * @param translet the current translet
     * @param speed the desired speed multiplier (1 for fast)
     * @return a map containing the new polling interval
     */
    @Request("/polling/interval")
    @Transform(FormatType.JSON)
    public Map<String, Object> pollingInterval(@NonNull Translet translet, int speed) {
        PollingRelaySession relaySession = pollingSessionManager.getSession(translet);
        if (relaySession == null) {
            return null;
        }

        if (speed == 1) {
            relaySession.setPollingInterval(1000);
        } else {
            PollingConfig pollingConfig = appMonManager.getPollingConfig();
            relaySession.setPollingInterval(pollingConfig.getPollingInterval());
        }

        return Map.of(
            "pollingInterval", relaySession.getPollingInterval()
        );
    }

    @Override
    public RelaySession findRelaySession(String sessionId) {
        return pollingSessionManager.getSession(sessionId);
    }

    @Override
    public void relay(String message) {
        pollingSessionManager.push(message);
    }

    @Override
    public void relay(RelaySession relaySession, String message) {
        pollingSessionManager.push(relaySession, message);
    }

}

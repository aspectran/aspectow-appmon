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
package com.aspectran.aspectow.appmon.engine.relay.websocket;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.aspectow.appmon.engine.relay.CommandOptions;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayer;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager;
import com.aspectran.aspectow.appmon.engine.relay.RelaySession;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.websocket.jsr356.AspectranConfigurator;
import com.aspectran.web.websocket.jsr356.SimplifiedEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_ESTABLISHED;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_FOCUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_JOIN;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_LOAD_PREVIOUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_PING;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_REFRESH;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * An {@link MessageRelayer} implementation based on the WebSocket protocol (JSR-356).
 * It provides real-time, bidirectional communication with clients.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
@Component
@ServerEndpoint(
        value = NODES_BASE_PATH + "/{nodeId}/appmon/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class WebsocketMessageRelayer extends SimplifiedEndpoint implements MessageRelayer {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketMessageRelayer.class);

    private static final String RESPONSE_PONG = "pong:";
    private static final String RESPONSE_JOINED = "joined:";

    private final AppMonManager appMonManager;

    private final MessageRelayManager messageRelayManager;

    @Autowired
    public WebsocketMessageRelayer(@NonNull AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
        this.messageRelayManager = appMonManager.getMessageRelayManager();
    }

    /**
     * Initializes the service by registering it with the {@link MessageRelayManager}.
     */
    @Initialize
    public void registerRelayer() {
        messageRelayManager.addRelayer(this);
    }

    /**
     * Destroys the service, unregistering from the manager.
     */
    @Destroy
    public void destroy() throws Exception {
        messageRelayManager.removeRelayer(this);
    }

    @Override
    protected boolean checkAuthorized(@NonNull Session session) {
        String token = session.getPathParameters().get("token");
        try {
            AppMonTokenIssuer.validateToken(token);
        } catch (InvalidPBTokenException e) {
            logger.error("Invalid token: {}", token);
            return false;
        }
        return true;
    }

    @Override
    protected void registerMessageHandlers(@NonNull Session session) {
        if (session.getMessageHandlers().isEmpty()) {
            session.addMessageHandler(String.class, message -> {
                setLoggingGroup();
                handleMessage(session, message);
            });
        }
    }

    private void handleMessage(Session session, String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        CommandOptions commandOptions = new CommandOptions(message);
        switch (commandOptions.getCommand()) {
            case COMMAND_PING:
                pong(session);
                break;
            case COMMAND_JOIN:
                join(session, commandOptions);
                break;
            case COMMAND_ESTABLISHED:
                joinComplete(session, commandOptions);
                break;
            case COMMAND_REFRESH:
            case COMMAND_LOAD_PREVIOUS:
                refreshData(session, commandOptions);
                break;
            case COMMAND_FOCUS:
                focus(session, commandOptions);
                break;
        }
    }

    @Override
    protected void onSessionRemoved(Session session) {
        RelaySession relaySession = new WebsocketRelaySession(session);
        messageRelayManager.unsubscribe(relaySession);
    }

    private void pong(Session session) {
        String newToken = AppMonTokenIssuer.issueToken(1800); // 30 min.
        sendText(session, RESPONSE_PONG + newToken);
    }

    private void join(Session session, @NonNull CommandOptions commandOptions) {
        WebsocketRelaySession relaySession = new WebsocketRelaySession(session);
        String timeZone = commandOptions.getTimeZone();
        if (StringUtils.hasText(timeZone)) {
            relaySession.setTimeZone(timeZone);
        }
        String appsToJoin = commandOptions.getAppsToJoin();
        String[] appIds = StringUtils.splitWithComma(appsToJoin);
        appIds = appMonManager.getVerifiedAppIds(appIds);
        if (!StringUtils.hasText(appsToJoin) || appIds.length > 0) {
            relaySession.setJoinedApps(appIds);
        }
        if (addSession(session)) {
            if (appMonManager.isGatewayMode()) {
                relay(relaySession, appMonManager.getNodeId() + ":" + RESPONSE_JOINED + session.getId());
            } else {
                relay(relaySession, RESPONSE_JOINED);
            }
        }
    }

    private void joinComplete(@NonNull Session session, @NonNull CommandOptions commandOptions) {
        String targetNodeId = commandOptions.getNodeId();
        RelaySession relaySession = new WebsocketRelaySession(session);
        messageRelayManager.subscribe(relaySession, targetNodeId);
        if (targetNodeId == null || appMonManager.getNodeId().equals(targetNodeId)) {
            List<String> messages = messageRelayManager.getLastMessages(relaySession);
            for (String message : messages) {
                sendText(session, message);
            }
        }
    }

    private void focus(@NonNull Session session, @NonNull CommandOptions commandOptions) {
        String focusedAppId = commandOptions.getAppId();
        RelaySession relaySession = new WebsocketRelaySession(session);
        relaySession.setFocusedAppId(focusedAppId);
    }

    private void refreshData(@NonNull Session session, @NonNull CommandOptions commandOptions) {
        RelaySession relaySession = new WebsocketRelaySession(session);
        List<String> messages = messageRelayManager.refreshData(relaySession, commandOptions);
        if (messages != null) {
            for (String message : messages) {
                sendText(session, message);
            }
        }
    }

    @Override
    public void relay(String message) {
        broadcast(message);
    }

    @Override
    public void relay(@NonNull RelaySession serviceSession, String message) {
        if (serviceSession instanceof WebsocketRelaySession session) {
            sendText(session.getSession(), message);
        }
    }

    @Override
    public RelaySession getLocalRelaySession(String sessionId) {
        Session session = findSession(sessionId);
        return (session != null ? new WebsocketRelaySession(session) : null);
    }

}

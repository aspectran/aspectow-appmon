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
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayer;
import com.aspectran.aspectow.appmon.engine.relay.RelaySession;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.utils.Assert;
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
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_LOAD_PREVIOUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_PING;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_REFRESH;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_SUBSCRIBE;
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
    private static final String RESPONSE_SUBSCRIBED = "subscribed:";

    private final AppMonManager appMonManager;

    private final MessageRelayManager messageRelayManager;

    @Autowired
    public WebsocketMessageRelayer(@NonNull AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
        this.messageRelayManager = appMonManager.getMessageRelayManager();
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
        CommandOptions commandOptions = new CommandOptions();
        try {
            commandOptions.parseCommand(message);
        } catch (Exception e) {
            logger.error("Failed to parse message: {}", message, e);
            return;
        }
        switch (commandOptions.getCommand()) {
            case COMMAND_PING:
                pong(session);
                break;
            case COMMAND_SUBSCRIBE:
                subscribe(session, commandOptions);
                break;
            case COMMAND_ESTABLISHED:
                established(session, commandOptions);
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
    protected void onSessionRemoved(@NonNull Session session) {
        messageRelayManager.unregisterSession(session.getId());
        RelaySession relaySession = new WebsocketRelaySession(session);
        messageRelayManager.unsubscribe(relaySession);
    }

    private void pong(Session session) {
        String newToken = AppMonTokenIssuer.issueToken(1800); // 30 min.
        sendText(session, appMonManager.getNodeId() + "::" + RESPONSE_PONG + newToken);
    }

    private void subscribe(Session session, @NonNull CommandOptions commandOptions) {
        String nodeId = commandOptions.getNodeId();
        Assert.hasText(nodeId, "Node ID cannot be empty");
        String nodeToSubscribe = commandOptions.getNodeToSubscribe();
        String appsToSubscribe = commandOptions.getAppsToSubscribe();
        boolean isExplicitNode = StringUtils.hasText(nodeToSubscribe);
        if (messageRelayManager.isSameNode(nodeId) || isExplicitNode || StringUtils.hasText(appsToSubscribe)) {
            if (addSession(session)) {
                messageRelayManager.registerSession(session.getId(), this);
                WebsocketRelaySession relaySession = new WebsocketRelaySession(session);
                if (isExplicitNode) {
                    relaySession.setSubscribedNodeId(nodeToSubscribe);
                }
                String timeZone = commandOptions.getTimeZone();
                if (StringUtils.hasText(timeZone)) {
                    relaySession.setTimeZone(timeZone);
                }
                String[] appIds = StringUtils.splitWithComma(appsToSubscribe);
                appIds = appMonManager.getVerifiedAppIds(appIds, appMonManager.getClusterAppInfoList(), isExplicitNode);
                if (appIds.length > 0) {
                    relaySession.setSubscribedApps(appIds);
                }
                String alive = (!messageRelayManager.isGatewayMode() ||
                        messageRelayManager.getNodeRegistry().isFound(nodeId)) ? "alive" : "";
                relay(relaySession, nodeId + "::" + RESPONSE_SUBSCRIBED + "primary:" + alive);
            }
        } else if (messageRelayManager.isGatewayMode()) {
            String alive = messageRelayManager.getNodeRegistry().isFound(nodeId) ? "alive" : "";
            WebsocketRelaySession relaySession = new WebsocketRelaySession(session);
            relay(relaySession, nodeId + "::" + RESPONSE_SUBSCRIBED + alive);
        }
    }

    private void established(@NonNull Session session, @NonNull CommandOptions commandOptions) {
        String nodeId = commandOptions.getNodeId();
        Assert.hasText(nodeId, "Node ID cannot be empty");
        String nodeToSubscribe = commandOptions.getNodeToSubscribe();
        boolean isExplicitNode = StringUtils.hasText(nodeToSubscribe);
        if (messageRelayManager.isSameNode(nodeId) || isExplicitNode) {
            RelaySession relaySession = new WebsocketRelaySession(session);
            if (messageRelayManager.subscribe(relaySession, nodeId, isExplicitNode)) {
                if (messageRelayManager.isSameNode(nodeId)) {
                    List<String> messages = messageRelayManager.getLastMessages(relaySession);
                    for (String message : messages) {
                        sendText(session, message);
                    }
                }
            }
        }
    }

    private void focus(@NonNull Session session, @NonNull CommandOptions commandOptions) {
        String nodeId = commandOptions.getNodeId();
        if (messageRelayManager.isSameNode(nodeId)) {
            String focusedAppId = commandOptions.getAppId();
            RelaySession relaySession = new WebsocketRelaySession(session);
            relaySession.setFocusedAppId(focusedAppId);
        }
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
    public RelaySession findRelaySession(String sessionId) {
        Session session = findSession(sessionId);
        return (session != null ? new WebsocketRelaySession(session) : null);
    }

    @Override
    public void relay(String message) {
        broadcast(message);
    }

    @Override
    public void relay(@NonNull RelaySession relaySession, String message) {
        if (relaySession instanceof WebsocketRelaySession wrappedSession) {
            sendText(wrappedSession.getSession(), message);
        }
    }

}

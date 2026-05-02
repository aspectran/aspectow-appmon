/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.console.commands.bridge.websocket;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.console.commands.bridge.CommandBridge;
import com.aspectran.aspectow.console.commands.bridge.CommandSession;
import com.aspectran.aspectow.console.commands.bridge.RemoteCommandParameters;
import com.aspectran.aspectow.console.commands.bridge.RemoteCommandResultParameters;
import com.aspectran.aspectow.console.commands.manager.RemoteCommandManager;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.apon.JsonToParameters;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.websocket.jsr356.AspectranConfigurator;
import com.aspectran.web.websocket.jsr356.SimplifiedEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebsocketCommandBridge provides a WebSocket endpoint for real-time
 * remote command result delivery.
 */
@Component
@ServerEndpoint(
        value = "/remote-commands/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class WebsocketCommandBridge extends SimplifiedEndpoint implements CommandBridge {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketCommandBridge.class);

    private final RemoteCommandManager remoteCommandManager;

    private final NodeManager nodeManager;

    @Autowired
    public WebsocketCommandBridge(RemoteCommandManager remoteCommandManager, NodeManager nodeManager) {
        this.remoteCommandManager = remoteCommandManager;
        this.nodeManager = nodeManager;
    }

    @Initialize
    public void register() {
        if (remoteCommandManager.getBroker() != null) {
            remoteCommandManager.getBroker().addBridge(this);
            logger.info("WebsocketCommandBridge registered with CommandBroker");
        }
    }

    @Override
    protected boolean checkAuthorized(@NonNull Session session) {
        String token = session.getPathParameters().get("token");
        try {
            AppMonTokenIssuer.validateToken(token);
            return true;
        } catch (InvalidPBTokenException e) {
            logger.warn("WebSocket connection rejected: invalid or expired token");
            return false;
        }
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

        try {
            RemoteCommandParameters parameters = JsonToParameters.from(message, RemoteCommandParameters.class);

            String header = parameters.getHeader();
            if ("execute".equals(header)) {
                execute(session, parameters);
            } else if ("join".equals(header)) {
                join(session);
            } else if ("ping".equals(header)) {
                pong(session);
            }
        } catch (Exception e) {
            logger.error("Failed to parse incoming remote command message: {}", message, e);
            sendText(session, "[ERROR] Invalid message format");
        }
    }

    private void join(Session session) {
        WebsocketCommandSession commandSession = new WebsocketCommandSession(session);
        commandSession.setNodeId(nodeManager.getNodeId());
        if (addSession(session)) {
            remoteCommandManager.getBroker().join(commandSession);
            RemoteCommandResultParameters resultParameters = new RemoteCommandResultParameters()
                    .setHeader("joined")
                    .setNodeId(nodeManager.getNodeId());
            sendText(session, resultParameters.toString());
            logger.debug("ConsoleClient joined remote command management: session {}", session.getId());
        }
    }

    private void pong(Session session) {
        RemoteCommandResultParameters resultParameters = new RemoteCommandResultParameters()
                .setHeader("pong");
        sendText(session, resultParameters.toString());
    }

    private void execute(Session session, @NonNull RemoteCommandParameters messageParameters) {
        CommandParameters commandParameters = messageParameters.getCommandParameters();
        if (commandParameters != null) {
            String targetNodeId = messageParameters.getTargetNodeId();
            if (targetNodeId == null || targetNodeId.isEmpty()) {
                targetNodeId = nodeManager.getNodeId();
            }

            final String finalTargetNodeId = targetNodeId;
            try {
                Thread.ofVirtual().start(() -> {
                    try {
                        remoteCommandManager.dispatch(finalTargetNodeId, commandParameters.toString());
                    } catch (Exception e) {
                        logger.error("Failed to execute command from session {}", session.getId(), e);
                        sendText(session, "[ERROR] " + e.getMessage());
                    }
                });
                logger.debug("Command execution initiated from session {}: target={}, command={}",
                        session.getId(), finalTargetNodeId, commandParameters.getCommandName());
            } catch (Exception e) {
                logger.error("Failed to initiate command execution from session {}", session.getId(), e);
                sendText(session, "[ERROR] " + e.getMessage());
            }
        }
    }

    @Override
    protected void onSessionRemoved(@NonNull Session session) {
        WebsocketCommandSession commandSession = new WebsocketCommandSession(session);
        remoteCommandManager.getBroker().release(commandSession);
        logger.debug("Remote command WebSocket session removed: {} (Total: {})", session.getId(), countSessions());
    }

    @Override
    public void bridge(String data) {
        if (data != null) {
            RemoteCommandResultParameters resultParameters = new RemoteCommandResultParameters()
                    .setHeader("result")
                    .setNodeId(nodeManager.getNodeId())
                    .setResult(data);
            broadcast(resultParameters.toString());
        }
    }

    @Override
    public void bridge(@NonNull CommandSession session, String data) {
        if (session instanceof WebsocketCommandSession websocketCommandSession) {
            RemoteCommandResultParameters resultParameters = new RemoteCommandResultParameters()
                    .setHeader("result")
                    .setNodeId(nodeManager.getNodeId())
                    .setResult(data);
            sendText(websocketCommandSession.getSession(), resultParameters.toString());
        }
    }

}

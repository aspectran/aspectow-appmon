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
package com.aspectran.aspectow.console.commands.bridge.websocket;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.node.management.commands.CommandRequestParameters;
import com.aspectran.aspectow.node.management.commands.CommandResponseParameters;
import com.aspectran.aspectow.node.management.commands.RemoteCommandManager;
import com.aspectran.aspectow.node.management.commands.bridge.CommandBridge;
import com.aspectran.aspectow.node.management.commands.bridge.CommandSession;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.apon.JsonToParameters;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.websocket.jsr356.AspectranConfigurator;
import com.aspectran.web.websocket.jsr356.SimplifiedEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.aspectran.aspectow.node.management.commands.bridge.CommandBroker.CATEGORY_COMMANDS;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * WebsocketCommandBridge provides a WebSocket endpoint for real-time
 * remote command result delivery.
 */
@Component
@ServerEndpoint(
        value = NODES_BASE_PATH + "/{nodeId}/" + CATEGORY_COMMANDS + "/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class WebsocketCommandBridge extends SimplifiedEndpoint implements CommandBridge {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketCommandBridge.class);

    private final RemoteCommandManager remoteCommandManager;

    /**
     * Constructs a new {@code WebsocketCommandBridge} with the specified remote
     * command manager and node manager.
     * @param remoteCommandManager the manager for executing remote commands
     */
    @Autowired
    public WebsocketCommandBridge(RemoteCommandManager remoteCommandManager) {
        this.remoteCommandManager = remoteCommandManager;
    }

    /**
     * Checks if the WebSocket connection is authorized by validating the token
     * passed in the path parameters.
     * @param session the WebSocket session
     * @return {@code true} if authorized; {@code false} otherwise
     */
    @Override
    protected boolean checkAuthorized(@NonNull Session session) {
        String token = session.getPathParameters().get("token");
        try {
            AppMonTokenIssuer.validateToken(token);
            boolean isDemo = AppMonTokenIssuer.isDemoToken(token);
            session.getUserProperties().put("isDemo", isDemo);
            return true;
        } catch (InvalidPBTokenException e) {
            logger.warn("WebSocket connection rejected: invalid or expired token");
            return false;
        }
    }

    /**
     * Registers message handlers for the WebSocket session to process incoming
     * text messages from the client.
     * @param session the WebSocket session
     */
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
            CommandRequestParameters request = JsonToParameters.from(message, CommandRequestParameters.class);

            String header = request.getHeader();
            if ("execute".equals(header)) {
                execute(session, request);
            } else if ("subscribe".equals(header)) {
                subscribe(session, request);
            } else if ("ping".equals(header)) {
                pong(session);
            }
        } catch (Exception e) {
            logger.error("Failed to parse incoming remote command message: {}", message, e);
            CommandResponseParameters response = new CommandResponseParameters()
                    .setError("Invalid request format");
            sendText(session, response.toString());
        }
    }

    /**
     * Handles clean up tasks when a WebSocket session is removed, including
     * unregistering the session and unsubscribing from the command broker.
     * @param session the removed WebSocket session
     */
    @Override
    protected void onSessionRemoved(@NonNull Session session) {
        remoteCommandManager.unregisterSession(session.getId());
        WebsocketCommandSession commandSession = new WebsocketCommandSession(session);
        remoteCommandManager.getBroker().unsubscribe(commandSession);
        if (logger.isDebugEnabled()) {
            logger.debug("Remote command WebSocket session removed: {} (Total: {})", session.getId(), countSessions());
        }
    }

    private void subscribe(Session session, @NonNull CommandRequestParameters request) {
        String targetNodeId = request.getTargetNodeId();
        if (!StringUtils.hasText(targetNodeId)) {
            CommandResponseParameters response = new CommandResponseParameters()
                    .setError("Target node is required");
            sendText(session, response.toString());
            return;
        }

        WebsocketCommandSession commandSession = new WebsocketCommandSession(session);
        commandSession.setNodeId(targetNodeId);

        if (addSession(session)) {
            remoteCommandManager.registerSession(session.getId(), this);
            remoteCommandManager.getBroker().subscribe(commandSession);
            CommandResponseParameters response = new CommandResponseParameters()
                    .setHeader("subscribed")
                    .setNodeId(remoteCommandManager.getNodeId());
            sendText(session, response.toString());
            if (logger.isDebugEnabled()) {
                logger.debug("ConsoleClient joined remote command management: session {}, targetNodeId: {}",
                        session.getId(), commandSession.getNodeId());
            }
        }
    }

    private void pong(Session session) {
        CommandResponseParameters response = new CommandResponseParameters()
                .setHeader("pong");
        sendText(session, response.toString());
    }

    private void execute(Session session, @NonNull CommandRequestParameters request) {
        CommandParameters commandParameters = request.getCommand();
        if (commandParameters != null) {
            Boolean isDemo = (Boolean) session.getUserProperties().get("isDemo");
            if (isDemo != null && isDemo) {
                String commandName = commandParameters.getString("command");
                String transletName = commandParameters.getString("translet");

                if (!"sysinfo".equals(commandName) && !"translet".equals(commandName)) {
                    CommandResponseParameters response = new CommandResponseParameters()
                            .setError("Only 'sysinfo' and 'translet' commands are allowed in the demo environment.");
                    sendText(session, response.toString());
                    return;
                }

                if ("translet".equals(commandName) && !"sample/commands/hello".equals(transletName)) {
                    CommandResponseParameters response = new CommandResponseParameters()
                            .setError("Only 'sample/commands/hello' translet can be executed in the demo environment.");
                    sendText(session, response.toString());
                    return;
                }
            }
            request.setSessionId(session.getId());
            try {
                remoteCommandManager.process(request);
                if (logger.isDebugEnabled()) {
                    logger.debug(ToStringBuilder.toString("Command execution initiated:", request));
                }
            } catch (Exception e) {
                logger.error("Failed to initiate command execution from session {}", session.getId(), e);
                CommandResponseParameters response = new CommandResponseParameters()
                        .setError("Failed to initiate command execution");
                sendText(session, response.toString());
            }
        }
    }

    /**
     * Finds a command session associated with the given session ID.
     * @param sessionId the session ID to locate
     * @return the command session, or {@code null} if not found
     */
    @Override
    public CommandSession findCommandSession(String sessionId) {
        Session session = findSession(sessionId);
        return (session != null ? new WebsocketCommandSession(session) : null);
    }

    /**
     * Broadcasts a command message to all connected sessions.
     * @param message the message to broadcast
     */
    @Override
    public void bridge(String message) {
        if (message != null) {
            broadcast(message);
        }
    }

    /**
     * Bridges a command message to a specific command session.
     * @param session the command session to receive the message
     * @param message the message to send
     */
    @Override
    public void bridge(@NonNull CommandSession session, String message) {
        if (message != null && session instanceof WebsocketCommandSession websocketCommandSession) {
            sendText(websocketCommandSession.getSession(), message);
        }
    }

}

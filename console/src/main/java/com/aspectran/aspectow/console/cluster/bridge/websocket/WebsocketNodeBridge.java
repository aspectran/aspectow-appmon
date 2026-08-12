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
package com.aspectran.aspectow.console.cluster.bridge.websocket;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.node.management.nodes.NodeRequestParameters;
import com.aspectran.aspectow.node.management.nodes.NodeResponseParameters;
import com.aspectran.aspectow.node.management.nodes.RemoteNodeManager;
import com.aspectran.aspectow.node.management.nodes.bridge.NodeBridge;
import com.aspectran.aspectow.node.management.nodes.bridge.NodeSession;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
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

import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * WebsocketNodeBridge provides real-time, bidirectional communication for cluster node management.
 *
 * <p>Created: 2026-04-19</p>
 */
@Component
@ServerEndpoint(
        value = NODES_BASE_PATH + "/{nodeId}/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class WebsocketNodeBridge extends SimplifiedEndpoint implements NodeBridge {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketNodeBridge.class);

    private final RemoteNodeManager remoteNodeManager;

    /**
     * Constructs a new {@code WebsocketNodeBridge} with the specified remote node manager and node manager.
     * @param remoteNodeManager the remote node manager
     */
    @Autowired
    public WebsocketNodeBridge(RemoteNodeManager remoteNodeManager) {
        this.remoteNodeManager = remoteNodeManager;
    }

    /**
     * Checks if the session is authorized using the path parameter token.
     * @param session the websocket session
     * @return true if authorized, false otherwise
     */
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

    /**
     * Registers the message handler to process incoming text messages from the session.
     * @param session the websocket session
     */
    @Override
    protected void registerMessageHandlers(@NonNull Session session) {
        if (session.getMessageHandlers().isEmpty()) {
            session.addMessageHandler(String.class, message -> handleMessage(session, message));
        }
    }

    /**
     * Handles clean up tasks when a session is removed.
     * @param session the websocket session that was removed
     */
    @Override
    protected void onSessionRemoved(@NonNull Session session) {
        remoteNodeManager.unregisterSession(session.getId());
        WebsocketNodeSession nodeSession = new WebsocketNodeSession(session);
        remoteNodeManager.getBroker().unsubscribe(nodeSession);
        if (logger.isDebugEnabled()) {
            logger.debug("Node management WebSocket session removed: {} (Total: {})", session.getId(), countSessions());
        }
    }

    private void handleMessage(Session session, String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }

        try {
            NodeRequestParameters request = JsonToParameters.from(message, NodeRequestParameters.class);

            String header = request.getHeader();
            if ("subscribe".equals(header)) {
                subscribe(session, request);
            } else if ("established".equals(header)) {
                established(session);
            } else if ("ping".equals(header)) {
                pong(session);
            }
        } catch (Exception e) {
            logger.error("Failed to parse incoming request message: {}", message, e);
            NodeResponseParameters response = new NodeResponseParameters()
                    .setError("Invalid request format");
            sendText(session, response.toString());
        }
    }

    private void pong(Session session) {
        NodeResponseParameters response = new NodeResponseParameters()
                .setHeader("pong");
        sendText(session, response.toString());
    }

    private void subscribe(Session session, @NonNull NodeRequestParameters request) {
        String targetNodeId = request.getTargetNodeId();
        if (!StringUtils.hasText(targetNodeId)) {
            NodeResponseParameters response = new NodeResponseParameters()
                    .setError("Target node is required");
            sendText(session, response.toString());
            return;
        }

        WebsocketNodeSession nodeSession = new WebsocketNodeSession(session);
        nodeSession.setNodeId(targetNodeId);

        if (addSession(session)) {
            remoteNodeManager.registerSession(session.getId(), this);
            remoteNodeManager.getBroker().subscribe(nodeSession);
            NodeResponseParameters response = new NodeResponseParameters()
                    .setHeader("subscribed")
                    .setNodeId(remoteNodeManager.getNodeId());
            sendText(session, response.toString());
            if (logger.isDebugEnabled()) {
                logger.debug("ConsoleClient joined node management: session {}, targetNodeId: {}",
                        session.getId(), nodeSession.getNodeId());
            }
        }
    }

    private void established(@NonNull Session session) {
        String nodeId = session.getPathParameters().get("nodeId");
        logger.info("Node management session established: {} (nodeId: {})", session.getId(), nodeId);
    }

    @Override
    public NodeSession findNodeSession(String sessionId) {
        Session session = findSession(sessionId);
        return (session != null ? new WebsocketNodeSession(session) : null);
    }

    @Override
    public void bridge(String message) {
        if (message != null) {
            broadcast(message);
        }
    }

    @Override
    public void bridge(@NonNull NodeSession session, String message) {
        if (message != null && session instanceof WebsocketNodeSession websocketNodeSession) {
            sendText(websocketNodeSession.getSession(), message);
        }
    }

}

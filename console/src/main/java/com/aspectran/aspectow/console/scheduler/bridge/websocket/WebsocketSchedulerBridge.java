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
package com.aspectran.aspectow.console.scheduler.bridge.websocket;

import com.aspectran.aspectow.console.auth.ConsoleTokenIssuer;
import com.aspectran.aspectow.node.management.scheduler.RemoteSchedulerManager;
import com.aspectran.aspectow.node.management.scheduler.SchedulerRequestParameters;
import com.aspectran.aspectow.node.management.scheduler.SchedulerResponseParameters;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBridge;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerSession;
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

import static com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBroker.CATEGORY_SCHEDULER;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * WebsocketSchedulerBridge provides a WebSocket endpoint for real-time
 * scheduler management.
 */
@Component
@ServerEndpoint(
        value = NODES_BASE_PATH + "/{nodeId}/" + CATEGORY_SCHEDULER + "/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class WebsocketSchedulerBridge extends SimplifiedEndpoint implements SchedulerBridge {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketSchedulerBridge.class);

    private final RemoteSchedulerManager remoteSchedulerManager;

    /**
     * Constructs a new {@code WebsocketSchedulerBridge} with the specified scheduler manager.
     * @param remoteSchedulerManager the scheduler manager to handle scheduler operations
     */
    @Autowired
    public WebsocketSchedulerBridge(RemoteSchedulerManager remoteSchedulerManager) {
        this.remoteSchedulerManager = remoteSchedulerManager;
    }

    /**
     * Checks if the session is authorized using the security token provided in the path parameters.
     * @param session the WebSocket session to authorize
     * @return {@code true} if authorized; {@code false} otherwise
     */
    @Override
    protected boolean checkAuthorized(@NonNull Session session) {
        String token = session.getPathParameters().get("token");
        try {
            ConsoleTokenIssuer.validateToken(token);
            return true;
        } catch (InvalidPBTokenException e) {
            logger.warn("Scheduler WebSocket connection rejected: invalid or expired token");
            return false;
        }
    }

    /**
     * Registers the message handler for incoming WebSocket messages on the session.
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
            SchedulerRequestParameters request = JsonToParameters.from(message, SchedulerRequestParameters.class);
            request.setNodeId(remoteSchedulerManager.getNodeId());
            request.setSessionId(session.getId());
            String header = request.getHeader();
            if ("execute".equals(header)) {
                execute(session, request);
            } else if ("subscribe".equals(header)) {
                subscribe(session, request);
            } else if ("established".equals(header)) {
                established(session);
            } else if ("ping".equals(header)) {
                pong(session);
            }
        } catch (Exception e) {
            logger.error("Failed to parse incoming scheduler management request: {}", message, e);
            SchedulerResponseParameters response = new SchedulerResponseParameters()
                    .setError("Invalid request format");
            sendText(session, response.toString());
        }
    }

    /**
     * Invoked when a WebSocket session is removed (closed or errored). Unregisters
     * the session from the scheduler manager and unsubscribes it.
     * @param session the WebSocket session being removed
     */
    @Override
    protected void onSessionRemoved(@NonNull Session session) {
        remoteSchedulerManager.unregisterSession(session.getId());
        WebsocketSchedulerSession schedulerSession = new WebsocketSchedulerSession(session);
        remoteSchedulerManager.getBroker().unsubscribe(schedulerSession);
        logger.debug("Scheduler WebSocket session removed: {}", session.getId());
    }

    private void subscribe(Session session, @NonNull SchedulerRequestParameters request) {
        String targetNodeId = request.getTargetNodeId();
        WebsocketSchedulerSession schedulerSession = new WebsocketSchedulerSession(session);
        if (StringUtils.hasText(targetNodeId)) {
            schedulerSession.setNodeId(targetNodeId);
        }

        addSession(session);
        remoteSchedulerManager.registerSession(session.getId(), this);

        SchedulerResponseParameters response = new SchedulerResponseParameters()
                .setHeader("subscribed")
                .setNodeId(remoteSchedulerManager.getNodeId());
        sendText(session, response.toString());

        remoteSchedulerManager.getBroker().subscribe(schedulerSession);

        if (logger.isDebugEnabled()) {
            logger.debug("ConsoleClient joined scheduler management: session {}, targetNodeId: {}",
                    session.getId(), schedulerSession.getNodeId());
        }
    }

    private void established(@NonNull Session session) {
        WebsocketSchedulerSession schedulerSession = new WebsocketSchedulerSession(session);
        remoteSchedulerManager.getBroker().subscribe(schedulerSession);
        if (logger.isDebugEnabled()) {
            logger.debug("Scheduler management session established: session {}", session.getId());
        }
    }

    private void pong(Session session) {
        SchedulerResponseParameters response = new SchedulerResponseParameters()
                .setHeader("pong");
        sendText(session, response.toString());
    }

    private void execute(Session session, @NonNull SchedulerRequestParameters request) {
        boolean hasTargetNodeId = StringUtils.hasText(request.getTargetNodeId());
        boolean hasTargetNodeIds = (request.getTargetNodeIds() != null && request.getTargetNodeIds().length > 0);
        if (!hasTargetNodeId && !hasTargetNodeIds) {
            SchedulerResponseParameters response = new SchedulerResponseParameters()
                    .setError("Target node is required");
            sendText(session, response.toString());
            return;
        }
        try {
            remoteSchedulerManager.process(request);
        } catch (Exception e) {
            logger.error("Failed to execute scheduler request from session {}", session.getId(), e);
            SchedulerResponseParameters response = new SchedulerResponseParameters()
                    .setError(e.getMessage());
            sendText(session, response.toString());
        }
    }

    /**
     * Finds and returns the scheduler session associated with the given session ID.
     * @param sessionId the session identifier
     * @return the {@link SchedulerSession} if found, or {@code null} if not found
     */
    @Override
    public SchedulerSession findSchedulerSession(String sessionId) {
        Session session = findSession(sessionId);
        return (session != null ? new WebsocketSchedulerSession(session) : null);
    }

    /**
     * Broadcasts a message to all connected sessions through this bridge.
     * @param message the message to broadcast
     */
    @Override
    public void bridge(String message) {
        if (message != null) {
            broadcast(message);
        }
    }

    /**
     * Sends a message to a specific scheduler session through this bridge.
     * @param session the target scheduler session
     * @param message the message to send
     */
    @Override
    public void bridge(@NonNull SchedulerSession session, String message) {
        if (session instanceof WebsocketSchedulerSession websocketSchedulerSession) {
            sendText(websocketSchedulerSession.getSession(), message);
        }
    }

}

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
package com.aspectran.aspectow.console.scheduler.bridge.websocket;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.console.scheduler.bridge.SchedulerBridge;
import com.aspectran.aspectow.console.scheduler.bridge.SchedulerRequestParameters;
import com.aspectran.aspectow.console.scheduler.bridge.SchedulerResponseParameters;
import com.aspectran.aspectow.console.scheduler.bridge.SchedulerSession;
import com.aspectran.aspectow.console.scheduler.manager.SchedulerManager;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Initialize;
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
 * WebsocketSchedulerBridge provides a WebSocket endpoint for real-time
 * scheduler management.
 */
@Component
@ServerEndpoint(
        value = "/nodes/scheduler/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class WebsocketSchedulerBridge extends SimplifiedEndpoint implements SchedulerBridge {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketSchedulerBridge.class);

    private final SchedulerManager schedulerManager;

    private final NodeManager nodeManager;

    @Autowired
    public WebsocketSchedulerBridge(SchedulerManager schedulerManager, NodeManager nodeManager) {
        this.schedulerManager = schedulerManager;
        this.nodeManager = nodeManager;
    }

    @Initialize
    public void register() {
        if (schedulerManager.getBroker() != null) {
            schedulerManager.getBroker().addBridge(this);
            logger.info("WebsocketSchedulerBridge registered with SchedulerBroker");
        }
    }

    @Override
    protected boolean checkAuthorized(@NonNull Session session) {
        String token = session.getPathParameters().get("token");
        try {
            AppMonTokenIssuer.validateToken(token);
            return true;
        } catch (InvalidPBTokenException e) {
            logger.warn("Scheduler WebSocket connection rejected: invalid or expired token");
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
            SchedulerRequestParameters parameters = JsonToParameters.from(message, SchedulerRequestParameters.class);
            String header = parameters.getHeader();
            if ("execute".equals(header)) {
                execute(session, parameters);
            } else if ("join".equals(header)) {
                join(session);
            } else if ("ping".equals(header)) {
                pong(session);
            }
        } catch (Exception e) {
            logger.error("Failed to parse incoming scheduler management request: {}", message, e);
            sendText(session, "[ERROR] Invalid request format");
        }
    }

    private void join(Session session) {
        WebsocketSchedulerSession schedulerSession = new WebsocketSchedulerSession(session);
        schedulerSession.setNodeId(nodeManager.getNodeId());
        if (addSession(session)) {
            SchedulerResponseParameters responseParameters = new SchedulerResponseParameters()
                    .setHeader("joined")
                    .setNodeId(nodeManager.getNodeId());
            sendText(session, responseParameters.toString());
            logger.debug("ConsoleClient joined scheduler management: session {}", session.getId());
        }
    }

    private void pong(Session session) {
        SchedulerResponseParameters responseParameters = new SchedulerResponseParameters()
                .setHeader("pong");
        sendText(session, responseParameters.toString());
    }

    private void execute(Session session, @NonNull SchedulerRequestParameters requestParameters) {
        String command = requestParameters.getCommand();
        if (command != null) {
            String targetNodeId = requestParameters.getTargetNodeId();
            if (targetNodeId == null || targetNodeId.isEmpty()) {
                targetNodeId = nodeManager.getNodeId();
            }

            final String finalTargetNodeId = targetNodeId;
            try {
                Thread.ofVirtual().start(() -> {
                    try {
                        schedulerManager.dispatch(finalTargetNodeId, requestParameters);
                    } catch (Exception e) {
                        logger.error("Failed to execute scheduler request from session {}", session.getId(), e);
                        sendText(session, "[ERROR] " + e.getMessage());
                    }
                });
                logger.debug("Scheduler request execution initiated from session {}: target={}, command={}",
                        session.getId(), finalTargetNodeId, command);
            } catch (Exception e) {
                logger.error("Failed to initiate scheduler request execution from session {}", session.getId(), e);
                sendText(session, "[ERROR] " + e.getMessage());
            }
        }
    }

    @Override
    protected void onSessionRemoved(@NonNull Session session) {
        logger.debug("Scheduler WebSocket session removed: {}", session.getId());
    }

    @Override
    public void bridge(String data) {
        if (data != null) {
            SchedulerResponseParameters responseParameters = new SchedulerResponseParameters()
                    .setHeader("result")
                    .setNodeId(nodeManager.getNodeId())
                    .setResult(data);
            broadcast(responseParameters.toString());
        }
    }

    @Override
    public void bridge(@NonNull SchedulerSession session, String data) {
        if (session instanceof WebsocketSchedulerSession websocketSchedulerSession) {
            SchedulerResponseParameters responseParameters = new SchedulerResponseParameters()
                    .setHeader("result")
                    .setNodeId(nodeManager.getNodeId())
                    .setResult(data);
            sendText(websocketSchedulerSession.getSession(), responseParameters.toString());
        }
    }

}

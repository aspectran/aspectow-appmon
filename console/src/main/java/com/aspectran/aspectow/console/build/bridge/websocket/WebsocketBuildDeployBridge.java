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
package com.aspectran.aspectow.console.build.bridge.websocket;

import com.aspectran.aspectow.console.auth.ConsoleTokenIssuer;
import com.aspectran.aspectow.console.build.bridge.BuildDeployBridge;
import com.aspectran.aspectow.console.build.bridge.BuildDeploySession;
import com.aspectran.aspectow.console.build.bridge.BuildRequestParameters;
import com.aspectran.aspectow.console.build.bridge.BuildResponseParameters;
import com.aspectran.aspectow.console.build.manager.BuildExecutionInfo;
import com.aspectran.aspectow.console.build.manager.RemoteBuildDeployManager;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.apon.JsonToParameters;
import com.aspectran.utils.apon.VariableParameters;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.websocket.jsr356.AspectranConfigurator;
import com.aspectran.web.websocket.jsr356.SimplifiedEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * WebsocketBuildDeployBridge provides a WebSocket endpoint for real-time
 * build log streaming and deployment operations.
 *
 * <p>Created: 2026-08-18</p>
 */
@Component
@ServerEndpoint(
        value = "/build-deploy/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class WebsocketBuildDeployBridge extends SimplifiedEndpoint implements BuildDeployBridge {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketBuildDeployBridge.class);

    private final RemoteBuildDeployManager remoteBuildDeployManager;

    private final NodeManager nodeManager;

    @Autowired
    public WebsocketBuildDeployBridge(RemoteBuildDeployManager remoteBuildDeployManager, NodeManager nodeManager) {
        this.remoteBuildDeployManager = remoteBuildDeployManager;
        this.nodeManager = nodeManager;
    }

    @Initialize
    public void register() {
        if (remoteBuildDeployManager.getBroker() != null) {
            remoteBuildDeployManager.getBroker().addBridge(this);
            logger.info("WebsocketBuildDeployBridge registered with BuildDeployBroker");
        }
    }

    @Override
    protected boolean checkAuthorized(@NonNull Session session) {
        String token = session.getPathParameters().get("token");
        try {
            ConsoleTokenIssuer.validateToken(token);
            boolean isDemo = ConsoleTokenIssuer.isDemoToken(token);
            session.getUserProperties().put("isDemo", isDemo);
            String username = ConsoleTokenIssuer.getUsername(token);
            if (StringUtils.hasText(username)) {
                session.getUserProperties().put("username", username);
            }
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
            BuildRequestParameters parameters = JsonToParameters.from(message, BuildRequestParameters.class);
            String header = parameters.getHeader();

            if ("execute".equals(header)) {
                execute(session, parameters);
            } else if ("cancel".equals(header)) {
                cancel(session, parameters);
            } else if ("join".equals(header) || "subscribe".equals(header)) {
                join(session);
            } else if ("ping".equals(header)) {
                pong(session);
            }
        } catch (Exception e) {
            logger.error("Failed to parse incoming build deploy message: {}", message, e);
            sendText(session, "[ERROR] Invalid message format: " + e.getMessage());
        }
    }

    private void join(Session session) {
        WebsocketBuildDeploySession buildSession = new WebsocketBuildDeploySession(session);
        buildSession.setNodeId(nodeManager.getNodeId());
        if (addSession(session)) {
            remoteBuildDeployManager.getBroker().addBridge(this);

            BuildResponseParameters res = new BuildResponseParameters()
                    .setHeader("subscribed")
                    .setNodeId(nodeManager.getNodeId());
            sendText(session, res.toString());
            logger.debug("BuildDeploy ConsoleClient joined/subscribed: session {}", session.getId());

            // If there are ongoing or recent executions across nodes, backfill status and logs
            Map<String, BuildExecutionInfo> lastExecutions = remoteBuildDeployManager.getLastExecutions();
            if (lastExecutions != null && !lastExecutions.isEmpty()) {
                for (BuildExecutionInfo exec : lastExecutions.values()) {
                    if (exec != null) {
                        remoteBuildDeployManager.getBroker().broadcastStatusChanged(exec);
                        List<String> logs = remoteBuildDeployManager.getRecentLogs(exec.getExecutionId());
                        if (logs != null && !logs.isEmpty()) {
                            remoteBuildDeployManager.getBroker().sendLogBackfill(
                                    buildSession, exec.getExecutionId(), exec.getTargetNodeId(), logs);
                        }
                    }
                }
            }
        }
    }

    private void pong(Session session) {
        BuildResponseParameters res = new BuildResponseParameters()
                .setHeader("pong");
        sendText(session, res.toString());
    }

    private void execute(@NonNull Session session, @NonNull BuildRequestParameters params) {
        Boolean isDemo = (Boolean) session.getUserProperties().get("isDemo");
        if (isDemo != null && isDemo) {
            logger.warn("Rejecting build execution request from DEMO user session: {}", session.getId());
            BuildResponseParameters res = new BuildResponseParameters()
                    .setHeader("status")
                    .setExecutionId(params.getExecutionId() != null ? params.getExecutionId() : "bld_rejected")
                    .setNodeId(params.getTargetNodeId() != null ? params.getTargetNodeId() : nodeManager.getNodeId())
                    .setStatus("FAILED")
                    .setError("Executing build scripts is not allowed in the demo environment.");
            sendText(session, res.toString());
            return;
        }

        String scriptName = params.getScriptName();
        if (StringUtils.isEmpty(scriptName)) {
            BuildResponseParameters res = new BuildResponseParameters()
                    .setHeader("status")
                    .setExecutionId(params.getExecutionId() != null ? params.getExecutionId() : "bld_error")
                    .setNodeId(params.getTargetNodeId() != null ? params.getTargetNodeId() : nodeManager.getNodeId())
                    .setStatus("FAILED")
                    .setError("Script name is required");
            sendText(session, res.toString());
            return;
        }

        String username = (String) session.getUserProperties().get("username");
        if (params.getParameters() == null) {
            params.setParameters(new VariableParameters());
        }
        if (!params.getParameters().hasParameter("requester")) {
            params.getParameters().putValue("requester", StringUtils.hasText(username) ? username : "SYSTEM");
        }

        try {
            remoteBuildDeployManager.dispatch(params);
            logger.info("Build execution dispatched: group={}, all={}, target={}, script={}",
                    params.getTargetGroup(), params.isTargetAll(), params.getTargetNodeId(), scriptName);
        } catch (Exception e) {
            logger.error("Failed to dispatch build execution", e);
            BuildResponseParameters res = new BuildResponseParameters()
                    .setHeader("status")
                    .setExecutionId(params.getExecutionId() != null ? params.getExecutionId() : "bld_error")
                    .setNodeId(params.getTargetNodeId() != null ? params.getTargetNodeId() : nodeManager.getNodeId())
                    .setStatus("FAILED")
                    .setError("Failed to dispatch build: " + e.getMessage());
            sendText(session, res.toString());
        }
    }

    private void cancel(@NonNull Session session, @NonNull BuildRequestParameters params) {
        Boolean isDemo = (Boolean) session.getUserProperties().get("isDemo");
        if (isDemo != null && isDemo) {
            logger.warn("Rejecting build cancellation request from DEMO user session: {}", session.getId());
            BuildResponseParameters res = new BuildResponseParameters()
                    .setHeader("status")
                    .setExecutionId(params.getExecutionId() != null ? params.getExecutionId() : "bld_rejected")
                    .setNodeId(params.getTargetNodeId() != null ? params.getTargetNodeId() : nodeManager.getNodeId())
                    .setError("Canceling build executions is not allowed in the demo environment.");
            sendText(session, res.toString());
            return;
        }

        String executionId = params.getExecutionId();
        String targetNodeId = params.getTargetNodeId();
        if (StringUtils.isEmpty(executionId)) {
            BuildResponseParameters res = new BuildResponseParameters()
                    .setHeader("status")
                    .setNodeId(targetNodeId != null ? targetNodeId : nodeManager.getNodeId())
                    .setError("Execution ID is required for cancellation");
            sendText(session, res.toString());
            return;
        }

        boolean cancelled = remoteBuildDeployManager.cancel(executionId, targetNodeId);
        if (cancelled) {
            logger.info("Build execution cancelled: id={}", executionId);
        } else {
            logger.warn("Could not cancel execution (already finished or not found): id={}", executionId);
            BuildResponseParameters res = new BuildResponseParameters()
                    .setHeader("log")
                    .setExecutionId(executionId)
                    .setNodeId(targetNodeId != null ? targetNodeId : nodeManager.getNodeId())
                    .setLine("[WARN] Could not cancel execution (already finished or not found): " + executionId);
            sendText(session, res.toString());
        }
    }

    @Override
    protected void onSessionRemoved(@NonNull Session session) {
        logger.debug("BuildDeploy WebSocket session removed: {} (Total: {})", session.getId(), countSessions());
    }

    @Override
    public void bridge(String data) {
        if (data != null) {
            broadcast(data);
        }
    }

    @Override
    public void bridge(@NonNull BuildDeploySession session, String data) {
        if (session instanceof WebsocketBuildDeploySession wsSession && data != null) {
            sendText(wsSession.getSession(), data);
        }
    }

}

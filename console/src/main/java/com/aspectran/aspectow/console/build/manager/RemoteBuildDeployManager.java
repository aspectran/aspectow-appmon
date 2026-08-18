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
package com.aspectran.aspectow.console.build.manager;

import com.aspectran.aspectow.console.build.bridge.BuildDeployBroker;
import com.aspectran.aspectow.console.build.bridge.BuildRequestParameters;
import com.aspectran.aspectow.console.build.bridge.redis.BuildMessageBridgeHandler;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.apon.JsonToParameters;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RemoteBuildDeployManager orchestrates build and deployment executions across cluster nodes.
 *
 * <p>Created: 2026-08-18</p>
 */
@Component
@Bean(id = "remoteBuildDeployManager")
public class RemoteBuildDeployManager implements InitializableBean {

    private static final Logger logger = LoggerFactory.getLogger(RemoteBuildDeployManager.class);

    private final NodeManager nodeManager;

    private final LocalScriptRunner localScriptRunner;

    private final BuildDeployBroker broker;

    private final com.aspectran.aspectow.console.build.audit.BuildAuditService buildAuditService;

    private final Map<String, BuildExecutionInfo> activeExecutions = new ConcurrentHashMap<>();

    private volatile BuildExecutionInfo lastExecution;

    public RemoteBuildDeployManager(@NonNull NodeManager nodeManager,
                                   LocalScriptRunner localScriptRunner,
                                   com.aspectran.aspectow.console.build.audit.BuildAuditService buildAuditService) {
        this.nodeManager = nodeManager;
        this.localScriptRunner = localScriptRunner;
        this.broker = new BuildDeployBroker(nodeManager.getNodeId(), nodeManager.getNodeMessagePublisher());
        this.buildAuditService = buildAuditService;
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing RemoteBuildDeployManager for node: {}", nodeManager.getNodeId());

        if (nodeManager.getNodeMessageSubscriber() != null) {
            BuildMessageBridgeHandler bridgeHandler = new BuildMessageBridgeHandler(this);
            nodeManager.getNodeMessageSubscriber().addListener(bridgeHandler);
        }
    }

    public BuildDeployBroker getBroker() {
        return broker;
    }

    public LocalScriptRunner getLocalScriptRunner() {
        return localScriptRunner;
    }

    public BuildExecutionInfo getLastExecution() {
        return lastExecution;
    }

    public BuildExecutionInfo getActiveExecution(String executionId) {
        return activeExecutions.get(executionId);
    }

    public List<String> getRecentLogs(String executionId) {
        return localScriptRunner.getLogBuffer(executionId);
    }

    /**
     * Dispatches a build request to the target node or handles it locally.
     * @param info the execution metadata
     */
    public void dispatch(@NonNull BuildExecutionInfo info) {
        if (StringUtils.isEmpty(info.getExecutionId())) {
            info.setExecutionId("bld_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }

        String targetNodeId = info.getTargetNodeId();
        if (StringUtils.isEmpty(targetNodeId)) {
            targetNodeId = nodeManager.getNodeId();
            info.setTargetNodeId(targetNodeId);
        }

        activeExecutions.put(info.getExecutionId(), info);
        lastExecution = info;

        if (buildAuditService != null) {
            String requester = (info.getParameters() != null && info.getParameters().get("requester") != null)
                    ? String.valueOf(info.getParameters().get("requester"))
                    : "SYSTEM";
            buildAuditService.startAudit(info, requester);
        }

        if (nodeManager.getNodeId().equals(targetNodeId)) {
            // Case 1: Local node execution
            logger.info("Starting local build execution [{}] for script [{}]",
                    info.getExecutionId(), info.getScriptName());

            broker.broadcastStatusChanged(info);

            localScriptRunner.runAsync(info,
                    line -> {
                        broker.broadcastLogLine(info.getExecutionId(), info.getTargetNodeId(), line);
                    },
                    completedInfo -> {
                        logger.info("Local build execution [{}] completed with status: {}",
                                completedInfo.getExecutionId(), completedInfo.getStatus());
                        activeExecutions.remove(completedInfo.getExecutionId());
                        broker.broadcastStatusChanged(completedInfo);

                        if (buildAuditService != null) {
                            List<String> logLines = localScriptRunner.getLogBuffer(completedInfo.getExecutionId());
                            buildAuditService.completeAudit(completedInfo, logLines);
                        }
                    }
            );
        } else {
            // Case 2: Remote node execution via Redis Relay
            if (nodeManager.getNodeMessagePublisher() != null) {
                try {
                    BuildRequestParameters req = new BuildRequestParameters()
                            .setHeader("execute")
                            .setExecutionId(info.getExecutionId())
                            .setTargetNodeId(targetNodeId)
                            .setScriptName(info.getScriptName());

                    nodeManager.getNodeMessagePublisher().publishRelay(
                            BuildDeployBroker.CATEGORY_BUILD, targetNodeId, req.toString());
                    logger.debug("Build execution request dispatched to cluster target {}: {}",
                            targetNodeId, info.getExecutionId());
                } catch (Exception e) {
                    logger.error("Failed to dispatch build request to cluster target {}", targetNodeId, e);
                    info.setStatus(BuildExecutionInfo.Status.FAILED);
                    info.setErrorSummary("Failed to dispatch to remote node: " + e.getMessage());
                    broker.broadcastStatusChanged(info);
                }
            } else {
                logger.warn("Cannot dispatch build request: Node message publisher not available");
                info.setStatus(BuildExecutionInfo.Status.FAILED);
                info.setErrorSummary("Node message publisher is not available for remote dispatch");
                broker.broadcastStatusChanged(info);
            }
        }
    }

    /**
     * Cancels an execution locally or relays cancel command to remote node.
     */
    public boolean cancel(String executionId, String targetNodeId) {
        if (StringUtils.isEmpty(targetNodeId) || nodeManager.getNodeId().equals(targetNodeId)) {
            boolean cancelled = localScriptRunner.cancel(executionId);
            BuildExecutionInfo info = activeExecutions.get(executionId);
            if (info != null) {
                info.setStatus(BuildExecutionInfo.Status.CANCELLED);
                broker.broadcastStatusChanged(info);
            }
            return cancelled;
        } else {
            if (nodeManager.getNodeMessagePublisher() != null) {
                try {
                    BuildRequestParameters req = new BuildRequestParameters()
                            .setHeader("cancel")
                            .setExecutionId(executionId)
                            .setTargetNodeId(targetNodeId);
                    nodeManager.getNodeMessagePublisher().publishRelay(
                            BuildDeployBroker.CATEGORY_BUILD, targetNodeId, req.toString());
                    return true;
                } catch (Exception e) {
                    logger.error("Failed to relay cancel command to node {}", targetNodeId, e);
                }
            }
            return false;
        }
    }

    /**
     * Processes incoming relay messages from Redis.
     * @param message the raw relay message payload
     */
    public void process(String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }

        try {
            if (message.contains("header: execute") || message.contains("\"header\":\"execute\"")) {
                BuildRequestParameters req = JsonToParameters.from(message, BuildRequestParameters.class);
                if (nodeManager.getNodeId().equals(req.getTargetNodeId())) {
                    BuildExecutionInfo info = new BuildExecutionInfo();
                    info.setExecutionId(req.getExecutionId());
                    info.setTargetNodeId(req.getTargetNodeId());
                    info.setScriptName(req.getScriptName());
                    dispatch(info);
                }
            } else if (message.contains("header: cancel") || message.contains("\"header\":\"cancel\"")) {
                BuildRequestParameters req = JsonToParameters.from(message, BuildRequestParameters.class);
                if (nodeManager.getNodeId().equals(req.getTargetNodeId())) {
                    cancel(req.getExecutionId(), req.getTargetNodeId());
                }
            } else {
                // Relay log or status event to local connected WebSocket clients
                broker.bridge(message);
            }
        } catch (Exception e) {
            logger.error("Error processing incoming build relay message: {}", message, e);
        }
    }

}

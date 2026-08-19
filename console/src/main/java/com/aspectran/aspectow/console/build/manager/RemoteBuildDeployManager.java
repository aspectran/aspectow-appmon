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

import com.aspectran.aspectow.console.build.audit.BuildAuditService;
import com.aspectran.aspectow.console.build.bridge.BuildDeployBroker;
import com.aspectran.aspectow.console.build.bridge.BuildRequestParameters;
import com.aspectran.aspectow.console.build.bridge.redis.BuildMessageBridgeHandler;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeRegistry;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.apon.JsonToParameters;
import com.aspectran.utils.apon.VariableParameters;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
     * Dispatches a build request based on parameters (supporting single node, group, or all nodes).
     * @param params the request parameters
     */
    public void dispatch(@NonNull BuildRequestParameters params) {
        String scriptName = params.getScriptName();
        String targetNodeId = params.getTargetNodeId();
        String targetGroup = params.getTargetGroup();
        boolean targetAll = params.isTargetAll();

        List<String> targetNodeIds = resolveTargetNodeIds(targetNodeId, targetGroup, targetAll);
        if (targetNodeIds.isEmpty()) {
            logger.warn("No target nodes found for dispatch: nodeId={}, group={}, all={}",
                    targetNodeId, targetGroup, targetAll);
            BuildExecutionInfo failed = new BuildExecutionInfo();
            failed.setExecutionId(params.getExecutionId() != null ? params.getExecutionId() : "bld_unknown");
            failed.setScriptName(scriptName);
            failed.setStatus(BuildExecutionInfo.Status.FAILED);
            failed.setErrorSummary("No target nodes found matching criteria (node=" + targetNodeId + ", group=" + targetGroup + ", all=" + targetAll + ")");
            broker.broadcastStatusChanged(failed);
            return;
        }

        for (String nodeId : targetNodeIds) {
            BuildExecutionInfo info = new BuildExecutionInfo();
            info.setExecutionId(params.getExecutionId() != null && targetNodeIds.size() == 1
                    ? params.getExecutionId()
                    : ("bld_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)));
            info.setTargetNodeId(nodeId);
            info.setScriptName(scriptName);
            info.setTriggerType("MANUAL");

            if (params.getParameters() != null) {
                for (String pName : params.getParameters().getParameterNames()) {
                    info.getParameters().put(pName, params.getParameters().getString(pName));
                }
            }

            dispatch(info);
        }
    }

    /**
     * Resolves the list of target node IDs based on node ID, group, or all flag.
     * @param targetNodeId optional node ID
     * @param targetGroup optional group ID
     * @param targetAll true if all nodes in cluster
     * @return list of node IDs
     */
    public List<String> resolveTargetNodeIds(String targetNodeId, String targetGroup, boolean targetAll) {
        List<String> result = new ArrayList<>();
        NodeRegistry registry = nodeManager.getNodeRegistry();

        // 1. Group target
        if (StringUtils.hasText(targetGroup)) {
            if (registry != null) {
                List<NodeInfo> groupNodes = registry.getNodesByGroup(targetGroup);
                for (NodeInfo n : groupNodes) {
                    if (n.getId() != null && !result.contains(n.getId())) {
                        result.add(n.getId());
                    }
                }
            }
            if (result.isEmpty() && targetGroup.equals(nodeManager.getGroupId())) {
                result.add(nodeManager.getNodeId());
            }
            return result;
        }

        // 2. All nodes target
        if (targetAll) {
            if (registry != null) {
                List<NodeInfo> allNodes = registry.getNodes();
                for (NodeInfo n : allNodes) {
                    if (n.getId() != null && !result.contains(n.getId())) {
                        result.add(n.getId());
                    }
                }
            }
            if (result.isEmpty()) {
                result.add(nodeManager.getNodeId());
            }
            return result;
        }

        // 3. Specific Node target (or check if it was actually passed as a group name)
        if (StringUtils.hasText(targetNodeId)) {
            if (registry != null && !registry.isFound(targetNodeId)) {
                List<NodeInfo> groupNodes = registry.getNodesByGroup(targetNodeId);
                if (!groupNodes.isEmpty()) {
                    for (NodeInfo n : groupNodes) {
                        if (n.getId() != null && !result.contains(n.getId())) {
                            result.add(n.getId());
                        }
                    }
                    return result;
                }
            }
            result.add(targetNodeId);
            return result;
        }

        // Default to local node
        result.add(nodeManager.getNodeId());
        return result;
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

                    if (info.getParameters() != null && !info.getParameters().isEmpty()) {
                        VariableParameters varParams = new VariableParameters();
                        for (Map.Entry<String, Object> entry : info.getParameters().entrySet()) {
                            if (entry.getValue() != null) {
                                varParams.putValue(entry.getKey(), entry.getValue().toString());
                            }
                        }
                        req.setParameters(varParams);
                    }

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
                    if (req.getParameters() != null) {
                        for (String pName : req.getParameters().getParameterNames()) {
                            info.getParameters().put(pName, req.getParameters().getString(pName));
                        }
                    }
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

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
import com.aspectran.aspectow.console.build.audit.BuildHistory;
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
import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.apon.VariableParameters;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final BuildAuditService buildAuditService;

    private final Map<String, BuildExecutionInfo> activeExecutions = new ConcurrentHashMap<>();

    public RemoteBuildDeployManager(@NonNull NodeManager nodeManager,
                                   LocalScriptRunner localScriptRunner,
                                   BuildAuditService buildAuditService) {
        this.nodeManager = nodeManager;
        this.localScriptRunner = localScriptRunner;
        this.broker = new BuildDeployBroker(nodeManager.getNodeId(), nodeManager.getNodeMessagePublisher(), nodeManager.getNodeRegistry());
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

    public String getNodeId() {
        return nodeManager.getNodeId();
    }

    public BuildDeployBroker getBroker() {
        return broker;
    }

    public LocalScriptRunner getLocalScriptRunner() {
        return localScriptRunner;
    }

    public BuildExecutionInfo getLastExecution() {
        return getLastExecution(nodeManager.getNodeId());
    }

    public BuildExecutionInfo getLastExecution(String nodeId) {
        if (StringUtils.isEmpty(nodeId)) {
            nodeId = nodeManager.getNodeId();
        }

        // 1. Check if there is an active execution running on this node
        for (BuildExecutionInfo active : activeExecutions.values()) {
            if (nodeId.equals(active.getTargetNodeId())) {
                return active;
            }
        }

        // 2. Fetch latest execution record from shared DB
        if (buildAuditService != null) {
            try {
                BuildHistory buildHistory = buildAuditService.getLatestBuildHistory(nodeId);
                if (buildHistory != null) {
                    return convertToBuildExecutionInfo(buildHistory);
                }
            } catch (Exception e) {
                logger.trace("Failed to load last build execution for node {} from shared DB audit history", nodeId, e);
            }
        }
        return null;
    }

    public Set<String> getAvailableNodeIds() {
        Set<String> nodeIds = new HashSet<>();
        nodeIds.add(nodeManager.getNodeId());
        if (nodeManager.getNodeInfoHolder() != null && nodeManager.getNodeInfoHolder().getNodeInfoList() != null) {
            for (NodeInfo info : nodeManager.getNodeInfoHolder().getNodeInfoList()) {
                if (info.getId() != null) {
                    nodeIds.add(info.getId());
                }
            }
        }
        if (nodeManager.getNodeRegistry() != null) {
            List<NodeInfo> registered = nodeManager.getNodeRegistry().getNodes();
            if (registered != null) {
                for (NodeInfo info : registered) {
                    if (info.getId() != null) {
                        nodeIds.add(info.getId());
                    }
                }
            }
        }
        return nodeIds;
    }

    public Map<String, BuildExecutionInfo> getLastExecutions() {
        return getLastExecutions(getAvailableNodeIds());
    }

    public Map<String, BuildExecutionInfo> getLastExecutions(Collection<String> targetNodeIds) {
        Map<String, BuildExecutionInfo> result = new HashMap<>();
        if (targetNodeIds == null || targetNodeIds.isEmpty()) {
            return result;
        }

        // 1. Fetch latest build histories from shared DB for available target nodes only
        if (buildAuditService != null) {
            try {
                List<BuildHistory> list = buildAuditService.getLatestBuildHistories(targetNodeIds);
                if (list != null) {
                    for (BuildHistory h : list) {
                        if (h.getTargetNodeId() != null) {
                            result.put(h.getTargetNodeId(), convertToBuildExecutionInfo(h));
                        }
                    }
                }
            } catch (Exception e) {
                logger.trace("Failed to load latest build histories from shared DB", e);
            }
        }

        // 2. Override with active running executions if any
        for (BuildExecutionInfo active : activeExecutions.values()) {
            if (active.getTargetNodeId() != null && targetNodeIds.contains(active.getTargetNodeId())) {
                result.put(active.getTargetNodeId(), active);
            }
        }

        return result;
    }

    @NonNull
    private BuildExecutionInfo convertToBuildExecutionInfo(@NonNull BuildHistory buildHistory) {
        BuildExecutionInfo info = new BuildExecutionInfo();
        info.setExecutionId(buildHistory.getExecutionId());
        info.setTargetNodeId(buildHistory.getTargetNodeId());
        info.setScriptName(buildHistory.getScriptName());
        if (buildHistory.getStatus() != null) {
            try {
                info.setStatus(BuildExecutionInfo.Status.valueOf(buildHistory.getStatus()));
            } catch (Exception ignored) {
                info.setStatus(BuildExecutionInfo.Status.FAILED);
            }
        }
        info.setExitCode(buildHistory.getExitCode());
        info.setStartedAt(buildHistory.getStartedAt());
        info.setFinishedAt(buildHistory.getFinishedAt());
        info.setDurationMs(buildHistory.getDurationMs());
        info.setGitBranch(buildHistory.getGitBranch());
        info.setGitCommitBefore(buildHistory.getGitCommitBefore());
        info.setGitCommitAfter(buildHistory.getGitCommitAfter());
        info.setGitCommitMsg(buildHistory.getGitCommitMsg());
        info.setErrorSummary(buildHistory.getErrorSummary());
        return info;
    }

    public BuildExecutionInfo getActiveExecution(String executionId) {
        return activeExecutions.get(executionId);
    }

    public List<String> getRecentLogs(String executionId) {
        if (executionId == null) {
            return Collections.emptyList();
        }
        List<String> logs = localScriptRunner.getLogBuffer(executionId);
        if (logs != null && !logs.isEmpty()) {
            return logs;
        }
        if (buildAuditService != null) {
            try {
                String rawLog = buildAuditService.getDecompressedLogsByExecutionId(executionId);
                if (StringUtils.hasText(rawLog)) {
                    return Arrays.asList(rawLog.split("\n"));
                }
            } catch (Exception ignored) {
            }
        }
        return Collections.emptyList();
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
        boolean targetServices = params.isTargetServices();

        List<String> targetNodeIds = resolveTargetNodeIds(targetNodeId, targetGroup, targetAll, targetServices);
        if (targetNodeIds.isEmpty()) {
            logger.warn("No target nodes found for dispatch: nodeId={}, group={}, all={}, services={}",
                    targetNodeId, targetGroup, targetAll, targetServices);
            BuildExecutionInfo failed = new BuildExecutionInfo();
            failed.setExecutionId(params.getExecutionId() != null ? params.getExecutionId() : "bld_unknown");
            failed.setScriptName(scriptName);
            failed.setStatus(BuildExecutionInfo.Status.FAILED);
            failed.setErrorSummary("No target nodes found matching criteria (node=" + targetNodeId + ", group=" + targetGroup + ", all=" + targetAll + ", services=" + targetServices + ")");
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
        return resolveTargetNodeIds(targetNodeId, targetGroup, targetAll, false);
    }

    /**
     * Resolves the list of target node IDs based on node ID, group, all flag, or services flag.
     * @param targetNodeId optional node ID
     * @param targetGroup optional group ID
     * @param targetAll true if all nodes in cluster
     * @param targetServices true if service nodes only (excluding console-dedicated nodes)
     * @return list of node IDs
     */
    public List<String> resolveTargetNodeIds(String targetNodeId, String targetGroup, boolean targetAll, boolean targetServices) {
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

        // 3. Service nodes target (excluding console nodes)
        if (targetServices) {
            if (registry != null) {
                List<NodeInfo> allNodes = registry.getNodes();
                for (NodeInfo n : allNodes) {
                    if (n.getId() != null && !n.isConsole() && !result.contains(n.getId())) {
                        result.add(n.getId());
                    }
                }
            }
            if (result.isEmpty()) {
                NodeInfo localNode = nodeManager.getNodeInfoHolder().getNodeInfo(nodeManager.getNodeId());
                if (localNode == null || !localNode.isConsole()) {
                    result.add(nodeManager.getNodeId());
                }
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

        if (nodeManager.getNodeId().equals(targetNodeId)) {
            // Case 1: Local node execution
            logger.info("Starting local build execution [{}] for script [{}]",
                    info.getExecutionId(), info.getScriptName());

            activeExecutions.put(info.getExecutionId(), info);

            if (buildAuditService != null) {
                String requester = (info.getParameters() != null && info.getParameters().containsKey("requester"))
                        ? String.valueOf(info.getParameters().get("requester"))
                        : "SYSTEM";
                buildAuditService.startAudit(info, requester);
            }

            broker.broadcastStatusChanged(info);

            localScriptRunner.runAsync(info,
                    startedInfo -> {
                        logger.info("Local build execution [{}] started running", startedInfo.getExecutionId());
                        broker.broadcastStatusChanged(startedInfo);
                    },
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
            Parameters params = JsonToParameters.from(message);
            String header = params.getString("header");

            if ("execute".equals(header)) {
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
            } else if ("cancel".equals(header)) {
                BuildRequestParameters req = JsonToParameters.from(message, BuildRequestParameters.class);
                if (nodeManager.getNodeId().equals(req.getTargetNodeId())) {
                    cancel(req.getExecutionId(), req.getTargetNodeId());
                }
            } else if ("log".equals(header) || "status".equals(header)) {
                String nodeId = params.getString("nodeId");
                if (!nodeManager.getNodeId().equals(nodeId)) {
                    broker.bridge(message);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing incoming build relay message: {}", message, e);
        }
    }

}

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
import com.aspectran.aspectow.console.build.bridge.BuildResponseParameters;
import com.aspectran.aspectow.console.build.bridge.redis.BuildMessageBridgeHandler;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeRegistry;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
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
import java.util.Iterator;
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

    private static final int MAX_REMOTE_LOG_BUFFER_SIZE = 10000;

    private static final int MAX_REMOTE_BUFFER_SESSIONS = 100;

    private final NodeManager nodeManager;

    private final LocalScriptRunner localScriptRunner;

    private final BuildDeployBroker broker;

    private final BuildAuditService buildAuditService;

    private final Map<String, BuildExecutionInfo> activeExecutions = new ConcurrentHashMap<>();

    private final Map<String, List<String>> remoteLogBuffers = new ConcurrentHashMap<>();

    public RemoteBuildDeployManager(@NonNull NodeManager nodeManager,
                                   LocalScriptRunner localScriptRunner,
                                   BuildAuditService buildAuditService) {
        this.nodeManager = nodeManager;
        this.localScriptRunner = localScriptRunner;
        this.broker = new BuildDeployBroker(nodeManager.getNodeId(), nodeManager.getNodeMessagePublisher(), nodeManager.getNodeRegistry());
        this.buildAuditService = buildAuditService;
    }

    @Destroy
    public void destroy() {
        activeExecutions.clear();
        remoteLogBuffers.clear();
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

    public Map<String, BuildExecutionInfo> getExecutions(String executionId) {
        if (StringUtils.isEmpty(executionId)) {
            return Collections.emptyMap();
        }
        Map<String, BuildExecutionInfo> result = new HashMap<>();

        // 1. Fetch from shared DB audit history
        if (buildAuditService != null) {
            try {
                List<BuildHistory> list = buildAuditService.getBuildHistoriesByExecutionId(executionId);
                if (list != null) {
                    for (BuildHistory h : list) {
                        if (h.getTargetNodeId() != null) {
                            result.put(h.getTargetNodeId(), convertToBuildExecutionInfo(h));
                        }
                    }
                }
            } catch (Exception e) {
                logger.trace("Failed to load build histories for execution {} from shared DB", executionId, e);
            }
        }

        // 2. Override with active running executions if matching executionId
        for (BuildExecutionInfo active : activeExecutions.values()) {
            if (executionId.equals(active.getExecutionId()) && active.getTargetNodeId() != null) {
                result.put(active.getTargetNodeId(), active);
            }
        }

        return result;
    }

    public BuildExecutionInfo getExecution(String executionId, String nodeId) {
        if (StringUtils.isEmpty(executionId)) {
            return null;
        }
        // 1. Check active executions
        for (BuildExecutionInfo active : activeExecutions.values()) {
            if (executionId.equals(active.getExecutionId())) {
                if (nodeId == null || nodeId.equals(active.getTargetNodeId())) {
                    return active;
                }
            }
        }
        // 2. Fetch from DB
        if (buildAuditService != null) {
            try {
                BuildHistory history = (nodeId != null)
                        ? buildAuditService.getHistoryDetailByExecutionIdAndNodeId(executionId, nodeId)
                        : buildAuditService.getHistoryDetailByExecutionId(executionId);
                if (history != null) {
                    return convertToBuildExecutionInfo(history);
                }
            } catch (Exception e) {
                logger.trace("Failed to load build execution {} from DB", executionId, e);
            }
        }
        return null;
    }

    public Map<String, List<String>> getNodeLogs(String executionId) {
        if (StringUtils.isEmpty(executionId)) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> result = new HashMap<>();
        if (buildAuditService != null) {
            try {
                result.putAll(buildAuditService.getNodeLogsByExecutionId(executionId));
            } catch (Exception ignored) {
            }
        }
        List<String> localLogs = localScriptRunner.getLogBuffer(executionId);
        if (localLogs != null && !localLogs.isEmpty()) {
            result.put(nodeManager.getNodeId(), localLogs);
        }
        return result;
    }

    public List<String> getRecentLogs(BuildExecutionInfo exec) {
        if (exec == null) {
            return Collections.emptyList();
        }
        List<String> logs = localScriptRunner.getLogBuffer(exec.getExecutionId());
        if (logs != null && !logs.isEmpty()) {
            return logs;
        }
        if (buildAuditService != null && exec.getExecutionId() != null) {
            try {
                BuildHistory history = (exec.getTargetNodeId() != null)
                        ? buildAuditService.getHistoryDetailByExecutionIdAndNodeId(exec.getExecutionId(), exec.getTargetNodeId())
                        : buildAuditService.getHistoryDetailByExecutionId(exec.getExecutionId());
                if (history != null && history.getHistoryId() != null) {
                    String rawLog = buildAuditService.getDecompressedLogs(history.getHistoryId());
                    if (StringUtils.hasText(rawLog)) {
                        return Arrays.asList(rawLog.split("\n"));
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return Collections.emptyList();
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
     * @param request the request parameters
     */
    public void dispatch(@NonNull BuildRequestParameters request) {
        String scriptName = request.getScriptName();
        String targetNodeId = request.getTargetNodeId();
        String targetGroup = request.getTargetGroup();
        boolean targetAll = request.isTargetAll();
        boolean targetServices = request.isTargetServices();

        List<String> targetNodeIds = resolveTargetNodeIds(targetNodeId, targetGroup, targetAll, targetServices);
        if (targetNodeIds.isEmpty()) {
            logger.warn("No target nodes found for dispatch: nodeId={}, group={}, all={}, services={}",
                    targetNodeId, targetGroup, targetAll, targetServices);
            BuildExecutionInfo failed = new BuildExecutionInfo();
            failed.setExecutionId(request.getExecutionId() != null ? request.getExecutionId() : "bld_unknown");
            failed.setScriptName(scriptName);
            failed.setStatus(BuildExecutionInfo.Status.FAILED);
            failed.setErrorSummary("No target nodes found matching criteria (node=" + targetNodeId + ", group=" + targetGroup + ", all=" + targetAll + ", services=" + targetServices + ")");
            broker.broadcastStatusChanged(failed);
            return;
        }

        String executionId = (request.getExecutionId() != null
                ? request.getExecutionId()
                : ("bld_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)));

        for (String nodeId : targetNodeIds) {
            BuildExecutionInfo info = new BuildExecutionInfo();
            info.setExecutionId(executionId);
            info.setTargetNodeId(nodeId);
            info.setScriptName(scriptName);
            info.setStatus(BuildExecutionInfo.Status.PENDING);
            info.setStartedAt(java.time.Instant.now());
            info.setTriggerType("MANUAL");

            if (request.getParameters() != null) {
                for (String pName : request.getParameters().getParameterNames()) {
                    info.getParameters().put(pName, request.getParameters().getString(pName));
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
        } else if (targetAll) {
            // 2. All nodes target
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
        } else if (targetServices) {
            // 3. Service nodes target (excluding console nodes)
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
        } else if (StringUtils.hasText(targetNodeId)) {
            // 4. Specific Node target (or check if it was actually passed as a group name)
            if (registry != null && !registry.isFound(targetNodeId)) {
                List<NodeInfo> groupNodes = registry.getNodesByGroup(targetNodeId);
                if (!groupNodes.isEmpty()) {
                    for (NodeInfo n : groupNodes) {
                        if (n.getId() != null && !result.contains(n.getId())) {
                            result.add(n.getId());
                        }
                    }
                }
            }
            if (result.isEmpty()) {
                result.add(targetNodeId);
            }
        } else {
            // Default to local node
            result.add(nodeManager.getNodeId());
        }

        Collections.sort(result);
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

        final String targetNodeId = (StringUtils.hasText(info.getTargetNodeId())
                ? info.getTargetNodeId()
                : nodeManager.getNodeId());
        info.setTargetNodeId(targetNodeId);

        if (nodeManager.getNodeId().equals(targetNodeId)) {
            // Case 1: Local node execution
            logger.info("Starting local build execution [{}] for script [{}]",
                    info.getExecutionId(), info.getScriptName());

            activeExecutions.put(info.getExecutionId(), info);
            activeExecutions.put(info.getExecutionId() + ":" + targetNodeId, info);

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
                        if (buildAuditService != null) {
                            buildAuditService.updateStatus(startedInfo);
                        }
                        broker.broadcastStatusChanged(startedInfo);
                    },
                    line -> {
                        broker.broadcastLogLine(info.getExecutionId(), info.getTargetNodeId(), line);
                    },
                    completedInfo -> {
                        logger.info("Local build execution [{}] completed with status: {}",
                                completedInfo.getExecutionId(), completedInfo.getStatus());
                        activeExecutions.remove(completedInfo.getExecutionId());
                        activeExecutions.remove(completedInfo.getExecutionId() + ":" + targetNodeId);
                        broker.broadcastStatusChanged(completedInfo);

                        if (buildAuditService != null) {
                            List<String> logLines = localScriptRunner.getLogBuffer(completedInfo.getExecutionId());
                            buildAuditService.completeAudit(completedInfo, logLines);
                        }
                    }
            );
        } else {
            // Case 2: Remote node execution via Redis Relay
            activeExecutions.put(info.getExecutionId() + ":" + targetNodeId, info);
            if (buildAuditService != null) {
                String requester = (info.getParameters() != null && info.getParameters().containsKey("requester"))
                        ? String.valueOf(info.getParameters().get("requester"))
                        : "SYSTEM";
                buildAuditService.startAudit(info, requester);
            }

            broker.broadcastStatusChanged(info);

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
                    activeExecutions.remove(info.getExecutionId() + ":" + targetNodeId);
                    broker.broadcastStatusChanged(info);
                }
            } else {
                logger.warn("Cannot dispatch build request: Node message publisher not available");
                info.setStatus(BuildExecutionInfo.Status.FAILED);
                info.setErrorSummary("Node message publisher is not available for remote dispatch");
                activeExecutions.remove(info.getExecutionId() + ":" + targetNodeId);
                broker.broadcastStatusChanged(info);
            }
        }
    }

    /**
     * Cancels an execution locally or relays cancel command to remote node.
     */
    public boolean cancel(String executionId, String targetNodeId) {
        if (StringUtils.hasText(targetNodeId) && !nodeManager.getNodeId().equals(targetNodeId)) {
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

        boolean cancelled = localScriptRunner.cancel(executionId);
        BuildExecutionInfo info = activeExecutions.get(executionId);
        if (info != null) {
            info.setStatus(BuildExecutionInfo.Status.CANCELLED);
            broker.broadcastStatusChanged(info);
        }
        if (!cancelled && StringUtils.isEmpty(targetNodeId) && nodeManager.getNodeMessagePublisher() != null && nodeManager.getNodeRegistry() != null) {
            try {
                for (NodeInfo nodeInfo : nodeManager.getNodeRegistry().getNodes()) {
                    if (nodeInfo.getId() != null && !nodeManager.getNodeId().equals(nodeInfo.getId())) {
                        BuildRequestParameters req = new BuildRequestParameters()
                                .setHeader("cancel")
                                .setExecutionId(executionId)
                                .setTargetNodeId(nodeInfo.getId());
                        nodeManager.getNodeMessagePublisher().publishRelay(
                                BuildDeployBroker.CATEGORY_BUILD, nodeInfo.getId(), req.toString());
                        cancelled = true;
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to broadcast cancel command across cluster nodes", e);
            }
        }
        return cancelled;
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
                    if ("log".equals(header)) {
                        String execId = params.getString("executionId");
                        String line = params.getString("line");
                        if (StringUtils.hasText(execId) && StringUtils.hasText(line)) {
                            appendRemoteLog(execId, nodeId, line);
                        }
                    } else if ("status".equals(header)) {
                        BuildResponseParameters res = JsonToParameters.from(message, BuildResponseParameters.class);
                        if (res.getExecutionId() != null) {
                            String st = res.getStatus();
                            if (StringUtils.hasText(st)) {
                                BuildExecutionInfo info = new BuildExecutionInfo();
                                info.setExecutionId(res.getExecutionId());
                                info.setTargetNodeId(nodeId);
                                info.setScriptName(res.getScriptName());
                                try {
                                    info.setStatus(BuildExecutionInfo.Status.valueOf(st));
                                } catch (Exception ignored) {
                                }
                                info.setExitCode(res.getExitCode());
                                info.setDurationMs(res.getDurationMs());
                                info.setGitBranch(res.getGitBranch());
                                info.setGitCommitBefore(res.getGitCommitBefore());
                                info.setGitCommitAfter(res.getGitCommitAfter());
                                info.setGitCommitMsg(res.getGitCommitMsg());
                                info.setErrorSummary(res.getError());

                                if (info.getStatus() == BuildExecutionInfo.Status.RUNNING || info.getStatus() == BuildExecutionInfo.Status.PENDING) {
                                    activeExecutions.put(info.getExecutionId() + ":" + nodeId, info);
                                    if (buildAuditService != null) {
                                        buildAuditService.updateStatus(info);
                                    }
                                } else {
                                    activeExecutions.remove(info.getExecutionId() + ":" + nodeId);
                                    activeExecutions.remove(info.getExecutionId());
                                    if (buildAuditService != null) {
                                        List<String> logs = remoteLogBuffers.remove(info.getExecutionId() + ":" + nodeId);
                                        buildAuditService.completeAudit(info, logs);
                                    }
                                }
                            }
                        }
                    }
                    broker.bridge(message);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing incoming build relay message: {}", message, e);
        }
    }

    private void appendRemoteLog(String execId, String nodeId, String line) {
        String key = execId + ":" + nodeId;
        if (remoteLogBuffers.size() > MAX_REMOTE_BUFFER_SESSIONS && !remoteLogBuffers.containsKey(key)) {
            Iterator<String> it = remoteLogBuffers.keySet().iterator();
            if (it.hasNext()) {
                remoteLogBuffers.remove(it.next());
            }
        }
        List<String> buffer = remoteLogBuffers.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
        if (buffer.size() >= MAX_REMOTE_LOG_BUFFER_SIZE) {
            buffer.removeFirst();
        }
        buffer.add(line);
    }

}

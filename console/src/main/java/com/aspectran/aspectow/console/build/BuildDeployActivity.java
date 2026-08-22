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
package com.aspectran.aspectow.console.build;

import com.aspectran.aspectow.console.build.manager.BuildExecutionInfo;
import com.aspectran.aspectow.console.build.manager.RemoteBuildDeployManager;
import com.aspectran.aspectow.console.cluster.NodeConsoleHelper;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BuildDeployActivity provides views and REST endpoints for remote build and deployment management.
 *
 * <p>Created: 2026-08-18</p>
 */
@Component("/cluster/build")
@Profile("[console.ui, console.custom-ui]")
public class BuildDeployActivity {

    private final NodeManager nodeManager;

    private final RemoteBuildDeployManager remoteBuildDeployManager;

    private final NodeConsoleHelper nodeConsoleHelper;

    @Autowired
    public BuildDeployActivity(NodeManager nodeManager,
                               RemoteBuildDeployManager remoteBuildDeployManager,
                               NodeConsoleHelper nodeConsoleHelper) {
        this.nodeManager = nodeManager;
        this.remoteBuildDeployManager = remoteBuildDeployManager;
        this.nodeConsoleHelper = nodeConsoleHelper;
    }

    /**
     * Displays the build &amp; deployment dashboard page.
     * @param nodeId the target node ID
     * @return a map of attributes for rendering the view
     */
    @Request("/")
    @Dispatch("cluster/build")
    @Action("page")
    public Map<String, Object> buildPage(String nodeId) {
        String clusterMode = nodeManager.getClusterConfig().getMode();
        List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(true);
        String targetNodeId = (nodeId != null ? (nodes.stream().anyMatch(n -> nodeId.equals(n.get("id"))) ? nodeId : null) : null);
        if (nodeId != null && targetNodeId == null) {
            throw new IllegalArgumentException("No node found with ID: " + nodeId);
        }

        Set<String> allowedScripts = remoteBuildDeployManager.getLocalScriptRunner().getAllowedScripts();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Build & Deployment");
        model.put("style", "build-page");
        model.put("group", "cluster-menu");
        model.put("clusterMode", clusterMode);
        model.put("nodes", nodes);
        model.put("allowedScripts", new ArrayList<>(allowedScripts));
        model.put("myNodeId", nodeManager.getNodeId());
        if (targetNodeId != null) {
            model.put("targetNodeId", targetNodeId);
        }

        Set<String> nodeIds = nodes.stream()
                .map(n -> (String) n.get("id"))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, BuildExecutionInfo> lastExecutions = remoteBuildDeployManager.getLastExecutions(nodeIds);
        if (lastExecutions != null && !lastExecutions.isEmpty()) {
            model.put("lastExecutions", lastExecutions);
        }

        BuildExecutionInfo lastExec = (targetNodeId != null
                ? remoteBuildDeployManager.getLastExecution(targetNodeId)
                : remoteBuildDeployManager.getLastExecution());
        if (lastExec != null) {
            model.put("lastExecution", lastExec);
        }

        return model;
    }

    /**
     * Lists all registered nodes with their current status.
     * @return the RestResponse containing a list of node information maps
     */
    @Request("/list")
    public RestResponse listNodes() {
        try {
            List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(true);
            return new SuccessResponse(nodes).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    /**
     * Returns the list of supported build and deployment scripts.
     * @return set of script names
     */
    @Request("/scripts")
    public Set<String> getAvailableScripts() {
        return remoteBuildDeployManager.getLocalScriptRunner().getAllowedScripts();
    }

    /**
     * Returns current build execution status.
     * @param executionId optional execution ID
     * @param nodeId optional node ID
     * @return execution status map
     */
    @Request("/status")
    public Map<String, Object> getStatus(String executionId, String nodeId) {
        BuildExecutionInfo info;
        if (StringUtils.hasText(executionId)) {
            info = remoteBuildDeployManager.getActiveExecution(executionId);
        } else if (StringUtils.hasText(nodeId)) {
            info = remoteBuildDeployManager.getLastExecution(nodeId);
        } else {
            info = remoteBuildDeployManager.getLastExecution();
        }

        Map<String, Object> result = new HashMap<>();
        if (info != null) {
            result.put("executionId", info.getExecutionId());
            result.put("targetNodeId", info.getTargetNodeId());
            result.put("scriptName", info.getScriptName());
            result.put("status", info.getStatus().name());
            result.put("exitCode", info.getExitCode());
            result.put("durationMs", info.getDurationMs());
            result.put("startedAt", info.getStartedAt() != null ? info.getStartedAt().toString() : null);
            result.put("finishedAt", info.getFinishedAt() != null ? info.getFinishedAt().toString() : null);
            result.put("gitBranch", info.getGitBranch());
            result.put("gitCommitBefore", info.getGitCommitBefore());
            result.put("gitCommitAfter", info.getGitCommitAfter());
            result.put("gitCommitMsg", info.getGitCommitMsg());
            result.put("errorSummary", info.getErrorSummary());
        } else {
            result.put("status", "IDLE");
        }
        return result;
    }

    /**
     * Dispatches a new build execution.
     * @param translet the current translet
     * @return execution result
     */
    @RequestToPost("/execute")
    public Map<String, Object> executeBuild(@NonNull Translet translet) {
        String targetNodeId = translet.getParameter("nodeId");
        String scriptName = translet.getParameter("scriptName");
        String branch = translet.getParameter("branch");

        if (StringUtils.isEmpty(scriptName)) {
            throw new IllegalArgumentException("Script name is required");
        }
        if (StringUtils.isEmpty(targetNodeId)) {
            targetNodeId = nodeManager.getNodeId();
        }

        BuildExecutionInfo info = new BuildExecutionInfo();
        info.setTargetNodeId(targetNodeId);
        info.setScriptName(scriptName);
        if (StringUtils.hasText(branch)) {
            info.getParameters().put("branch", branch);
        }

        com.aspectran.aspectow.console.auth.UserInfo userInfo =
                translet.getSessionAdapter().getAttribute(com.aspectran.aspectow.console.auth.UserInfo.USERINFO_KEY);
        String requester = (userInfo != null && StringUtils.hasText(userInfo.getUsername()))
                ? userInfo.getUsername()
                : "SYSTEM";
        info.getParameters().put("requester", requester);

        remoteBuildDeployManager.dispatch(info);

        Map<String, Object> result = new HashMap<>();
        result.put("executionId", info.getExecutionId());
        result.put("targetNodeId", targetNodeId);
        result.put("scriptName", scriptName);
        result.put("status", info.getStatus().name());
        result.put("message", "Build execution initiated successfully: " + info.getExecutionId());
        return result;
    }

    /**
     * Cancels an ongoing build execution.
     * @param translet the current translet
     * @return cancellation result
     */
    @RequestToPost("/cancel")
    public Map<String, Object> cancelBuild(@NonNull Translet translet) {
        String executionId = translet.getParameter("executionId");
        String targetNodeId = translet.getParameter("nodeId");

        if (StringUtils.isEmpty(executionId)) {
            throw new IllegalArgumentException("Execution ID is required");
        }

        boolean cancelled = remoteBuildDeployManager.cancel(executionId, targetNodeId);

        Map<String, Object> result = new HashMap<>();
        result.put("executionId", executionId);
        result.put("cancelled", cancelled);
        result.put("message", cancelled ? "Cancellation signal sent" : "Execution not found or already finished");
        return result;
    }

    /**
     * Checks the health and liveness of a specific node.
     * @param nodeId the target node ID
     * @return node health status map
     */
    @Request("/health/${nodeId}")
    public RestResponse checkNodeHealth(String nodeId) {
        if (StringUtils.isEmpty(nodeId)) {
            return new FailureResponse().setError("error", "Node ID is required");
        }
        try {
            boolean live = false;
            if (nodeManager.getNodeRegistry() != null) {
                live = nodeManager.getNodeRegistry().isLive(nodeId, 5000);
            } else if (nodeId.equals(nodeManager.getNodeId())) {
                live = true;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("nodeId", nodeId);
            data.put("alive", live);
            data.put("status", live ? "LIVE" : "DEAD");
            data.put("timestamp", System.currentTimeMillis());
            return new SuccessResponse(data).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    /**
     * Retrieves daemon output or error logs (daemon-stderr.log, daemon-stdout.log).
     * @param type "stderr" or "stdout" (default is "stderr")
     * @param lines optional max number of lines (default 200)
     * @return REST response containing log content and metadata
     */
    @Request("/daemon-log")
    public RestResponse getDaemonLog(String type, Integer lines) {
        try {
            String logType = (StringUtils.hasText(type) ? type : "stderr");
            int maxLines = (lines != null && lines > 0 ? lines : 200);
            Map<String, Object> data = remoteBuildDeployManager.getLocalScriptRunner().getDaemonLogInfo(logType, maxLines);
            data.put("timestamp", System.currentTimeMillis());
            return new SuccessResponse(data).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

}

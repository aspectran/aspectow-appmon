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
package com.aspectran.aspectow.console.scheduler;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.console.cluster.NodeConsoleHelper;
import com.aspectran.aspectow.console.scheduler.bridge.SchedulerRequestParameters;
import com.aspectran.aspectow.console.scheduler.bridge.polling.PollingSchedulerBridge;
import com.aspectran.aspectow.console.scheduler.bridge.polling.PollingSchedulerSession;
import com.aspectran.aspectow.console.scheduler.manager.SchedulerManager;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * SchedulerActivity provides views and API endpoints for scheduler management.
 *
 * <p>Created: 2026-04-26</p>
 */
@Component(NODES_BASE_PATH + "/scheduler")
public class SchedulerActivity {

    private final NodeManager nodeManager;

    private final SchedulerManager schedulerManager;

    private final PollingSchedulerBridge pollingSchedulerBridge;

    private final NodeConsoleHelper nodeConsoleHelper;

    @Autowired
    public SchedulerActivity(NodeManager nodeManager,
                             SchedulerManager schedulerManager,
                             PollingSchedulerBridge pollingSchedulerBridge,
                             NodeConsoleHelper nodeConsoleHelper) {
        this.nodeManager = nodeManager;
        this.schedulerManager = schedulerManager;
        this.pollingSchedulerBridge = pollingSchedulerBridge;
        this.nodeConsoleHelper = nodeConsoleHelper;
    }

    /**
     * Displays the scheduler management page.
     * @param nodeId the node ID
     * @return a map of attributes for rendering the view
     */
    @Request("/")
    @Dispatch("nodes/scheduler")
    @Action("page")
    public Map<String, Object> scheduler(String nodeId) {
        String clusterMode = nodeManager.getClusterConfig().getMode();
        List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(true);
        NodeInfo nodeInfo = (nodeId != null ? nodeManager.getNodeInfoHolder().getNodeInfo(nodeId) : null);
        if (nodeInfo == null) {
            nodeInfo = nodeManager.getNodeInfoHolder().getNodeInfo(nodeManager.getNodeId());
        }
        return Map.of(
                "title", "Scheduler Manager",
                "style", "scheduler-page",
                "group", "cluster-menu",
                "clusterMode", clusterMode,
                "nodes", nodes,
                "node", nodeConsoleHelper.createNodeMap(nodeInfo, true, true),
                "token", AppMonTokenIssuer.issueToken(30),
                "jobLockProvider", (CoreServiceHolder.getJobLockProvider() != null)
        );
    }

    /**
     * Lists all registered nodes with their current status.
     * @return a list of node information maps
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
     * Joins a polling session.
     * @param nodeId the node ID to join
     * @return the node ID
     */
    @Request("/join")
    public RestResponse join(String nodeId) {
        if (StringUtils.isEmpty(nodeId)) {
            nodeId = nodeManager.getNodeId();
        }
        try {
            PollingSchedulerSession session = pollingSchedulerBridge.createSession(nodeId);
            return new SuccessResponse(session.getNodeId()).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    /**
     * Pulls new scheduler messages for a polling session.
     * @param translet the translet
     * @return an array of messages
     */
    @Request("/pull")
    public RestResponse pull(@NonNull Translet translet) {
        String sessionId = translet.getParameter("sessionId");
        if (sessionId == null) {
            sessionId = translet.getParameter("nodeId");
        }
        try {
            PollingSchedulerSession session = pollingSchedulerBridge.getSession(sessionId);
            if (session != null) {
                String[] messages = pollingSchedulerBridge.pull(session);
                return new SuccessResponse(messages).ok();
            } else {
                return new FailureResponse().setError("not_found", "Session not found");
            }
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    /**
     * Executes a scheduler command via HTTP POST (for polling clients).
     * @param translet the translet
     * @return a success message
     */
    @RequestToPost("/execute")
    public RestResponse execute(@NonNull Translet translet) {
        String targetNodeId = translet.getParameter("nodeId");
        String command = translet.getParameter("command");
        String serviceName = translet.getParameter("serviceName");
        String scheduleId = translet.getParameter("scheduleId");
        String jobName = translet.getParameter("jobName");
        String loggingGroup = translet.getParameter("loggingGroup");
        String loadedLines = translet.getParameter("loadedLines");

        if (StringUtils.isEmpty(command)) {
            return new FailureResponse().setError("required", "Command is required");
        }
        if (StringUtils.isEmpty(targetNodeId)) {
            targetNodeId = nodeManager.getNodeId();
        }

        try {
            SchedulerRequestParameters parameters = new SchedulerRequestParameters();
            parameters.setCommand(command);
            parameters.setServiceName(serviceName);
            parameters.setScheduleId(scheduleId);
            parameters.setJobName(jobName);
            parameters.setLoggingGroup(loggingGroup);
            if (StringUtils.hasText(loadedLines)) {
                parameters.setLoadedLines(Integer.parseInt(loadedLines));
            }

            schedulerManager.dispatch(targetNodeId, parameters);
            return new SuccessResponse("Scheduler command initiated successfully").ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

}

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
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
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
    public List<Map<String, Object>> listNodes() {
        return nodeConsoleHelper.getNodes(true);
    }

    /**
     * Joins a polling session.
     * @param nodeId the node ID to join
     * @return the node ID
     */
    @Request("/join")
    @Transform(format = FormatType.TEXT)
    public String join(String nodeId) {
        if (StringUtils.isEmpty(nodeId)) {
            nodeId = nodeManager.getNodeId();
        }
        PollingSchedulerSession session = pollingSchedulerBridge.createSession(nodeId);
        return session.getNodeId();
    }

    /**
     * Pulls new scheduler messages for a polling session.
     * @param translet the translet
     * @return an array of messages
     */
    @Request("/pull")
    @Transform(format = FormatType.JSON)
    public String[] pull(@NonNull Translet translet) {
        String sessionId = translet.getParameter("sessionId");
        if (sessionId == null) {
            sessionId = translet.getParameter("nodeId");
        }
        PollingSchedulerSession session = pollingSchedulerBridge.getSession(sessionId);
        if (session != null) {
            return pollingSchedulerBridge.pull(session);
        } else {
            return null;
        }
    }

    /**
     * Executes a scheduler command via HTTP POST (for polling clients).
     * @param translet the translet
     * @return a success message
     */
    @RequestToPost("/execute")
    public Map<String, String> execute(@NonNull Translet translet) {
        String targetNodeId = translet.getParameter("nodeId");
        String command = translet.getParameter("command");
        String serviceName = translet.getParameter("serviceName");
        String scheduleId = translet.getParameter("scheduleId");
        String jobName = translet.getParameter("jobName");

        if (StringUtils.isEmpty(command)) {
            throw new IllegalArgumentException("Command is required");
        }
        if (StringUtils.isEmpty(targetNodeId)) {
            targetNodeId = nodeManager.getNodeId();
        }

        SchedulerRequestParameters parameters = new SchedulerRequestParameters();
        parameters.putValue(SchedulerRequestParameters.command, command);
        if (serviceName != null) parameters.putValue(SchedulerRequestParameters.serviceName, serviceName);
        if (scheduleId != null) parameters.putValue(SchedulerRequestParameters.scheduleId, scheduleId);
        if (jobName != null) parameters.putValue(SchedulerRequestParameters.jobName, jobName);

        schedulerManager.dispatch(targetNodeId, parameters);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Scheduler command initiated successfully");
        return result;
    }

}

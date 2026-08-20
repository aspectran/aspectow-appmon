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
package com.aspectran.aspectow.console.cluster;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.console.auth.UserInfo;
import com.aspectran.aspectow.console.build.manager.BuildExecutionInfo;
import com.aspectran.aspectow.console.build.manager.RemoteBuildDeployManager;
import com.aspectran.aspectow.node.config.GroupInfo;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.management.commands.CommandRequestParameters;
import com.aspectran.aspectow.node.management.commands.RemoteCommandManager;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ClusterActivity provides views and data for monitoring and managing cluster nodes.
 *
 * <p>Created: 2026-04-19</p>
 */
@Component("/cluster")
@Profile("[console.ui, console.custom-ui]")
public class ClusterActivity {

    private final NodeManager nodeManager;

    private final NodeConsoleHelper nodeConsoleHelper;

    private final RemoteCommandManager remoteCommandManager;

    private final RemoteBuildDeployManager remoteBuildDeployManager;

    /**
     * Constructs a new {@code ClusterActivity} with the specified node manager,
     * node console helper, remote command manager, and remote build deploy manager.
     * @param nodeManager the node manager
     * @param nodeConsoleHelper the node console helper
     * @param remoteCommandManager the remote command manager
     * @param remoteBuildDeployManager the remote build deploy manager
     */
    @Autowired
    public ClusterActivity(NodeManager nodeManager,
                           NodeConsoleHelper nodeConsoleHelper,
                           RemoteCommandManager remoteCommandManager,
                           RemoteBuildDeployManager remoteBuildDeployManager) {
        this.nodeManager = nodeManager;
        this.nodeConsoleHelper = nodeConsoleHelper;
        this.remoteCommandManager = remoteCommandManager;
        this.remoteBuildDeployManager = remoteBuildDeployManager;
    }

    /**
     * Displays the cluster nodes list page.
     * @return a map of attributes for rendering the view
     */
    @Request("/nodes/")
    @Dispatch("cluster/nodes")
    @Action("page")
    public Map<String, Object> listNodes(@NonNull Translet translet) {
        String clusterMode = nodeManager.getClusterConfig().getMode();
        List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(true);
        NodeInfo nodeInfo = nodeManager.getNodeInfoHolder().getNodeInfo(nodeManager.getNodeId());

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Cluster Nodes");
        model.put("style", "nodes-page");
        model.put("group", "cluster-menu");
        model.put("clusterMode", clusterMode);
        model.put("nodes", nodes);
        model.put("node", nodeConsoleHelper.createNodeMap(nodeInfo, true, true));

        UserInfo userInfo = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
        boolean isDemo = (userInfo != null && userInfo.hasRole("DEMO"));
        model.put("token", AppMonTokenIssuer.issueToken(30, isDemo));

        if (nodeManager.getClusterConfig().isGatewayMode()) {
            List<GroupInfo> groupInfos = nodeManager.getGroupInfoList();
            if (groupInfos != null && !groupInfos.isEmpty()) {
                List<Map<String, Object>> groups = new ArrayList<>();
                for (GroupInfo groupInfo : groupInfos) {
                    Map<String, Object> groupMap = new HashMap<>();
                    groupMap.put("id", groupInfo.getId());
                    groupMap.put("title", groupInfo.getTitle());
                    groupMap.put("description", groupInfo.getDescription());
                    groups.add(groupMap);
                }
                model.put("groups", groups);

                Map<String, List<Map<String, Object>>> groupedNodes = nodes.stream()
                         .filter(n -> n.get("group") != null)
                         .collect(Collectors.groupingBy(n -> (String) n.get("group")));
                model.put("groupedNodes", groupedNodes);
            }
        }
        return model;
    }

    /**
     * Issues a new authentication token for WebSocket connection.
     * @return the issued token
     */
    @Request("/token")
    public RestResponse refreshToken(@NonNull Translet translet) {
        UserInfo userInfo = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
        boolean isDemo = (userInfo != null && userInfo.hasRole("DEMO"));
        return new SuccessResponse(AppMonTokenIssuer.issueToken(30, isDemo)).ok();
    }

    /**
     * Returns the current cluster node list in JSON format for sync.
     * @return the REST response containing the node list
     */
    @Request("/nodes/list")
    public RestResponse listNodesJson() {
        List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(true);
        return new SuccessResponse(nodes).ok();
    }

    /**
     * Dispatches an in-JVM service restart command to a specific node using RemoteCommandManager.
     * Recreates the ActivityContext and reloads classes without stopping the JVM process.
     * @param translet the active translet
     * @return the REST response indicating success or failure
     */
    @Request("/nodes/${nodeId}/restart-service")
    public RestResponse restartNode(@NonNull Translet translet) {
        String nodeId = translet.getParameter("nodeId");
        if (nodeId != null) {
            try {
                CommandRequestParameters commandRequest = new CommandRequestParameters();
                commandRequest.setHeader("execute");
                commandRequest.setTargetNodeId(nodeId);

                CommandParameters commandParams = new CommandParameters();
                commandParams.readFrom("command: restart");
                commandRequest.setCommand(commandParams);

                remoteCommandManager.process(commandRequest);
                return new SuccessResponse("Service restart command dispatched to " + nodeId).ok();
            } catch (Exception e) {
                return new FailureResponse().setError("error", "Failed to dispatch service restart command: " + e.getMessage());
            }
        } else {
            return new FailureResponse().setError("error", "Missing nodeId parameter");
        }
    }

    /**
     * Dispatches a full OS process/daemon restart command to a specific node.
     * @param translet the active translet
     * @return the REST response indicating success or failure
     */
    @Request("/nodes/${nodeId}/restart-server")
    public RestResponse restartServer(@NonNull Translet translet) {
        String nodeId = translet.getParameter("nodeId");
        if (nodeId != null) {
            try {
                BuildExecutionInfo info = new BuildExecutionInfo();
                info.setTargetNodeId(nodeId);
                info.setScriptName("daemon.sh");
                info.getParameters().put("action", "restart");

                remoteBuildDeployManager.dispatch(info);
                return new SuccessResponse("Server restart command dispatched to " + nodeId).ok();
            } catch (Exception e) {
                return new FailureResponse().setError("error", "Failed to dispatch server restart command: " + e.getMessage());
            }
        } else {
            return new FailureResponse().setError("error", "Missing nodeId parameter");
        }
    }

    /**
     * Dispatches a pause command to a specific node using RemoteCommandManager.
     * @param translet the active translet
     * @return the REST response indicating success or failure
     */
    @Request("/nodes/${nodeId}/pause")
    public RestResponse pauseNode(@NonNull Translet translet) {
        String nodeId = translet.getParameter("nodeId");
        if (nodeId != null) {
            try {
                CommandRequestParameters commandRequest = new CommandRequestParameters();
                commandRequest.setHeader("execute");
                commandRequest.setTargetNodeId(nodeId);

                CommandParameters commandParams = new CommandParameters();
                commandParams.readFrom("command: pause");
                commandRequest.setCommand(commandParams);

                remoteCommandManager.process(commandRequest);
                return new SuccessResponse("Pause command dispatched to " + nodeId).ok();
            } catch (Exception e) {
                return new FailureResponse().setError("error", "Failed to dispatch pause command: " + e.getMessage());
            }
        } else {
            return new FailureResponse().setError("error", "Missing nodeId parameter");
        }
    }

    /**
     * Dispatches a resume command to a specific node using RemoteCommandManager.
     * @param translet the active translet
     * @return the REST response indicating success or failure
     */
    @Request("/nodes/${nodeId}/resume")
    public RestResponse resumeNode(@NonNull Translet translet) {
        String nodeId = translet.getParameter("nodeId");
        if (nodeId != null) {
            try {
                CommandRequestParameters commandRequest = new CommandRequestParameters();
                commandRequest.setHeader("execute");
                commandRequest.setTargetNodeId(nodeId);

                CommandParameters commandParams = new CommandParameters();
                commandParams.readFrom("command: resume");
                commandRequest.setCommand(commandParams);

                remoteCommandManager.process(commandRequest);
                return new SuccessResponse("Resume command dispatched to " + nodeId).ok();
            } catch (Exception e) {
                return new FailureResponse().setError("error", "Failed to dispatch resume command: " + e.getMessage());
            }
        } else {
            return new FailureResponse().setError("error", "Missing nodeId parameter");
        }
    }

}

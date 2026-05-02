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
package com.aspectran.aspectow.console.commands;

import com.aspectran.aspectow.console.cluster.NodeConsoleHelper;
import com.aspectran.aspectow.console.commands.manager.RemoteCommandManager;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * RemoteCommandsActivity provides views and REST API endpoints for managing
 * cluster nodes and executing remote file commands.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component(NODES_BASE_PATH + "/commands")
public class RemoteCommandsActivity {

    private final NodeManager nodeManager;

    private final RemoteCommandManager remoteCommandManager;

    private final NodeConsoleHelper nodeConsoleHelper;

    @Autowired
    public RemoteCommandsActivity(NodeManager nodeManager,
                                  RemoteCommandManager remoteCommandManager,
                                  NodeConsoleHelper nodeConsoleHelper) {
        this.nodeManager = nodeManager;
        this.remoteCommandManager = remoteCommandManager;
        this.nodeConsoleHelper = nodeConsoleHelper;
    }

    /**
     * Displays the node commands page.
     * @param nodeId the node ID
     * @return a map of attributes for rendering the view
     */
    @Request
    @Dispatch("nodes/commands")
    @Action("page")
    public Map<String, Object> nodeCommands(String nodeId) {
        String clusterMode = nodeManager.getClusterConfig().getMode();
        List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(true);
        NodeInfo nodeInfo = (nodeId != null ? nodeManager.getNodeInfoHolder().getNodeInfo(nodeId) : null);
        if (nodeId != null && nodeInfo == null) {
            throw new IllegalArgumentException("No node found with ID: " + nodeId);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Remote Commands");
        model.put("style", "commands-page");
        model.put("group", "cluster-menu");
        model.put("clusterMode", clusterMode);
        model.put("nodes", nodes);
        if (nodeInfo != null) {
            model.put("node", nodeConsoleHelper.createNodeMap(nodeInfo, true, true));
        }
        return model;
    }

    /**
     * Lists all registered nodes with their current status.
     * @return a list of node information maps
     */
    @Request("/list")
    public List<Map<String, Object>> listCommands() {
        return nodeConsoleHelper.getNodes(true);
    }

    /**
     * Creates a command to be sent to a specific node or all nodes.
     * @return a success message
     */
    @RequestToPost("/execute")
    public Map<String, String> executeCommand(@NonNull Translet translet) throws Exception {
        String targetNodeId = translet.getParameter("nodeId");
        String command = translet.getParameter("command");

        if (StringUtils.isEmpty(command)) {
            throw new IllegalArgumentException("Command is required");
        }
        if (StringUtils.isEmpty(targetNodeId)) {
            targetNodeId = nodeManager.getNodeId();
        }

        remoteCommandManager.dispatch(targetNodeId, command);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Command initiated successfully for node: " + targetNodeId);
        return result;
    }

}

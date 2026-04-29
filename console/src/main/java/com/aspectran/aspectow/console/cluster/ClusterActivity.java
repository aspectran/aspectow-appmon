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
package com.aspectran.aspectow.console.cluster;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;

import java.util.List;
import java.util.Map;

import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * ClusterActivity provides views and data for monitoring and managing cluster nodes.
 *
 * <p>Created: 2026-04-19</p>
 */
@Component(NODES_BASE_PATH)
public class ClusterActivity {

    private final NodeManager nodeManager;

    private final NodeConsoleHelper nodeConsoleHelper;

    @Autowired
    public ClusterActivity(NodeManager nodeManager, NodeConsoleHelper nodeConsoleHelper) {
        this.nodeManager = nodeManager;
        this.nodeConsoleHelper = nodeConsoleHelper;
    }

    /**
     * Displays the cluster nodes list page.
     * @return a map of attributes for rendering the view
     */
    @Request("/list")
    @Dispatch("nodes/list")
    @Action("page")
    public Map<String, Object> listNodes() {
        String clusterMode = nodeManager.getClusterConfig().getMode();
        List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(false);
        NodeInfo nodeInfo = nodeManager.getNodeInfoHolder().getNodeInfo(nodeManager.getNodeId());
        return Map.of(
                "title", "Cluster Nodes",
                "style", "nodes-page",
                "group", "cluster-menu",
                "clusterMode", clusterMode,
                "nodes", nodes,
                "node", nodeConsoleHelper.createNodeMap(nodeInfo, true, true),
                "token", AppMonTokenIssuer.issueToken(30)
        );
    }

    /**
     * Issues a new authentication token for WebSocket connection.
     * @return the issued token
     */
    @Request("/token")
    @Transform(format = FormatType.TEXT)
    public String refreshToken() {
        return AppMonTokenIssuer.issueToken(30);
    }

}

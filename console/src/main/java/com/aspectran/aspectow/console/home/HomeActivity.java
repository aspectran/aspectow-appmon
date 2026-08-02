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
package com.aspectran.aspectow.console.home;

import com.aspectran.aspectow.console.cluster.NodeConsoleHelper;
import com.aspectran.aspectow.node.config.GroupInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Activity for the home page of the Aspectow Management Console.
 */
@Component("/")
@Profile("[console.ui, console.custom-ui]")
public class HomeActivity {

    private final NodeManager nodeManager;

    private final NodeConsoleHelper nodeConsoleHelper;

    /**
     * Constructs a new {@code HomeActivity} with the specified node manager and node console helper.
     * @param nodeManager the node manager
     * @param nodeConsoleHelper the node console helper
     */
    @Autowired
    public HomeActivity(NodeManager nodeManager, NodeConsoleHelper nodeConsoleHelper) {
        this.nodeManager = nodeManager;
        this.nodeConsoleHelper = nodeConsoleHelper;
    }

    /**
     * Renders the home page of the Aspectow Management Console.
     * @return the model map containing attributes for rendering the home page
     */
    @Request("/")
    @Dispatch("home/home")
    @Action("page")
    public Map<String, Object> home() {
        String clusterMode = nodeManager.getClusterConfig().getMode();
        List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(false);

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Aspectow Console");
        model.put("headline", "Aspectow Management Console");
        model.put("include", "home");
        model.put("style", "dashboard-page");
        model.put("clusterMode", clusterMode);
        model.put("myNodeId", nodeManager.getNodeId());
        model.put("nodes", nodes);

        if (nodeManager.getClusterConfig().isGatewayMode()) {
            List<GroupInfo> groupInfos = nodeManager.getGroupInfoList();
            if (groupInfos != null && !groupInfos.isEmpty()) {
                List<Map<String, Object>> groups = new ArrayList<>();
                for (GroupInfo groupInfo : groupInfos) {
                    Map<String, Object> groupMap = new HashMap<>();
                    groupMap.put("id", groupInfo.getId());
                    groupMap.put("title", groupInfo.getTitle());
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

}

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

import com.aspectran.aspectow.node.config.EndpointConfig;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeRegistry;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Profile;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NodeConsoleHelper provides methods for transforming cluster node information
 * into UI-ready data.
 *
 * <p>Created: 2026-04-19</p>
 */
@Component
@Profile("[console.ui, console.custom-ui]")
public class NodeConsoleHelper {

    private final NodeManager nodeManager;

    /**
     * Constructs a new {@code NodeConsoleHelper} with the specified node manager.
     * @param nodeManager the node manager
     */
    @Autowired
    public NodeConsoleHelper(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    /**
     * Retrieves the list of cluster nodes formatted as maps for UI rendering.
     * @param includeEndpoint whether to include endpoint configuration in the node maps
     * @return the list of maps representing cluster nodes
     */
    public List<Map<String, Object>> getNodes(boolean includeEndpoint) {
        List<NodeInfo> configuredNodes = nodeManager.getNodeInfoHolder().getNodeInfoList();
        NodeRegistry nodeRegistry = nodeManager.getNodeRegistry();

        if (nodeManager.getClusterConfig().isDirectMode() || nodeRegistry == null) {
            List<Map<String, Object>> result = new ArrayList<>(configuredNodes.size());
            for (NodeInfo info : configuredNodes) {
                boolean alive = info.getId().equals(nodeManager.getNodeId());
                result.add(createNodeMap(info, alive, includeEndpoint));
            }
            return result;
        }

        // Use a map to merge configured nodes and registered nodes
        Map<String, NodeInfo> mergedNodes = new LinkedHashMap<>();
        for (NodeInfo info : configuredNodes) {
            mergedNodes.put(info.getId(), info);
        }

        List<NodeInfo> registeredNodes = nodeRegistry.getNodes();
        if (registeredNodes != null) {
            for (NodeInfo info : registeredNodes) {
                mergedNodes.put(info.getId(), info);
            }
        }

        List<NodeInfo> allNodes = new ArrayList<>(mergedNodes.values());
        allNodes.sort(Comparator.comparing(NodeInfo::getId, Comparator.nullsLast(String::compareTo)));

        Map<String, String> pulses = nodeRegistry.getAllPulses();
        List<Map<String, Object>> result = new ArrayList<>(allNodes.size());
        long now = System.currentTimeMillis();
        long timeout = 15000; // 15 seconds timeout

        for (NodeInfo info : allNodes) {
            String nodeId = info.getId();
            boolean alive = false;
            String pulseStr = (pulses != null ? pulses.get(nodeId) : null);
            if (pulseStr != null) {
                try {
                    long lastPulse = Long.parseLong(pulseStr);
                    alive = (now - lastPulse <= timeout);
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }
            result.add(createNodeMap(info, alive, includeEndpoint));
        }
        return result;
    }

    /**
     * Creates a map representation of the specified node information.
     * @param info the node information
     * @param alive whether the node is currently alive
     * @param includeEndpoint whether to include endpoint details
     * @return the map representation of the node info
     */
    public Map<String, Object> createNodeMap(@NonNull NodeInfo info, boolean alive, boolean includeEndpoint) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", info.getId());
        map.put("group", info.getGroup());

        String title = info.getTitle();
        if (title == null) {
            title = info.getId();
        }
        map.put("title", title);

        map.put("host", info.getHost());
        map.put("port", info.getPort());
        if (includeEndpoint) {
            EndpointConfig endpointConfig = info.getEndpointConfig();
            if (endpointConfig != null) {
                Map<String, String> endpointMap = new HashMap<>();
                endpointMap.put("mode", endpointConfig.getMode());
                endpointMap.put("path", endpointConfig.getPath());
                map.put("endpoint", endpointMap);
            }
        }

        String status = info.getStatus();
        if (!alive) {
            status = "dead";
        } else if (status == null) {
            status = "live";
        }
        map.put("status", status);
        map.put("console", info.isConsole());
        map.put("hasNodeManager", info.hasNodeManager());
        map.put("hasSchedulerManager", info.hasSchedulerManager());
        map.put("hasCommandManager", info.hasCommandManager());
        return map;
    }

}

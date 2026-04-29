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

import com.aspectran.aspectow.node.config.EndpointConfig;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeRegistry;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
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
public class NodeConsoleHelper {

    private final NodeManager nodeManager;

    @Autowired
    public NodeConsoleHelper(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public List<Map<String, Object>> getNodes(boolean includeEndpoint) {
        List<NodeInfo> configuredNodes = nodeManager.getNodeInfoHolder().getNodeInfoList();
        NodeRegistry nodeRegistry = nodeManager.getNodeRegistry();

        if (nodeManager.getClusterConfig().isDirectMode() || nodeRegistry == null) {
            List<Map<String, Object>> result = new ArrayList<>(configuredNodes.size());
            for (NodeInfo info : configuredNodes) {
                boolean alive = info.getNodeId().equals(nodeManager.getNodeId());
                result.add(createNodeMap(info, alive, includeEndpoint));
            }
            return result;
        }

        // Use a map to merge configured nodes and registered nodes
        Map<String, NodeInfo> mergedNodes = new LinkedHashMap<>();
        for (NodeInfo info : configuredNodes) {
            mergedNodes.put(info.getNodeId(), info);
        }

        List<NodeInfo> registeredNodes = nodeRegistry.getNodes();
        if (registeredNodes != null) {
            for (NodeInfo info : registeredNodes) {
                mergedNodes.put(info.getNodeId(), info);
            }
        }

        Map<String, String> pulses = nodeRegistry.getAllPulses();
        List<Map<String, Object>> result = new ArrayList<>(mergedNodes.size());
        long now = System.currentTimeMillis();
        long timeout = 15000; // 15 seconds timeout

        for (NodeInfo info : mergedNodes.values()) {
            String nodeId = info.getNodeId();
            boolean alive = false;
            String pulseStr = (pulses != null ? pulses.get(nodeId) : null);
            if (pulseStr != null) {
                try {
                    long lastPulse = Long.parseLong(pulseStr);
                    alive = (now - lastPulse <= timeout);
                } catch (NumberFormatException ignored) {
                }
            }
            result.add(createNodeMap(info, alive, includeEndpoint));
        }
        return result;
    }

    public Map<String, Object> createNodeMap(@NonNull NodeInfo info, boolean alive, boolean includeEndpoint) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", info.getNodeId());
        map.put("group", info.getGroup());
        map.put("title", info.getTitle());
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
        return map;
    }

}

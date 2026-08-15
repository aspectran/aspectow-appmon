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
package com.aspectran.aspectow.node.manager;

import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Provides an API for retrieving information about registered cluster nodes
 * from the Redis storage.
 */
public class NodeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NodeRegistry.class);

    private final String clusterId;

    private final RedisConnectionPool connectionPool;

    private final List<NodeRegistryListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new NodeRegistry.
     * @param clusterId the cluster ID
     * @param connectionPool the Redis connection pool
     */
    public NodeRegistry(String clusterId, RedisConnectionPool connectionPool) {
        this.clusterId = clusterId;
        this.connectionPool = connectionPool;
    }

    /**
     * Adds a listener for node registry events.
     * @param listener the listener to add
     */
    public void addListener(NodeRegistryListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener for node registry events.
     * @param listener the listener to remove
     */
    public void removeListener(NodeRegistryListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Notifies all registered listeners that a node has been registered or re-registered.
     * @param nodeInfo the node information
     */
    public void notifyNodeRegistered(NodeInfo nodeInfo) {
        if (nodeInfo == null || listeners.isEmpty()) {
            return;
        }
        for (NodeRegistryListener listener : listeners) {
            try {
                listener.onNodeRegistered(nodeInfo);
            } catch (Exception e) {
                logger.error("Error invoking NodeRegistryListener.onNodeRegistered() for node '{}'", nodeInfo.getId(), e);
            }
        }
    }

    /**
     * Notifies all registered listeners that a node has been unregistered or evicted.
     * @param nodeId the ID of the unregistered node
     */
    public void notifyNodeUnregistered(String nodeId) {
        if (nodeId == null || listeners.isEmpty()) {
            return;
        }
        for (NodeRegistryListener listener : listeners) {
            try {
                listener.onNodeUnregistered(nodeId);
            } catch (Exception e) {
                logger.error("Error invoking NodeRegistryListener.onNodeUnregistered() for node '{}'", nodeId, e);
            }
        }
    }

    /**
     * Retrieves all registered nodes as NodeInfo objects.
     * @return a list of NodeInfo objects
     */
    public List<NodeInfo> getNodes() {
        Map<String, String> rawNodes = getAllNodes();
        List<NodeInfo> nodes = new ArrayList<>(rawNodes.size());
        for (String aponData : rawNodes.values()) {
            try {
                NodeInfo nodeInfo = new NodeInfo();
                nodeInfo.readFrom(aponData);
                nodes.add(nodeInfo);
            } catch (IOException e) {
                logger.warn("Failed to parse node info APON data", e);
            }
        }
        return nodes;
    }

    /**
     * Retrieves all registered nodes from Redis as raw APON strings.
     * @return a map of node IDs to their metadata (APON strings)
     */
    public Map<String, String> getAllNodes() {
        if (connectionPool == null || !connectionPool.isAvailable()) {
            return Collections.emptyMap();
        }
        String key = NodeMessageProtocol.getNodesHashKey(clusterId);
        if (logger.isTraceEnabled()) {
            logger.trace("Retrieving all nodes from Redis hash: {}", key);
        }
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            return connection.sync().hgetall(key);
        } catch (Exception e) {
            if (connectionPool.isAvailable()) {
                logger.error("Failed to retrieve nodes from Redis registry", e);
            }
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves all registered groups from Redis for the current cluster.
     * @return a map of group IDs to their metadata (APON strings)
     */
    public Map<String, String> getAllGroups() {
        if (connectionPool == null || !connectionPool.isAvailable()) {
            return Collections.emptyMap();
        }
        String key = NodeMessageProtocol.getGroupsHashKey(clusterId);
        if (logger.isTraceEnabled()) {
            logger.trace("Retrieving all groups from Redis hash: {}", key);
        }
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            return connection.sync().hgetall(key);
        } catch (Exception e) {
            if (connectionPool.isAvailable()) {
                logger.error("Failed to retrieve groups from Redis registry", e);
            }
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves all registered applications from Redis for a specific group.
     * @param groupId the group ID
     * @return a map of application IDs to their metadata (APON strings)
     */
    public Map<String, String> getAllApps(String groupId) {
        String key = NodeMessageProtocol.getAppsHashKey(clusterId, groupId);
        String orderKey = NodeMessageProtocol.getAppsOrderKey(clusterId, groupId);
        if (logger.isTraceEnabled()) {
            logger.trace("Retrieving all apps for group: {} from Redis hash: {}", groupId, key);
        }
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            RedisCommands<String, String> sync = connection.sync();
            List<String> order = sync.lrange(orderKey, 0, -1);
            Map<String, String> allApps = sync.hgetall(key);
            if (order != null && !order.isEmpty()) {
                Map<String, String> orderedApps = new LinkedHashMap<>();
                for (String appId : order) {
                    String aponData = allApps.get(appId);
                    if (aponData != null) {
                        orderedApps.put(appId, aponData);
                    }
                }
                if (orderedApps.size() < allApps.size()) {
                    for (Map.Entry<String, String> entry : allApps.entrySet()) {
                        if (!orderedApps.containsKey(entry.getKey())) {
                            orderedApps.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
                return orderedApps;
            } else {
                return allApps;
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve apps for group {} from Redis registry", groupId, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves the last pulse timestamps for all nodes.
     * @return a map of node IDs to their last pulse timestamps
     */
    public Map<String, String> getAllPulses() {
        String key = NodeMessageProtocol.getPulsesHashKey(clusterId);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            return connection.sync().hgetall(key);
        } catch (Exception e) {
            logger.error("Failed to retrieve node pulses from Redis registry", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves a specific node's information.
     * @param nodeId the node ID
     * @return the node metadata string, or null if not found
     */
    public String getNode(String nodeId) {
        String key = NodeMessageProtocol.getNodesHashKey(clusterId);
        if (logger.isTraceEnabled()) {
            logger.trace("Retrieving node info for: {} from {}", nodeId, key);
        }
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            return connection.sync().hget(key, nodeId);
        } catch (Exception e) {
            logger.error("Failed to retrieve node info for {} from Redis registry", nodeId, e);
            return null;
        }
    }

    /**
     * Retrieves a specific node's information as a NodeInfo object.
     * @param nodeId the node ID
     * @return the node information as a NodeInfo object, or null if not found
     */
    public NodeInfo getNodeInfo(String nodeId) {
        String aponData = getNode(nodeId);
        if (aponData == null) {
            return null;
        }
        try {
            NodeInfo nodeInfo = new NodeInfo();
            nodeInfo.readFrom(aponData);
            return nodeInfo;
        } catch (IOException e) {
            logger.warn("Failed to parse node info APON data", e);
            return null;
        }
    }

    /**
     * Checks if the node with the specified ID is found in the registry.
     * @param nodeId the node ID to check
     * @return true if the node exists in the registry, false otherwise
     */
    public boolean isFound(String nodeId) {
        return getNode(nodeId) != null;
    }

    /**
     * Checks if a node is considered 'live' based on its last pulse timestamp.
     * @param nodeId the node ID
     * @param timeoutMillis the timeout threshold in milliseconds
     * @return true if the node is live, false otherwise
     */
    public boolean isLive(String nodeId, long timeoutMillis) {
        String key = NodeMessageProtocol.getPulsesHashKey(clusterId);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            String pulse = connection.sync().hget(key, nodeId);
            if (pulse != null) {
                try {
                    long lastPulse = Long.parseLong(pulse);
                    return (System.currentTimeMillis() - lastPulse <= timeoutMillis);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to check liveness for node {} from Redis registry", nodeId, e);
        }
        return false;
    }

    /**
     * Explicitly removes a node and its pulse from the registry.
     * Also performs metadata garbage collection.
     * @param nodeId the node ID to remove
     */
    public void removeNode(String nodeId) {
        String nodesKey = NodeMessageProtocol.getNodesHashKey(clusterId);
        String pulsesKey = NodeMessageProtocol.getPulsesHashKey(clusterId);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            RedisCommands<String, String> sync = connection.sync();
            sync.hdel(nodesKey, nodeId);
            sync.hdel(pulsesKey, nodeId);
            cleanupOrphanedGroups(sync);
            notifyNodeUnregistered(nodeId);
        } catch (Exception e) {
            logger.error("Failed to remove node '{}' and metadata from cluster '{}'", nodeId, clusterId, e);
        }
    }

    /**
     * Evicts nodes that have not sent a pulse within the specified timeout.
     * Also cleans up orphaned group and app metadata.
     * @param timeoutMillis the timeout threshold in milliseconds
     */
    public void evictZombieNodes(long timeoutMillis) {
        String nodesKey = NodeMessageProtocol.getNodesHashKey(clusterId);
        String pulsesKey = NodeMessageProtocol.getPulsesHashKey(clusterId);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            RedisCommands<String, String> sync = connection.sync();
            Map<String, String> pulses = sync.hgetall(pulsesKey);
            long now = System.currentTimeMillis();
            List<String> evictedNodeIds = new ArrayList<>();
            for (Map.Entry<String, String> entry : pulses.entrySet()) {
                String nodeId = entry.getKey();
                try {
                    long lastPulse = Long.parseLong(entry.getValue());
                    if (now - lastPulse > timeoutMillis) {
                        logger.info("Evicting zombie node '{}' from cluster '{}'", nodeId, clusterId);
                        sync.hdel(nodesKey, nodeId);
                        sync.hdel(pulsesKey, nodeId);
                        evictedNodeIds.add(nodeId);
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            if (!evictedNodeIds.isEmpty()) {
                cleanupOrphanedGroups(sync);
                for (String evictedNodeId : evictedNodeIds) {
                    notifyNodeUnregistered(evictedNodeId);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to evict zombie nodes and metadata from cluster '{}'", clusterId, e);
        }
    }

    /**
     * Removes groups that no longer have active nodes.
     * @param sync the Redis commands
     */
    private void cleanupOrphanedGroups(@NonNull RedisCommands<String, String> sync) {
        String nodesKey = NodeMessageProtocol.getNodesHashKey(clusterId);
        String groupsKey = NodeMessageProtocol.getGroupsHashKey(clusterId);

        Map<String, String> remainingNodes = sync.hgetall(nodesKey);
        Set<String> activeGroups = new HashSet<>();
        for (String aponData : remainingNodes.values()) {
            try {
                NodeInfo info = new NodeInfo();
                info.readFrom(aponData);
                if (info.getGroup() != null) {
                    activeGroups.add(info.getGroup());
                }
            } catch (Exception e) {
                // ignore
            }
        }

        Map<String, String> registeredGroups = sync.hgetall(groupsKey);
        for (String gid : registeredGroups.keySet()) {
            if (!activeGroups.contains(gid)) {
                logger.info("Cleaning up orphaned group metadata: {} (Cluster: {})", gid, clusterId);
                sync.hdel(groupsKey, gid);
            }
        }
    }

    /**
     * Stops the node registry and performs any necessary cleanup.
     */
    public void stop() {
        listeners.clear();
    }

}

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provides an API for retrieving information about registered cluster nodes
 * from the Redis storage.
 */
public class NodeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NodeRegistry.class);

    private final String clusterId;

    private final RedisConnectionPool connectionPool;

    public NodeRegistry(String clusterId, RedisConnectionPool connectionPool) {
        this.clusterId = clusterId;
        this.connectionPool = connectionPool;
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
        String key = NodeMessageProtocol.getNodesHashKey(clusterId);
        logger.debug("Retrieving all nodes from Redis hash: {}", key);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            return connection.sync().hgetall(key);
        } catch (Exception e) {
            logger.error("Failed to retrieve nodes from Redis registry", e);
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
        logger.debug("Retrieving node info for: {} from {}", nodeId, key);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            return connection.sync().hget(key, nodeId);
        } catch (Exception e) {
            logger.error("Failed to retrieve node info for {} from Redis registry", nodeId, e);
            return null;
        }
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

}

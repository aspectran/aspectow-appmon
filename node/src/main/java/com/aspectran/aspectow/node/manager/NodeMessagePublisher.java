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

import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.List;

/**
 * Provides methods to publish management control messages and transparent
 * application data to Redis Pub/Sub channels for inter-node communication relay.
 */
public class NodeMessagePublisher {

    private final String clusterId;

    private final String nodeId;

    private final RedisConnectionPool connectionPool;

    /**
     * Constructs a new NodeMessagePublisher.
     * @param clusterId the cluster ID
     * @param nodeId the node ID
     * @param connectionPool the Redis connection pool
     */
    public NodeMessagePublisher(String clusterId, String nodeId, RedisConnectionPool connectionPool) {
        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.connectionPool = connectionPool;
    }

    /**
     * Returns the cluster ID.
     * @return the cluster ID
     */
    public String getClusterId() {
        return clusterId;
    }

    /**
     * Returns the node ID.
     * @return the node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Publishes a management control message for this node.
     * This method waits for the publication to complete.
     * @param message the message to publish
     * @throws Exception if an error occurs during publication
     */
    public void publishControl(String message) throws Exception {
        publishControl(nodeId, message);
    }

    /**
     * Publishes a management control message to a specific node.
     * This method waits for the publication to complete.
     * @param targetNodeId the ID of the node to receive the message
     * @param message the message to publish
     * @throws Exception if an error occurs during publication
     */
    public void publishControl(String targetNodeId, String message) throws Exception {
        String channel = NodeMessageProtocol.getControlChannel(clusterId, targetNodeId);
        syncPublish(channel, message);
    }

    /**
     * Publishes a management control message to a specific node.
     * This method waits for the publication to complete.
     * @param targetNodeId the ID of the node to receive the message
     * @param message the message to publish
     * @throws Exception if an error occurs during publication
     */
    public void publishControl(String category, String targetNodeId, String message) throws Exception {
        String channel = NodeMessageProtocol.getControlChannel(category, clusterId, targetNodeId);
        syncPublish(channel, message);
    }

    /**
     * Publishes a transparent application message to be relayed from this node.
     * This method sends the message asynchronously and does not wait for completion.
     * @param category the category of the relay message
     * @param message the message to publish
     * @throws Exception if an error occurs while obtaining a connection
     */
    public void publishRelay(String category, String message) throws Exception {
        String channel = NodeMessageProtocol.getRelayChannel(category, clusterId, nodeId);
        asyncPublish(channel, message);
    }

    /**
     * Publishes a transparent application message to be relayed to a specific node.
     * @param category the category of the relay message
     * @param targetNodeId the target node ID
     * @param message the message to publish
     * @throws Exception if an error occurs during publication
     */
    public void publishRelay(String category, String targetNodeId, String message) throws Exception {
        String channel = NodeMessageProtocol.getRelayChannel(category, clusterId, targetNodeId);
        asyncPublish(channel, message);
    }

    /**
     * Publishes a transparent application message to be relayed to a specific session on a node.
     * @param category the category of the relay message
     * @param targetNodeId the target node ID
     * @param sessionId the session ID
     * @param message the message to publish
     * @throws Exception if an error occurs during publication
     */
    public void publishRelay(String category, String targetNodeId, String sessionId, String message) throws Exception {
        String channel = NodeMessageProtocol.getRelayChannel(category, clusterId, targetNodeId, sessionId);
        asyncPublish(channel, message);
    }

    /**
     * Publishes multiple transparent application messages to be relayed to a specific session on a node.
     * Borrows a single connection from the pool to pipeline and publish all messages efficiently.
     * @param category the category of the relay message
     * @param targetNodeId the target node ID
     * @param sessionId the session ID
     * @param messages the list of messages to publish
     * @throws Exception if an error occurs during publication
     */
    public void publishRelay(String category, String targetNodeId, String sessionId, List<String> messages) throws Exception {
        String channel = NodeMessageProtocol.getRelayChannel(category, clusterId, targetNodeId, sessionId);
        asyncPublish(channel, messages);
    }

    /**
     * Publishes a message to a specific channel synchronously.
     * @param channel the channel to publish to
     * @param message the message to publish
     * @throws Exception if an error occurs during publication
     */
    public void syncPublish(String channel, String message) throws Exception {
        if (connectionPool.isAvailable()) {
            try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
                connection.sync().publish(channel, message);
            }
        }
    }

    /**
     * Publishes multiple messages to a specific channel synchronously using a single connection.
     * @param channel the channel to publish to
     * @param messages the list of messages to publish
     * @throws Exception if an error occurs during publication
     */
    public void syncPublish(String channel, List<String> messages) throws Exception {
        if (messages != null && !messages.isEmpty() && connectionPool.isAvailable()) {
            try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
                var syncCommands = connection.sync();
                for (String message : messages) {
                    syncCommands.publish(channel, message);
                }
            }
        }
    }

    /**
     * Publishes a message to a specific channel asynchronously.
     * @param channel the channel to publish to
     * @param message the message to publish
     * @throws Exception if an error occurs during publication
     */
    public void asyncPublish(String channel, String message) throws Exception {
        if (connectionPool.isAvailable()) {
            try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
                connection.async().publish(channel, message);
            }
        }
    }

    /**
     * Publishes multiple messages to a specific channel asynchronously using a single connection.
     * @param channel the channel to publish to
     * @param messages the list of messages to publish
     * @throws Exception if an error occurs during publication
     */
    public void asyncPublish(String channel, List<String> messages) throws Exception {
        if (messages != null && !messages.isEmpty() && connectionPool.isAvailable()) {
            try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
                var asyncCommands = connection.async();
                for (String message : messages) {
                    asyncCommands.publish(channel, message);
                }
            }
        }
    }

}

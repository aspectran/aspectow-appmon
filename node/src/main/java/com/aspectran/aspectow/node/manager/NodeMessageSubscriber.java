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
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.TYPE_CONTROL;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.TYPE_RELAY;

/**
 * Listens to management control and transparent relay channels and notifies
 * registered listeners based on the message category.
 */
public class NodeMessageSubscriber extends RedisPubSubAdapter<String, String> implements RedisConnectionStateListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeMessageSubscriber.class);

    private final String clusterId;

    private final String nodeId;

    private final RedisConnectionPool connectionPool;

    private final Set<NodeMessageListener> listeners = new CopyOnWriteArraySet<>();

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    /**
     * Constructs a new NodeMessageSubscriber.
     * @param clusterId the cluster ID
     * @param nodeId the node ID
     * @param connectionPool the Redis connection pool
     */
    public NodeMessageSubscriber(String clusterId, String nodeId, RedisConnectionPool connectionPool) {
        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.connectionPool = connectionPool;
    }

    /**
     * Returns the node ID of this subscriber.
     * @return the node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Adds a node message listener.
     * @param listener the listener to add
     */
    public void addListener(NodeMessageListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a node message listener.
     * @param listener the listener to remove
     */
    public void removeListener(NodeMessageListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void message(String pattern, String channel, String message) {
        message(channel, message);
    }

    @Override
    public void message(@NonNull String channel, String message) {
        // Expected patterns:
        // aspectow:cluster:control:<category>:<clusterId>:<nodeId>
        // aspectow:cluster:relay:<category>:<clusterId>:<nodeId>:<sessionId>

        String[] parts = channel.split(":");
        if (parts.length < 6) {
            return;
        }

        String type = parts[2]; // control or relay
        String category = parts[3]; // appmon, commands, etc.
        String targetNodeId = parts[5]; // node ID
        String sessionId = (parts.length > 6 ? parts[6] : null); // session ID

        if (!nodeId.equals(targetNodeId)) {
            return;
        }

        if (TYPE_CONTROL.equals(type)) {
            for (NodeMessageListener listener : listeners) {
                String listenerCategory = listener.getCategory();
                if (listenerCategory == null || listenerCategory.equals(category)) {
                    listener.onControlMessage(targetNodeId, message);
                }
            }
        } else if (TYPE_RELAY.equals(type)) {
            for (NodeMessageListener listener : listeners) {
                String listenerCategory = listener.getCategory();
                if (listenerCategory == null || listenerCategory.equals(category)) {
                    if (sessionId != null) {
                        listener.onRelayMessage(targetNodeId, sessionId, message);
                    } else {
                        listener.onRelayMessage(targetNodeId, message);
                    }
                }
            }
        }
    }

    /**
     * Starts the subscriber, establishing a connection and subscribing to the Redis pattern.
     */
    public void start() {
        pubSubConnection = connectionPool.getPubSubConnection();
        pubSubConnection.addListener((RedisPubSubListener<String, String>)this);
        pubSubConnection.addListener((RedisConnectionStateListener)this);

        String subscribePattern = NodeMessageProtocol.getClusterSubscriptionPattern(clusterId, nodeId);
        pubSubConnection.sync().psubscribe(subscribePattern);
        logger.info("NodeMessageSubscriber initialized and subscribed to pattern: {}", subscribePattern);
    }

    @Override
    public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
        if (pubSubConnection != null && pubSubConnection.isOpen()) {
            try {
                String subscribePattern = NodeMessageProtocol.getClusterSubscriptionPattern(clusterId, nodeId);
                pubSubConnection.async().psubscribe(subscribePattern).whenComplete((res, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to re-subscribe pattern after reconnection for node '{}'", nodeId, ex);
                    } else {
                        logger.info("NodeMessageSubscriber re-subscribed to pattern after reconnection: {}", subscribePattern);
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to trigger re-subscription after reconnection for node '{}'", nodeId, e);
            }
        }
    }

    @Override
    public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
        logger.warn("NodeMessageSubscriber disconnected from Redis for node '{}'", nodeId);
    }

    @Override
    public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, @NonNull Throwable cause) {
        logger.warn("NodeMessageSubscriber caught Redis exception for node '{}': {}", nodeId, cause.getMessage());
    }

    /**
     * Stops the subscriber, removing listeners and closing the connection.
     */
    public void stop() {
        if (pubSubConnection != null) {
            try {
                pubSubConnection.removeListener((RedisPubSubListener<String, String>)this);
                pubSubConnection.removeListener((RedisConnectionStateListener)this);
                pubSubConnection.close();
            } catch (Exception e) {
                logger.warn("Error closing pub/sub connection for node '{}'", nodeId, e);
            } finally {
                pubSubConnection = null;
            }
        }
    }

}

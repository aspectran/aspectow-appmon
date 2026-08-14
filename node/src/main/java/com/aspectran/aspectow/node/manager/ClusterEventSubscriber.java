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
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Specifically listens to the cluster-wide event channel and notifies
 * registered listeners about node join and leave events.
 *
 * <p>Created: 2026-05-24</p>
 */
public class ClusterEventSubscriber extends RedisPubSubAdapter<String, String> implements RedisConnectionStateListener {

    private static final Logger logger = LoggerFactory.getLogger(ClusterEventSubscriber.class);

    public static final String MESSAGE_JOINED = "JOINED:";

    public static final String MESSAGE_LEFT = "LEFT:";

    public static final String MESSAGE_STATUS_CHANGED = "STATUS_CHANGED:";

    private final String clusterId;

    private final RedisConnectionPool connectionPool;

    private final Set<ClusterEventListener> listeners = new CopyOnWriteArraySet<>();

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    /**
     * Constructs a new ClusterEventSubscriber.
     * @param clusterId the cluster ID
     * @param connectionPool the Redis connection pool
     */
    public ClusterEventSubscriber(String clusterId, RedisConnectionPool connectionPool) {
        this.clusterId = clusterId;
        this.connectionPool = connectionPool;
    }

    /**
     * Adds a cluster event listener.
     * @param listener the listener to add
     */
    public void addListener(ClusterEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a cluster event listener.
     * @param listener the listener to remove
     */
    public void removeListener(ClusterEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void message(@NonNull String channel, @NonNull String message) {
        if (message.startsWith(MESSAGE_JOINED)) {
            String aponData = message.substring(7);
            try {
                NodeInfo info = new NodeInfo();
                info.readFrom(aponData);
                for (ClusterEventListener listener : listeners) {
                    listener.onNodeJoined(info);
                }
            } catch (IOException e) {
                logger.warn("Failed to parse JOINED event data", e);
            }
        } else if (message.startsWith(MESSAGE_LEFT)) {
            String leftNodeId = message.substring(5);
            for (ClusterEventListener listener : listeners) {
                listener.onNodeLeft(leftNodeId);
            }
        } else if (message.startsWith(MESSAGE_STATUS_CHANGED)) {
            String aponData = message.substring(15);
            try {
                NodeInfo info = new NodeInfo();
                info.readFrom(aponData);
                for (ClusterEventListener listener : listeners) {
                    listener.onNodeStatusChanged(info);
                }
            } catch (IOException e) {
                logger.warn("Failed to parse STATUS_CHANGED event data", e);
            }
        }
    }

    /**
     * Starts the subscriber, establishing a connection and subscribing to cluster events.
     */
    public void start() {
        pubSubConnection = connectionPool.getPubSubConnection();
        pubSubConnection.addListener((RedisPubSubListener<String, String>)this);
        pubSubConnection.addListener((RedisConnectionStateListener)this);

        String eventsChannel = NodeMessageProtocol.getClusterEventsChannel(clusterId);
        pubSubConnection.sync().subscribe(eventsChannel);
        logger.info("ClusterEventSubscriber initialized and subscribed to channel: {}", eventsChannel);
    }

    @Override
    public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
        if (pubSubConnection != null && pubSubConnection.isOpen()) {
            try {
                String eventsChannel = NodeMessageProtocol.getClusterEventsChannel(clusterId);
                pubSubConnection.async().subscribe(eventsChannel).whenComplete((res, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to re-subscribe channel after reconnection for cluster '{}'", clusterId, ex);
                    } else {
                        logger.info("ClusterEventSubscriber re-subscribed to channel after reconnection: {}", eventsChannel);
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to trigger re-subscription after reconnection for cluster '{}'", clusterId, e);
            }
        }
    }

    @Override
    public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
        logger.warn("ClusterEventSubscriber disconnected from Redis for cluster '{}'", clusterId);
    }

    @Override
    public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, @NonNull Throwable cause) {
        logger.warn("ClusterEventSubscriber caught Redis exception for cluster '{}': {}", clusterId, cause.getMessage());
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
                logger.warn("Error closing pub/sub connection for cluster '{}'", clusterId, e);
            } finally {
                pubSubConnection = null;
            }
        }
    }

}

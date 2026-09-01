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
import com.aspectran.utils.logging.LoggingGroupHelper;
import com.aspectran.utils.thread.CustomizableThreadFactory;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listens to the cluster-wide event channel and notifies
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

    private final ExecutorService executorService = Executors.newCachedThreadPool(
            new CustomizableThreadFactory("cluster-event-sub-")
    );

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private String defaultLoggingGroup;

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
     * Returns the default logging group name.
     * @return the default logging group name
     */
    public String getDefaultLoggingGroup() {
        return defaultLoggingGroup;
    }

    /**
     * Sets the default logging group name.
     * @param defaultLoggingGroup the default logging group name
     */
    public void setDefaultLoggingGroup(String defaultLoggingGroup) {
        this.defaultLoggingGroup = defaultLoggingGroup;
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
        if (!executorService.isShutdown()) {
            executorService.submit(() -> dispatchMessage(channel, message));
        }
    }

    private void dispatchMessage(@NonNull String channel, @NonNull String message) {
        setLoggingGroup();
        try {
            if (message.startsWith(MESSAGE_JOINED)) {
                String aponData = message.substring(7);
                try {
                    NodeInfo info = new NodeInfo();
                    info.readFrom(aponData);
                    for (ClusterEventListener listener : listeners) {
                        try {
                            listener.onNodeJoined(info);
                        } catch (Exception e) {
                            logger.error("Error processing node joined event for node '{}'", info.getId(), e);
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Failed to parse JOINED event data", e);
                }
            } else if (message.startsWith(MESSAGE_LEFT)) {
                String leftNodeId = message.substring(5);
                for (ClusterEventListener listener : listeners) {
                    try {
                        listener.onNodeLeft(leftNodeId);
                    } catch (Exception e) {
                        logger.error("Error processing node left event for node '{}'", leftNodeId, e);
                    }
                }
            } else if (message.startsWith(MESSAGE_STATUS_CHANGED)) {
                String aponData = message.substring(15);
                try {
                    NodeInfo info = new NodeInfo();
                    info.readFrom(aponData);
                    for (ClusterEventListener listener : listeners) {
                        try {
                            listener.onNodeStatusChanged(info);
                        } catch (Exception e) {
                            logger.error("Error processing node status changed event for node '{}'", info.getId(), e);
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Failed to parse STATUS_CHANGED event data", e);
                }
            }
        } finally {
            clearLoggingGroup();
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
        setLoggingGroup();
        try {
            logger.info("ClusterEventSubscriber initialized and subscribed to channel: {}", eventsChannel);
        } finally {
            clearLoggingGroup();
        }
    }

    @Override
    public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
        if (pubSubConnection != null && pubSubConnection.isOpen()) {
            try {
                String eventsChannel = NodeMessageProtocol.getClusterEventsChannel(clusterId);
                pubSubConnection.async().subscribe(eventsChannel).whenComplete((res, ex) -> {
                    setLoggingGroup();
                    try {
                        if (ex != null) {
                            logger.error("Failed to re-subscribe channel after reconnection for cluster '{}'", clusterId, ex);
                        } else {
                            logger.info("ClusterEventSubscriber re-subscribed to channel after reconnection: {}", eventsChannel);
                        }
                    } finally {
                        clearLoggingGroup();
                    }
                });
            } catch (Exception e) {
                setLoggingGroup();
                try {
                    logger.error("Failed to trigger re-subscription after reconnection for cluster '{}'", clusterId, e);
                } finally {
                    clearLoggingGroup();
                }
            }
        }
    }

    @Override
    public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
        setLoggingGroup();
        try {
            logger.warn("ClusterEventSubscriber disconnected from Redis for cluster '{}'", clusterId);
        } finally {
            clearLoggingGroup();
        }
    }

    @Override
    public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, @NonNull Throwable cause) {
        setLoggingGroup();
        try {
            logger.warn("ClusterEventSubscriber caught Redis exception for cluster '{}': {}", clusterId, cause.getMessage());
        } finally {
            clearLoggingGroup();
        }
    }

    /**
     * Stops the subscriber, removing listeners and closing the connection.
     */
    public void stop() {
        executorService.shutdown();
        if (pubSubConnection != null) {
            try {
                pubSubConnection.removeListener((RedisPubSubListener<String, String>)this);
                pubSubConnection.removeListener((RedisConnectionStateListener)this);
                pubSubConnection.close();
            } catch (Exception e) {
                setLoggingGroup();
                try {
                    logger.warn("Error closing pub/sub connection for cluster '{}'", clusterId, e);
                } finally {
                    clearLoggingGroup();
                }
            } finally {
                pubSubConnection = null;
            }
        }
    }

    private void setLoggingGroup() {
        if (defaultLoggingGroup != null) {
            LoggingGroupHelper.set(defaultLoggingGroup);
        }
    }

    private void clearLoggingGroup() {
        if (defaultLoggingGroup != null) {
            LoggingGroupHelper.clear();
        }
    }

}

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

import com.aspectran.aspectow.node.config.ClusterConfig;
import com.aspectran.aspectow.node.config.GroupInfo;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.apon.AponWriter;
import com.aspectran.utils.thread.CustomizableThreadFactory;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.aspectran.aspectow.node.manager.ClusterEventSubscriber.MESSAGE_JOINED;
import static com.aspectran.aspectow.node.manager.ClusterEventSubscriber.MESSAGE_LEFT;
import static com.aspectran.aspectow.node.manager.ClusterEventSubscriber.MESSAGE_STATUS_CHANGED;

/**
 * Responsible for reporting the current node's status to the cluster registry.
 * <p>It periodically updates the pulse to indicate that the node is active
 * and healthy.</p>
 */
public class NodeReporter {

    private static final Logger logger = LoggerFactory.getLogger(NodeReporter.class);

    private static final long DEFAULT_PULSE_INTERVAL = 10000L;

    private static final long DEFAULT_PULSE_TIMEOUT = 60000L;

    private final NodeManager nodeManager;

    private final NodeRegistry nodeRegistry;

    private final NodeMessagePublisher nodeMessagePublisher;

    private final NodePortProvider portProvider;

    private final ScheduledExecutorService scheduler;

    /**
     * Constructs a new NodeReporter.
     * @param nodeManager the node manager orchestrating this node
     * @param portProvider the provider for the active service port of the node
     */
    public NodeReporter(@NonNull NodeManager nodeManager, NodePortProvider portProvider) {
        this.nodeManager = nodeManager;
        this.nodeRegistry = nodeManager.getNodeRegistry();
        this.nodeMessagePublisher = nodeManager.getNodeMessagePublisher();
        this.portProvider = portProvider;

        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("node-reporter-");
        threadFactory.setDaemon(true);
        this.scheduler = new ScheduledThreadPoolExecutor(1, threadFactory);
    }

    /**
     * Starts the node reporter.
     * Registers the node, broadcasts the join event, and schedules periodic pulse updates.
     * @throws Exception if an error occurs during startup
     */
    public void start() throws Exception {
        NodeInfo nodeInfo = getNodeInfo();

        logger.info("Initializing NodeReporter for cluster: {}, node: {}", getClusterConfig().getId(), nodeInfo.getId());

        nodeInfo.setStatus("live");

        // 1. Register the node in Redis Hash
        registerNode();

        // 2. Broadcast join event
        broadcastJoin();

        // 3. Start periodic pulse update
        long interval = nodeInfo.getPulseInterval(getClusterConfig().getPulseInterval(DEFAULT_PULSE_INTERVAL));
        scheduler.scheduleAtFixedRate(this::sendPulse, 0, interval, TimeUnit.MILLISECONDS);

        // 4. Start periodic maintenance (zombie eviction & full sync compensation)
        if (getClusterConfig().isGatewayMode()) {
            scheduler.scheduleAtFixedRate(this::performMaintenance, interval * 10, interval * 10, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops the node reporter.
     * Broadcasts the leave event, unregisters the node, and shuts down the scheduler.
     */
    public void stop() {
        NodeInfo nodeInfo = getNodeInfo();

        logger.info("Stopping NodeReporter for node: {}", nodeInfo.getId());

        // 1. Immediately shutdown scheduler to cancel background pulse/maintenance tasks
        scheduler.shutdownNow();

        nodeInfo.setStatus("stopping");

        // 2. Perform leave broadcast and unregistration asynchronously with a strict timeout
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    broadcastLeave();
                } catch (Exception e) {
                    logger.warn("Failed to broadcast leave event during stop for node '{}'", nodeInfo.getId(), e);
                }
                try {
                    unregisterNode();
                } catch (Exception e) {
                    logger.warn("Failed to unregister node during stop for node '{}'", nodeInfo.getId(), e);
                }
            }).get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.warn("Timed out waiting for node '{}' to unregister from cluster during shutdown", nodeInfo.getId());
        } catch (Exception e) {
            logger.warn("Error during node unregistration for node '{}'", nodeInfo.getId(), e);
        }

        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                logger.debug("Scheduler for NodeReporter did not terminate within 1 second");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Updates the status of the node in the cluster registry and broadcasts the change.
     * @param status the new status (e.g. "live", "paused")
     */
    public void updateStatus(String status) {
        getNodeInfo().setStatus(status);
        try {
            registerNode();
            broadcastStatusChanged();
        } catch (Exception e) {
            logger.error("Failed to update and broadcast node status: {}", status, e);
        }
    }

    private void registerNode() throws IOException {
        NodeInfo nodeInfo = getNodeInfo();
        String clusterId = getClusterConfig().getId();
        String key = NodeMessageProtocol.getNodesHashKey(clusterId);

        // Extract and set active service port from NodePortProvider
        if (portProvider != null) {
            Integer port = portProvider.getActivePort();
            if (port != null) {
                nodeInfo.setPort(port);
            }
        }

        // Generate and set authentication token
        nodeInfo.setToken(nodeManager.generateToken());

        // Convert NodeInfo to APON string for storage
        String aponData = new AponWriter().nullWritable(false).write(nodeInfo).toString();

        if (logger.isDebugEnabled()) {
            logger.debug("Registering node '{}' in Redis hash '{}': {}", nodeInfo.getId(), key,
                    ToStringBuilder.toString(nodeInfo));
        }

        try (StatefulRedisConnection<String, String> connection = getConnectionPool().getConnection()) {
            RedisCommands<String, String> sync = connection.sync();
            sync.hset(key, nodeInfo.getId(), aponData);

            // Ensure group info is also registered in Redis
            GroupInfo groupInfo = getGroupInfo();
            if (groupInfo != null) {
                String groupsKey = NodeMessageProtocol.getGroupsHashKey(clusterId);
                Boolean groupExists = sync.hexists(groupsKey, groupInfo.getId());
                if (Boolean.FALSE.equals(groupExists)) {
                    sync.hset(groupsKey, groupInfo.getId(), groupInfo.toString());
                    logger.info("Registered group info to Redis hash '{}': (Group: {})", groupsKey, groupInfo.getId());
                }
            }

            nodeRegistry.notifyNodeRegistered(nodeInfo);
        } catch (Exception e) {
            logger.error("Failed to register node '{}' in Redis registry", nodeInfo.getId(), e);
        }
    }

    private void broadcastJoin() {
        NodeInfo nodeInfo = getNodeInfo();
        try {
            nodeInfo.setToken(nodeManager.generateToken());
            String aponData = new AponWriter().nullWritable(false).write(nodeInfo).toString();
            String channel = NodeMessageProtocol.getClusterEventsChannel(getClusterConfig().getId());
            nodeMessagePublisher.asyncPublish(channel, MESSAGE_JOINED + aponData);
        } catch (Exception e) {
            logger.error("Failed to broadcast join event for node '{}'", nodeInfo.getId(), e);
        }
    }

    private void broadcastStatusChanged() {
        NodeInfo nodeInfo = getNodeInfo();
        try {
            nodeInfo.setToken(nodeManager.generateToken());
            String aponData = new AponWriter().nullWritable(false).write(nodeInfo).toString();
            String channel = NodeMessageProtocol.getClusterEventsChannel(getClusterConfig().getId());
            nodeMessagePublisher.asyncPublish(channel, MESSAGE_STATUS_CHANGED + aponData);
        } catch (Exception e) {
            logger.error("Failed to broadcast statusChanged event for node '{}'", nodeInfo.getId(), e);
        }
    }

    private void broadcastLeave() {
        NodeInfo nodeInfo = getNodeInfo();
        try {
            String channel = NodeMessageProtocol.getClusterEventsChannel(getClusterConfig().getId());
            nodeMessagePublisher.asyncPublish(channel, MESSAGE_LEFT + nodeInfo.getId());
        } catch (Exception e) {
            logger.error("Failed to broadcast leave event for node '{}'", nodeInfo.getId(), e);
        }
    }

    private void sendPulse() {
        NodeInfo nodeInfo = getNodeInfo();
        String clusterId = getClusterConfig().getId();
        String pulsesKey = NodeMessageProtocol.getPulsesHashKey(clusterId);
        String nodesKey = NodeMessageProtocol.getNodesHashKey(clusterId);
        long timestamp = System.currentTimeMillis();

        if (logger.isTraceEnabled()) {
            logger.trace("Sending pulse for node '{}' to '{}': {}", nodeInfo.getId(), pulsesKey, timestamp);
        }

        try (StatefulRedisConnection<String, String> connection = getConnectionPool().getConnection()) {
            RedisCommands<String, String> sync = connection.sync();
            Boolean nodeExists = sync.hexists(nodesKey, nodeInfo.getId());
            if (Boolean.FALSE.equals(nodeExists)) {
                logger.warn("Node '{}' missing from Redis registry hash '{}'. Re-registering...", nodeInfo.getId(), nodesKey);
                registerNode();
                broadcastJoin();
            }
            sync.hset(pulsesKey, nodeInfo.getId(), String.valueOf(timestamp));
        } catch (Exception e) {
            logger.error("Failed to send pulse for node '{}' to Redis registry", nodeInfo.getId(), e);
        }
    }

    private void performMaintenance() {
        // 1. Evict zombie nodes from global registry
        NodeInfo nodeInfo = getNodeInfo();
        long pulseInterval = nodeInfo.getPulseInterval(getClusterConfig().getPulseInterval(DEFAULT_PULSE_INTERVAL));
        long defaultTimeout = Math.max(pulseInterval * 6, DEFAULT_PULSE_TIMEOUT);
        long timeout = getClusterConfig().getPulseTimeout(defaultTimeout);
        nodeRegistry.evictZombieNodes(timeout);

        // 2. Compensate for missed Pub/Sub events by syncing local cache with global registry
        nodeManager.syncNodes();
    }

    private void unregisterNode() {
        NodeInfo nodeInfo = getNodeInfo();
        if (logger.isDebugEnabled()) {
            logger.debug("Unregistering node '{}' from cluster '{}'", nodeInfo.getId(), getClusterConfig().getId());
        }
        try {
            nodeRegistry.removeNode(nodeInfo.getId());
        } catch (Exception e) {
            logger.error("Failed to unregister node '{}' from Redis registry", nodeInfo.getId(), e);
        }
    }

    private ClusterConfig getClusterConfig() {
        return nodeManager.getClusterConfig();
    }

    private NodeInfo getNodeInfo() {
        return nodeManager.getNodeInfoHolder().getNodeInfo(nodeManager.getNodeId());
    }

    private GroupInfo getGroupInfo() {
        return nodeManager.getGroupInfoHolder().getGroupInfo(nodeManager.getGroupId());
    }

    private RedisConnectionPool getConnectionPool() {
        return nodeManager.getRedisConnectionPool();
    }

}

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
package com.aspectran.aspectow.node.manager;

import com.aspectran.aspectow.node.config.ClusterConfig;
import com.aspectran.aspectow.node.config.GroupInfo;
import com.aspectran.aspectow.node.config.GroupInfoHolder;
import com.aspectran.aspectow.node.config.NodeConfig;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.config.NodeInfoHolder;
import com.aspectran.aspectow.node.config.SchedulerConfig;
import com.aspectran.aspectow.node.config.SecretConfig;
import com.aspectran.aspectow.node.management.commands.RemoteCommandManager;
import com.aspectran.aspectow.node.management.nodes.RemoteNodeManager;
import com.aspectran.aspectow.node.management.scheduler.RemoteSchedulerManager;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.aspectow.node.redis.RedisConnectionPoolConfig;
import com.aspectran.aspectow.node.redis.RedisScheduledJobLockProvider;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.ScheduleRule;
import com.aspectran.core.context.rule.type.TriggerType;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.core.service.ServiceHoldingListener;
import com.aspectran.utils.Assert;
import com.aspectran.utils.PBEncryptionUtils;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.SystemUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * A builder for creating and configuring the {@link NodeManager} instance,
 * handling the orchestration of node-specific and cluster-wide settings.
 */
public abstract class NodeManagerBuilder {

    private static final Logger logger = LoggerFactory.getLogger(NodeManagerBuilder.class);

    public static final String MY_NODE_ID_PROPERTY = "aspectow.node.id";

    public static final String MY_GROUP_ID_PROPERTY = "aspectow.node.group";

    public static final String MY_CONSOLE_PROPERTY = "aspectow.node.console";

    private static final String DEFAULT_CLUSTER_ID = "cluster1";

    private static final String DEFAULT_NODE_ID = "node1";

    private static final String DEFAULT_GROUP_ID = "group1";

    private static final String DEFAULT_GROUP_TITLE = "Group 1";

    /**
     * Builds and initializes a new {@link NodeManager} instance.
     * @param context the activity context
     * @param nodeConfig the node configuration
     * @param redisConnectionPoolConfig the Redis connection pool configuration
     * @return the configured and built NodeManager instance
     * @throws Exception if an error occurs during initialization
     */
    @NonNull
    public static NodeManager build(
            ActivityContext context, NodeConfig nodeConfig,
            RedisConnectionPoolConfig redisConnectionPoolConfig) throws Exception {
        return build(context, nodeConfig, redisConnectionPoolConfig, null);
    }

    /**
     * Builds and initializes a new {@link NodeManager} instance with an optional logging group.
     * @param context the activity context
     * @param nodeConfig the node configuration
     * @param redisConnectionPoolConfig the Redis connection pool configuration
     * @param loggingGroup the explicit logging group name, or null to use context name
     * @return the configured and built NodeManager instance
     * @throws Exception if an error occurs during initialization
     */
    @NonNull
    public static NodeManager build(
            ActivityContext context, NodeConfig nodeConfig,
            RedisConnectionPoolConfig redisConnectionPoolConfig,
            String loggingGroup) throws Exception {
        Assert.notNull(context, "context must not be null");
        Assert.notNull(nodeConfig, "nodeConfig must not be null");

        ClusterConfig clusterConfig = nodeConfig.touchClusterConfig();
        String clusterId = clusterConfig.getId();
        if (!StringUtils.hasText(clusterId)) {
            clusterConfig.setId(DEFAULT_CLUSTER_ID);
            clusterId = DEFAULT_CLUSTER_ID;
        }
        if (clusterConfig.isDirectMode() && !"direct".equals(clusterConfig.getMode())) {
            clusterConfig.setMode("direct");
        }
        if (!clusterConfig.isDirectMode()) {
            validateSecretConfig(clusterConfig.getSecretConfig());
        }

        // Forcefully set the base path for cluster endpoint
        clusterConfig.touchEndpointConfig().setPath(NodeMessageProtocol.NODES_BASE_PATH);

        String myNodeId = resolveMyNodeId();
        String myGroupId = resolveMyGroupId();

        // 1. Resolve Group ID
        GroupInfoHolder groupInfoHolder = new GroupInfoHolder(nodeConfig.getGroupInfoList());
        GroupInfo myGroupInfo = null;
        if (StringUtils.hasText(myGroupId)) {
            myGroupInfo = groupInfoHolder.getGroupInfo(myGroupId);
        }
        if (myGroupInfo == null) {
            if (groupInfoHolder.hasGroupInfo()) {
                // If no group specified via env, use the first one from config
                myGroupInfo = groupInfoHolder.getGroupInfos().iterator().next();
                myGroupId = myGroupInfo.getId();
            } else {
                // Last fallback
                myGroupId = StringUtils.hasText(myGroupId) ? myGroupId : DEFAULT_GROUP_ID;
                myGroupInfo = new GroupInfo();
                myGroupInfo.setId(myGroupId);
                myGroupInfo.setTitle(DEFAULT_GROUP_TITLE);
                groupInfoHolder.putGroupInfo(myGroupInfo);
            }
        }

        // 2. Resolve Node ID
        String nodeId;
        NodeInfo nodeInfo;
        NodeInfoHolder nodeInfoHolder = new NodeInfoHolder(nodeConfig.getNodeInfoList());
        if (StringUtils.hasText(myNodeId)) {
            nodeInfo = nodeInfoHolder.getNodeInfo(myNodeId);
        } else {
            nodeInfo = null;
        }

        if (nodeInfo == null) {
            if (clusterConfig.isGatewayMode() && !StringUtils.hasText(myNodeId)) {
                // Gateway mode + No explicit ID -> Always Dynamic
                String shortId = UUID.randomUUID().toString().split("-")[0];
                nodeId = shortId + (StringUtils.hasText(myGroupId) ? "@" + myGroupId : "");
            } else {
                // Direct mode or Explicit ID (even if not in config)
                nodeId = StringUtils.hasText(myNodeId) ? myNodeId : DEFAULT_NODE_ID;
            }
            nodeInfo = new NodeInfo();
            nodeInfo.setId(nodeId);
            nodeInfo.setGroup(myGroupId);
            nodeInfoHolder.putNodeInfo(nodeInfo);
        } else {
            nodeId = nodeInfo.getId();
        }

        Boolean myConsole = resolveMyConsole();
        if (myConsole != null) {
            nodeInfo.setConsole(myConsole);
        }

        // Ensure all nodes have a group assigned
        for (NodeInfo info : nodeInfoHolder.getNodeInfoList()) {
            if (!StringUtils.hasText(info.getGroup())) {
                info.setGroup(myGroupId);
            }
        }

        // Forcefully set the base path for node endpoint
        for (NodeInfo info : nodeInfoHolder.getNodeInfoList()) {
            info.touchEndpointConfig().setPath(NodeMessageProtocol.NODES_BASE_PATH);
        }

        // Auto-detect host if not specified
        if (!StringUtils.hasText(nodeInfo.getHost())) {
            String host = SystemUtils.getHostName();
            if ("localhost".equals(host)) {
                host = SystemUtils.getLocalIP();
            }
            nodeInfo.setHost(host);
        }

        logger.info("Current Node: {} (Host: {})", nodeId, nodeInfo.getHost());

        boolean hasNodeManager = context.getBeanRegistry().containsBean(RemoteNodeManager.class);
        boolean hasSchedulerManager = context.getBeanRegistry().containsBean(RemoteSchedulerManager.class);
        boolean hasCommandManager = context.getBeanRegistry().containsBean(RemoteCommandManager.class);

        nodeInfo.setHasNodeManager(hasNodeManager);
        nodeInfo.setHasSchedulerManager(hasSchedulerManager);
        nodeInfo.setHasCommandManager(hasCommandManager);

        NodeManager nodeManager = new NodeManager(nodeId, myGroupId, clusterConfig, nodeInfoHolder, groupInfoHolder);

        if (clusterConfig.isGatewayMode()) {
            if (redisConnectionPoolConfig == null) {
                throw new IllegalStateException("RedisConnectionPoolConfig is required for gateway mode");
            }
            RedisConnectionPool connectionPool = new RedisConnectionPool(redisConnectionPoolConfig);
            connectionPool.initialize();
            nodeManager.setRedisConnectionPool(connectionPool);

            NodePortProvider portProvider = null;
            if (context.getBeanRegistry().containsBean(NodePortProvider.class)) {
                portProvider = context.getBeanRegistry().getBean(NodePortProvider.class);
            }

            NodeRegistry nodeRegistry = new NodeRegistry(clusterId, connectionPool);
            NodeMessagePublisher nodeMessagePublisher = new NodeMessagePublisher(clusterId, nodeId, connectionPool);

            String defaultLoggingGroup = StringUtils.hasText(loggingGroup) ? loggingGroup : context.getName();
            NodeMessageSubscriber nodeMessageSubscriber = new NodeMessageSubscriber(clusterId, nodeId, connectionPool);
            nodeMessageSubscriber.setDefaultLoggingGroup(defaultLoggingGroup);
            ClusterEventSubscriber clusterEventSubscriber = new ClusterEventSubscriber(clusterId, connectionPool);
            clusterEventSubscriber.setDefaultLoggingGroup(defaultLoggingGroup);

            nodeManager.setLoggingGroup(defaultLoggingGroup);
            nodeManager.setNodeRegistry(nodeRegistry);
            nodeManager.setNodeMessagePublisher(nodeMessagePublisher);
            nodeManager.setNodeMessageSubscriber(nodeMessageSubscriber);
            nodeManager.setClusterEventSubscriber(clusterEventSubscriber);

            NodeReporter nodeReporter = new NodeReporter(nodeManager, portProvider);
            nodeManager.setNodeReporter(nodeReporter);

            for (NodeInfo info : nodeRegistry.getNodes()) {
                if (!nodeId.equals(info.getId())) {
                    NodeInfo existingInfo = nodeManager.getNodeInfoHolder().getNodeInfo(info.getId());
                    if (existingInfo != null) {
                        // Partial update: preserve static config from node-config.apon
                        // Create a new NodeInfo instance to ensure atomic update for potential concurrent readers
                        NodeInfo newInfo = existingInfo.copyWithUpdatedState(info);
                        nodeManager.getNodeInfoHolder().putNodeInfo(newInfo);
                    } else {
                        // Full update for dynamic join
                        nodeManager.getNodeInfoHolder().putNodeInfo(info);
                    }
                }
            }
            // Initialize nodes not found in registry as offline
            for (NodeInfo info : nodeManager.getNodeInfoList()) {
                if (info.getStatus() == null) {
                    info.setStatus("offline");
                }
            }

            RedisScheduledJobLockProvider jobLockProvider = new RedisScheduledJobLockProvider(connectionPool, clusterId);
            SchedulerConfig schedulerConfig = clusterConfig.getSchedulerConfig();
            if (schedulerConfig != null) {
                if (schedulerConfig.hasLockTimeout()) {
                    jobLockProvider.setLockTimeoutSeconds(schedulerConfig.getLockTimeout());
                }
                if (schedulerConfig.hasReleasedOnUnlock()) {
                    jobLockProvider.setReleasedOnUnlock(schedulerConfig.isReleasedOnUnlock());
                }
            }
            CoreServiceHolder.setJobLockProvider(jobLockProvider);
            logger.info("Registered RedisScheduledJobLockProvider for cluster-wide job locking");

            isolateSimpleTriggers(context);
            for (CoreService service : CoreServiceHolder.getAllServices()) {
                isolateSimpleTriggers(service.getActivityContext());
            }
            CoreServiceHolder.addServiceHoldingListener(new ServiceHoldingListener() {
                @Override
                public void afterServiceHolding(CoreService service) {
                    isolateSimpleTriggers(service.getActivityContext());
                }
            });
        }
        return nodeManager;
    }

    private static void isolateSimpleTriggers(ActivityContext context) {
        if (context != null && context.getScheduleRuleRegistry() != null) {
            for (ScheduleRule scheduleRule : context.getScheduleRuleRegistry().getScheduleRules()) {
                if (scheduleRule.getTriggerType() == TriggerType.SIMPLE && !scheduleRule.isIsolated()) {
                    logger.warn("Schedule '{}' in context '{}' is configured with a SIMPLE trigger but is not isolated. " +
                            "Simple triggers cannot be locked reliably across multiple nodes due to potential time drift. " +
                            "It will be forced to execute in isolated mode.",
                            scheduleRule.getId(), (context.getName() != null ? context.getName() : "root"));
                    scheduleRule.setIsolated(true);
                }
            }
        }
    }

    private static String resolveMyNodeId() {
        return SystemUtils.getProperty(MY_NODE_ID_PROPERTY);
    }

    private static String resolveMyGroupId() {
        return SystemUtils.getProperty(MY_GROUP_ID_PROPERTY);
    }

    @Nullable
    private static Boolean resolveMyConsole() {
        String val = SystemUtils.getProperty(MY_CONSOLE_PROPERTY);
        if (StringUtils.hasText(val)) {
            return Boolean.parseBoolean(val);
        }
        return null;
    }

    private static void validateSecretConfig(SecretConfig secretConfig) {
        String password = (secretConfig != null ? secretConfig.getPassword() : null);
        if (password == null) {
            password = PBEncryptionUtils.getPassword();
        }
        if (password == null) {
            throw new IllegalStateException("Encryption password is required for gateway or autoscaling mode; " +
                    "Please set it in node-config.apon or via the 'aspectran.encryption.password' system property");
        }

        String algorithm = (secretConfig != null ? secretConfig.getAlgorithm() : null);
        String salt = (secretConfig != null ? secretConfig.getSalt() : null);
        if (algorithm == null) {
            algorithm = PBEncryptionUtils.getAlgorithm();
        }
        if (salt == null) {
            salt = PBEncryptionUtils.getSalt();
        }
        PBEncryptionUtils.validate(algorithm, password, salt);
    }

}

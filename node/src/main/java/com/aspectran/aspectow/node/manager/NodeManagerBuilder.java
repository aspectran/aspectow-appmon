/*
 * Copyright (c) 2020-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the \"License\");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aspectran.aspectow.node.manager;

import com.aspectran.aspectow.node.config.ClusterConfig;
import com.aspectran.aspectow.node.config.NodeConfig;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.config.NodeInfoHolder;
import com.aspectran.aspectow.node.config.SchedulerConfig;
import com.aspectran.aspectow.node.config.SecretConfig;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.aspectow.node.redis.RedisConnectionPoolConfig;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import com.aspectran.aspectow.node.redis.RedisMessageSubscriber;
import com.aspectran.aspectow.node.redis.RedisScheduledJobLockProvider;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.utils.Assert;
import com.aspectran.utils.PBEncryptionUtils;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.SystemUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * A builder for creating and configuring the {@link NodeManager} instance,
 * handling the orchestration of node-specific and cluster-wide settings.
 */
public abstract class NodeManagerBuilder {

    private static final Logger logger = LoggerFactory.getLogger(NodeManagerBuilder.class);

    private static final String MY_NODE_ID_PROPERTY_NAME = "aspectow.node.id";

    private static final String DEFAULT_CLUSTER_ID = "cluster";

    private static final String DEFAULT_NODE_ID = "node";

    @NonNull
    public static NodeManager build(ActivityContext context, NodeConfig nodeConfig, RedisConnectionPoolConfig redisConnectionPoolConfig) throws Exception {
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

        String nodeId;
        NodeInfo nodeInfo;
        NodeInfoHolder nodeInfoHolder;
        if (clusterConfig.isAutoscalingMode()) {
            nodeId = UUID.randomUUID().toString();
            nodeInfo = new NodeInfo();
            nodeInfo.setNodeId(nodeId);
            nodeInfoHolder = new NodeInfoHolder();
            nodeInfoHolder.putNodeInfo(nodeInfo);
        } else {
            String myNodeId = resolveMyNodeId();
            nodeInfoHolder = new NodeInfoHolder(nodeConfig.getNodeInfoList());
            nodeInfo = nodeInfoHolder.getNodeInfo(myNodeId);
            if (nodeInfo == null) {
                List<NodeInfo> nodeInfoList = nodeConfig.getNodeInfoList();
                if (DEFAULT_NODE_ID.equals(myNodeId) && nodeInfoList != null && nodeInfoList.size() == 1) {
                    nodeInfo = nodeInfoList.getFirst();
                    nodeId = nodeInfo.getNodeId();
                } else {
                    if (clusterConfig.isGatewayMode()) {
                        throw new IllegalStateException("Node information for '" + myNodeId + "' is not defined in " +
                                "the configuration file, which is required in gateway mode.");
                    }
                    nodeId = myNodeId;
                    nodeInfo = new NodeInfo();
                    nodeInfo.setNodeId(nodeId);
                    nodeInfoHolder.putNodeInfo(nodeInfo);
                }
            } else {
                nodeId = myNodeId;
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

        NodeManager nodeManager = new NodeManager(nodeId, clusterConfig, nodeInfoHolder);

        if (!clusterConfig.isDirectMode()) {
            if (redisConnectionPoolConfig == null) {
                throw new IllegalStateException("RedisConnectionPoolConfig is required for cluster mode");
            }
            RedisConnectionPool connectionPool = new RedisConnectionPool(redisConnectionPoolConfig);
            connectionPool.initialize();
            nodeManager.setRedisConnectionPool(connectionPool);

            NodePortProvider portProvider = null;
            if (context.getBeanRegistry().containsBean(NodePortProvider.class)) {
                portProvider = context.getBeanRegistry().getBean(NodePortProvider.class);
            }

            NodeRegistry nodeRegistry = new NodeRegistry(clusterId, connectionPool);
            NodeReporter nodeReporter = new NodeReporter(clusterConfig, nodeInfo, connectionPool, portProvider);
            RedisMessagePublisher redisMessagePublisher = new RedisMessagePublisher(clusterId, nodeId, connectionPool);
            RedisMessageSubscriber redisMessageSubscriber = new RedisMessageSubscriber(clusterId, nodeId, connectionPool);
            if (clusterConfig.isGatewayMode()) {
                redisMessageSubscriber.setSubscribePattern(NodeMessageProtocol.getClusterSubscriptionPattern(clusterId));
            }

            nodeManager.setNodeRegistry(nodeRegistry);
            nodeManager.setNodeReporter(nodeReporter);
            nodeManager.setRedisMessagePublisher(redisMessagePublisher);
            nodeManager.setRedisMessageSubscriber(redisMessageSubscriber);

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
        }
        return nodeManager;
    }

    private static String resolveMyNodeId() {
        return SystemUtils.getProperty(MY_NODE_ID_PROPERTY_NAME, DEFAULT_NODE_ID);
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

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
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.config.NodeInfoHolder;
import com.aspectran.aspectow.node.config.SecretConfig;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import com.aspectran.aspectow.node.redis.RedisMessageSubscriber;
import com.aspectran.utils.PBEncryptionUtils;
import com.aspectran.utils.apon.VariableParameters;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.utils.security.TimeLimitedPBTokenIssuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * The core orchestrator for the Aspectow Node Manager.
 * <p>This class centralizes the management of cluster node status reporting,
 * inter-node communication relay, and secure cluster coordination using
 * Redis and PBE-based tokens.</p>
 */
public class NodeManager {

    private static final Logger logger = LoggerFactory.getLogger(NodeManager.class);

    private final String nodeId;

    private final ClusterConfig clusterConfig;

    private final NodeInfoHolder nodeInfoHolder;

    private RedisConnectionPool redisConnectionPool;

    private NodeRegistry nodeRegistry;

    private NodeReporter nodeReporter;

    private RedisMessagePublisher redisMessagePublisher;

    private RedisMessageSubscriber redisMessageSubscriber;

    /**
     * Instantiates a new NodeManager.
     * @param nodeId the unique identifier of the current node
     * @param clusterConfig the cluster-wide configuration
     * @param nodeInfoHolder the holder for node-specific information
     */
    public NodeManager(String nodeId, ClusterConfig clusterConfig, NodeInfoHolder nodeInfoHolder) {
        this.nodeId = nodeId;
        this.clusterConfig = clusterConfig;
        this.nodeInfoHolder = nodeInfoHolder;
    }

    /**
     * Returns the unique identifier of the current node.
     * @return the node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Returns the cluster configuration.
     * @return the cluster configuration
     */
    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    /**
     * Returns the holder for node information.
     * @return the node info holder
     */
    public NodeInfoHolder getNodeInfoHolder() {
        return nodeInfoHolder;
    }

    /**
     * Returns a list of information for all registered nodes in the cluster.
     * @return the list of node information
     */
    public List<NodeInfo> getNodeInfoList() {
        return nodeInfoHolder.getNodeInfoList();
    }

    /**
     * Returns the Redis connection pool.
     * @return the Redis connection pool
     */
    public RedisConnectionPool getRedisConnectionPool() {
        return redisConnectionPool;
    }

    /**
     * Sets the Redis connection pool.
     * @param redisConnectionPool the Redis connection pool
     */
    public void setRedisConnectionPool(RedisConnectionPool redisConnectionPool) {
        this.redisConnectionPool = redisConnectionPool;
    }

    /**
     * Returns the node registry used for tracking active nodes.
     * @return the node registry
     */
    public NodeRegistry getNodeRegistry() {
        return nodeRegistry;
    }

    /**
     * Sets the node registry.
     * @param nodeRegistry the node registry
     */
    public void setNodeRegistry(NodeRegistry nodeRegistry) {
        this.nodeRegistry = nodeRegistry;
    }

    /**
     * Returns the node reporter responsible for status broadcasts.
     * @return the node reporter
     */
    public NodeReporter getNodeReporter() {
        return nodeReporter;
    }

    /**
     * Sets the node reporter.
     * @param nodeReporter the node reporter
     */
    public void setNodeReporter(NodeReporter nodeReporter) {
        this.nodeReporter = nodeReporter;
    }

    /**
     * Returns the publisher for sending relay messages via Redis.
     * @return the Redis message publisher
     */
    public RedisMessagePublisher getRedisMessagePublisher() {
        return redisMessagePublisher;
    }

    /**
     * Sets the Redis message publisher.
     * @param redisMessagePublisher the Redis message publisher
     */
    public void setRedisMessagePublisher(RedisMessagePublisher redisMessagePublisher) {
        this.redisMessagePublisher = redisMessagePublisher;
    }

    /**
     * Returns the subscriber for receiving relay messages via Redis.
     * @return the Redis message subscriber
     */
    public RedisMessageSubscriber getRedisMessageSubscriber() {
        return redisMessageSubscriber;
    }

    /**
     * Sets the Redis message subscriber.
     * @param redisMessageSubscriber the Redis message subscriber
     */
    public void setRedisMessageSubscriber(RedisMessageSubscriber redisMessageSubscriber) {
        this.redisMessageSubscriber = redisMessageSubscriber;
    }

    /**
     * Gracefully shuts down all managed components and releases resources.
     */
    public void destroy() {
        if (nodeReporter != null) {
            nodeReporter.stop();
        }
        if (redisMessageSubscriber != null) {
            redisMessageSubscriber.stop();
        }
        if (redisConnectionPool != null) {
            logger.info("Closing Redis connection pool for node: {}", nodeId);
            redisConnectionPool.destroy();
        }
    }

    /**
     * Generates a time-limited, encrypted authentication token for this node.
     * <p>The token contains identity information and is secured using the cluster's
     * shared secret and Password-Based Encryption (PBE).</p>
     * @return an encrypted token string
     */
    public String generateToken() {
        SecretConfig secretConfig = clusterConfig.getSecretConfig();
        String password = (secretConfig != null ? secretConfig.getPassword() : null);
        if (password == null) {
            password = PBEncryptionUtils.getPassword();
        }
        if (password == null) {
            throw new IllegalStateException("Encryption password not found for token generation");
        }
        String salt = (secretConfig != null ? secretConfig.getSalt() : PBEncryptionUtils.getSalt());

        VariableParameters payload = new VariableParameters();
        payload.putValue("nodeId", nodeId);
        payload.putValue("clusterId", clusterConfig.getId());

        // Default 30 seconds expiration
        return TimeLimitedPBTokenIssuer.createToken(payload, 30000L, password, salt);
    }

    /**
     * Validates the provided authentication token.
     * @param token the token string to validate
     * @throws InvalidPBTokenException if the token is invalid or expired
     */
    public void validateToken(String token) throws InvalidPBTokenException {
        SecretConfig secretConfig = clusterConfig.getSecretConfig();
        String password = (secretConfig != null ? secretConfig.getPassword() : null);
        if (password == null) {
            password = PBEncryptionUtils.getPassword();
        }
        if (password == null) {
            throw new InvalidPBTokenException(token, "Encryption password not found for token validation");
        }
        String salt = (secretConfig != null ? secretConfig.getSalt() : PBEncryptionUtils.getSalt());

        TimeLimitedPBTokenIssuer.validate(token, password, salt);
    }

}

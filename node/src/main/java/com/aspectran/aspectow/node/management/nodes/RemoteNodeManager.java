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
package com.aspectran.aspectow.node.management.nodes;

import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.management.nodes.bridge.NodeBridge;
import com.aspectran.aspectow.node.management.nodes.bridge.NodeBroker;
import com.aspectran.aspectow.node.management.nodes.bridge.NodeSession;
import com.aspectran.aspectow.node.manager.ClusterEventListener;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeMessagePublisher;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.utils.Assert;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RemoteNodeManager orchestrates node management and event propagation across the cluster.
 * It manages local node status sessions and broadcasts join/left events to connected clients.
 */
public class RemoteNodeManager implements InitializableBean, DisposableBean, ClusterEventListener {

    private static final Logger logger = LoggerFactory.getLogger(RemoteNodeManager.class);

    private final Map<String, NodeBridge> sessionBridgeMap = new ConcurrentHashMap<>();

    private final NodeManager nodeManager;

    private final NodeMessagePublisher messagePublisher;

    private final NodeBroker broker;

    /**
     * Instantiates a new RemoteNodeManager.
     * @param nodeManager the node manager
     */
    public RemoteNodeManager(@NonNull NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.messagePublisher = nodeManager.getNodeMessagePublisher();
        this.broker = new NodeBroker();
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing RemoteNodeManager for node: {}", getNodeId());

        // Register as a cluster event listener to handle node join/left events
        if (nodeManager.getClusterEventSubscriber() != null) {
            nodeManager.getClusterEventSubscriber().addListener(this);
        }
    }

    @Override
    public void destroy() {
        if (nodeManager.getClusterEventSubscriber() != null) {
            nodeManager.getClusterEventSubscriber().removeListener(this);
        }
    }

    /**
     * Returns the message publisher for node communications.
     * @return the message publisher
     */
    public NodeMessagePublisher getMessagePublisher() {
        return messagePublisher;
    }

    /**
     * Returns whether the manager is running in gateway mode (i.e. has a message publisher).
     * @return true if gateway mode, false otherwise
     */
    public boolean isGatewayMode() {
        return (messagePublisher != null);
    }

    /**
     * Returns the unique identifier of the local node.
     * @return the node ID
     */
    public String getNodeId() {
        return nodeManager.getNodeId();
    }

    /**
     * Returns whether the specified node ID is the local node.
     * @param targetNodeId the node ID to check
     * @return true if it is the local node, false otherwise
     */
    public boolean isSameNode(String targetNodeId) {
        return (targetNodeId != null && targetNodeId.equals(getNodeId()));
    }

    /**
     * Returns the node broker.
     * @return the node broker
     */
    public NodeBroker getBroker() {
        return broker;
    }

    /**
     * Registers a session with its corresponding node bridge.
     * @param sessionId the session identifier
     * @param nodeBridge the node bridge
     */
    public void registerSession(String sessionId, NodeBridge nodeBridge) {
        sessionBridgeMap.put(sessionId, nodeBridge);
    }

    /**
     * Unregisters a session.
     * @param sessionId the session identifier
     */
    public void unregisterSession(String sessionId) {
        sessionBridgeMap.remove(sessionId);
    }

    /**
     * Bridges a node message to a specific session.
     * @param sessionId the session identifier
     * @param message the message to bridge
     */
    public void bridge(String sessionId, String message) {
        Assert.notNull(sessionId, "sessionId must not be null");
        NodeBridge bridge = sessionBridgeMap.get(sessionId);
        if (bridge != null) {
            NodeSession session = bridge.findNodeSession(sessionId);
            if (session != null) {
                bridge.bridge(session, message);
            }
        }
    }

    @Override
    public void onNodeJoined(NodeInfo nodeInfo) {
        if (sessionBridgeMap.isEmpty()) {
            return;
        }
        try {
            NodeResponseParameters response = new NodeResponseParameters()
                    .setHeader("nodeJoined")
                    .setNode(nodeInfo);
            String message = response.toString();
            for (Map.Entry<String, NodeBridge> entry : sessionBridgeMap.entrySet()) {
                String sessionId = entry.getKey();
                NodeBridge bridge = entry.getValue();
                NodeSession session = bridge.findNodeSession(sessionId);
                if (session != null) {
                    bridge.bridge(session, message);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to broadcast nodeJoined event of node {}", nodeInfo.getId(), e);
        }
    }

    @Override
    public void onNodeLeft(String leftNodeId) {
        if (sessionBridgeMap.isEmpty()) {
            return;
        }
        try {
            NodeInfo nodeInfo = new NodeInfo();
            nodeInfo.setId(leftNodeId);
            nodeInfo.setStatus("offline");

            NodeResponseParameters response = new NodeResponseParameters()
                    .setHeader("nodeLeft")
                    .setNode(nodeInfo);
            String message = response.toString();
            for (Map.Entry<String, NodeBridge> entry : sessionBridgeMap.entrySet()) {
                String sessionId = entry.getKey();
                NodeBridge bridge = entry.getValue();
                NodeSession session = bridge.findNodeSession(sessionId);
                if (session != null) {
                    bridge.bridge(session, message);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to broadcast nodeLeft event of node {}", leftNodeId, e);
        }
    }

    @Override
    public void onNodeStatusChanged(NodeInfo nodeInfo) {
        if (sessionBridgeMap.isEmpty()) {
            return;
        }
        try {
            NodeResponseParameters response = new NodeResponseParameters()
                    .setHeader("nodeStatusChanged")
                    .setNode(nodeInfo);
            String message = response.toString();
            for (Map.Entry<String, NodeBridge> entry : sessionBridgeMap.entrySet()) {
                String sessionId = entry.getKey();
                NodeBridge bridge = entry.getValue();
                NodeSession session = bridge.findNodeSession(sessionId);
                if (session != null) {
                    bridge.bridge(session, message);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to broadcast nodeStatusChanged event of node {}", nodeInfo.getId(), e);
        }
    }

}

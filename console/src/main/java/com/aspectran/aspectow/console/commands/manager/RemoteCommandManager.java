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
package com.aspectran.aspectow.console.commands.manager;

import com.aspectran.aspectow.console.commands.bridge.CommandBroker;
import com.aspectran.aspectow.console.commands.bridge.redis.CommandMessageBridgeHandler;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RemoteCommandManager orchestrates remote daemon command execution across the cluster.
 * It manages local execution, remote dispatching via Redis, and broadcasting
 * results to connected clients.
 */
@Component
@Bean(id = "remoteCommandManager")
public class RemoteCommandManager implements InitializableBean {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCommandManager.class);

    private final NodeManager nodeManager;

    private final LocalCommandService localCommandService;

    private final CommandBroker broker;

    public RemoteCommandManager(@NonNull NodeManager nodeManager, LocalCommandService localCommandService) {
        this.nodeManager = nodeManager;
        this.localCommandService = localCommandService;
        this.broker = new CommandBroker(nodeManager.getNodeId(), nodeManager.getRedisMessagePublisher());
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing RemoteCommandManager for node: {}", nodeManager.getNodeId());

        // Register a listener for command relay messages (commands and results) from Redis
        if (nodeManager.getRedisMessageSubscriber() != null) {
            CommandMessageBridgeHandler bridgeHandler = new CommandMessageBridgeHandler(this);
            nodeManager.getRedisMessageSubscriber().addListener(bridgeHandler);
        }
    }

    public CommandBroker getBroker() {
        return broker;
    }

    /**
     * Dispatches a command request to a specific node or handles it locally.
     * @param targetNodeId the ID of the node to receive the request
     * @param commandData the command payload in APON/JSON format
     */
    public void dispatch(String targetNodeId, String commandData) {
        if (nodeManager.getNodeId().equals(targetNodeId)) {
            // Case 1: Target is local node, execute directly and broadcast result to local clients
            logger.debug("Executing local daemon command: {}", commandData);
            String response = localCommandService.execute(commandData);
            if (response != null) {
                broadcast(response);
            }
        } else {
            // Case 2: Target is a remote node, relay via Redis
            if (nodeManager.getRedisMessagePublisher() != null) {
                try {
                    // Command target is embedded in the message format if needed,
                    // but for commands we currently rely on the publishRelay mechanism
                    nodeManager.getRedisMessagePublisher().publishRelay(CommandBroker.CATEGORY_COMMANDS, commandData);
                    logger.debug("Daemon command dispatched to cluster (target={}): {}", targetNodeId, commandData);
                } catch (Exception e) {
                    logger.error("Failed to dispatch daemon command to cluster", e);
                }
            } else {
                logger.warn("Cannot dispatch command: Redis publisher not available");
            }
        }
    }

    /**
     * Processes an incoming message received from the cluster relay.
     * @param message the raw relay message
     */
    public void process(String message) {
        // Since categorization is handled by the BridgeHandler, 
        // we just need to distinguish between a command and a result.
        // For RemoteCommandManager, we assume if it's not a known result format, it's a command.
        // But for consistency with SchedulerManager, we can use a prefix or check the content.
        if (message.startsWith("command:")) {
            String commandData = message.substring(8);
            String response = localCommandService.execute(commandData);
            if (response != null && nodeManager.getRedisMessagePublisher() != null) {
                try {
                    nodeManager.getRedisMessagePublisher().publishRelay(CommandBroker.CATEGORY_COMMANDS, response);
                } catch (Exception e) {
                    logger.error("Failed to relay daemon command response to cluster", e);
                }
            }
        } else {
            broadcast(message);
        }
    }

    /**
     * Broadcasts a command execution result to all connected clients on this node.
     * @param response the result payload
     */
    public void broadcast(String response) {
        if (logger.isTraceEnabled()) {
            logger.trace("Broadcasting command result to local clients: {}", response);
        }
        if (broker != null) {
            broker.bridge(response);
        }
    }

}

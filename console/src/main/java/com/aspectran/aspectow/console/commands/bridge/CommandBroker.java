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
package com.aspectran.aspectow.console.commands.bridge;

import com.aspectran.aspectow.console.commands.manager.RemoteCommandManager;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * CommandBroker handles the distribution of command results
 * to connected clients (via WebSockets or Polling).
 */
public class CommandBroker {

    private static final Logger logger = LoggerFactory.getLogger(CommandBroker.class);

    public static final String CATEGORY_COMMANDS = "commands";

    public static final String CONTROL_JOIN = "commands:join";

    public static final String CONTROL_RELEASE = "commands:release";

    private final String nodeId;

    private final RedisMessagePublisher messagePublisher;

    private final RemoteCommandManager commandManager;

    private final Set<CommandBridge> bridges = new CopyOnWriteArraySet<>();

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    public CommandBroker(String nodeId, RedisMessagePublisher messagePublisher, RemoteCommandManager commandManager) {
        this.nodeId = nodeId;
        this.messagePublisher = messagePublisher;
        this.commandManager = commandManager;
    }

    public String nodeId() {
        return nodeId;
    }

    public RedisMessagePublisher getMessagePublisher() {
        return messagePublisher;
    }

    public SubscriptionRegistry getSubscriptionRegistry() {
        return subscriptionRegistry;
    }

    public void addBridge(CommandBridge bridge) {
        bridges.add(bridge);
    }

    public void removeBridge(CommandBridge bridge) {
        bridges.remove(bridge);
    }

    public synchronized void join(@NonNull CommandSession session) {
        if (session.isValid()) {
            boolean alreadyInUse = subscriptionRegistry.isInUse();
            subscriptionRegistry.addLocalSubscription(session.getId());
            if (!alreadyInUse) {
                commandManager.startExporters();
            }
            publishControl(CONTROL_JOIN);
        }
    }

    public synchronized void release(@NonNull CommandSession session) {
        subscriptionRegistry.removeLocalSubscription(session.getId());
        if (!subscriptionRegistry.isInUse()) {
            commandManager.stopExporters();
        }
        if (!subscriptionRegistry.isInUseLocally()) {
            publishControl(CONTROL_RELEASE);
        }
    }

    private void publishControl(String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishControl(message);
            } catch (Exception e) {
                logger.error("Failed to publish control message to Redis", e);
            }
        }
    }

    /**
     * Bridges a command result to all connected clients.
     * @param resultData the result payload to send
     */
    public void bridge(String resultData) {
        for (CommandBridge bridge : bridges) {
            try {
                bridge.bridge(resultData);
            } catch (Exception e) {
                logger.warn("Failed to bridge command result via {}: {}",
                        bridge.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Bridges a command result to a specific session.
     * @param session the target session
     * @param resultData the result payload to send
     */
    public void bridge(CommandSession session, String resultData) {
        for (CommandBridge bridge : bridges) {
            try {
                bridge.bridge(session, resultData);
            } catch (Exception e) {
                logger.warn("Failed to bridge command result via {}: {}",
                        bridge.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

}

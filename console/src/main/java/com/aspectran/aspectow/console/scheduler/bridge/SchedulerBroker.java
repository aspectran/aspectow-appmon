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
package com.aspectran.aspectow.console.scheduler.bridge;

import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * SchedulerBroker handles the distribution of scheduler management results
 * to connected clients.
 */
public class SchedulerBroker {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerBroker.class);

    private final String nodeId;

    private final RedisMessagePublisher messagePublisher;

    private final Set<SchedulerBridge> bridges = new CopyOnWriteArraySet<>();

    public SchedulerBroker(String nodeId, RedisMessagePublisher messagePublisher) {
        this.nodeId = nodeId;
        this.messagePublisher = messagePublisher;
    }

    public String getNodeId() {
        return nodeId;
    }

    public RedisMessagePublisher getMessagePublisher() {
        return messagePublisher;
    }

    public void addBridge(SchedulerBridge bridge) {
        bridges.add(bridge);
    }

    public void removeBridge(SchedulerBridge bridge) {
        bridges.remove(bridge);
    }

    /**
     * Bridges a scheduler result to all connected clients.
     * @param data the result payload to send
     */
    public void bridge(String data) {
        for (SchedulerBridge bridge : bridges) {
            try {
                bridge.bridge(data);
            } catch (Exception e) {
                logger.warn("Failed to bridge scheduler result via {}: {}",
                        bridge.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Bridges a scheduler result to a specific session.
     * @param session the target session
     * @param data the result payload to send
     */
    public void bridge(SchedulerSession session, String data) {
        for (SchedulerBridge bridge : bridges) {
            try {
                bridge.bridge(session, data);
            } catch (Exception e) {
                logger.warn("Failed to bridge scheduler result via {}: {}",
                        bridge.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

}

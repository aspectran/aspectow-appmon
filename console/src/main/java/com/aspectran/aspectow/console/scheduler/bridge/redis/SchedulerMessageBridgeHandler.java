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
package com.aspectran.aspectow.console.scheduler.bridge.redis;

import com.aspectran.aspectow.console.scheduler.manager.SchedulerManager;
import com.aspectran.aspectow.node.manager.NodeMessageProtocol;
import com.aspectran.aspectow.node.redis.RedisMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SchedulerMessageBridgeHandler listens to Redis relay messages related to
 * scheduler management and forwards them to the SchedulerManager.
 */
public class SchedulerMessageBridgeHandler implements RedisMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerMessageBridgeHandler.class);

    private final SchedulerManager schedulerManager;

    public SchedulerMessageBridgeHandler(SchedulerManager schedulerManager) {
        this.schedulerManager = schedulerManager;
    }

    @Override
    public String getCategory() {
        return NodeMessageProtocol.CATEGORY_SCHEDULER;
    }

    @Override
    public void onRelayMessage(String nodeId, String message) {
        if (message.startsWith("command:")) {
            schedulerManager.process(message);
        } else {
            schedulerManager.broadcast(message);
        }
    }

}

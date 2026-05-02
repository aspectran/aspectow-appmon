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

import com.aspectran.aspectow.console.scheduler.bridge.SchedulerBroker;
import com.aspectran.aspectow.console.scheduler.manager.SchedulerManager;
import com.aspectran.aspectow.node.redis.RedisMessageListener;
import org.jspecify.annotations.NonNull;

/**
 * SchedulerMessageBridgeHandler listens to Redis relay messages related to
 * scheduler management and forwards them to the SchedulerManager.
 */
public class SchedulerMessageBridgeHandler implements RedisMessageListener {

    private final SchedulerManager schedulerManager;

    public SchedulerMessageBridgeHandler(SchedulerManager schedulerManager) {
        this.schedulerManager = schedulerManager;
    }

    @Override
    public String getCategory() {
        return SchedulerBroker.CATEGORY_SCHEDULER;
    }

    @Override
    public void onControlMessage(String nodeId, String message) {
        schedulerManager.handleControlMessage(nodeId, message);
    }

    @Override
    public void onRelayMessage(String nodeId, @NonNull String message) {
        if (message.startsWith("command:")) {
            schedulerManager.process(message);
        } else if (message.startsWith("log/s:")) {
            // Target specific session: "log/s:<sessionId>:<loggingGroup>:<line>"
            int idx = message.indexOf(':', 6);
            if (idx != -1) {
                String sessionId = message.substring(6, idx);
                String content = "scheduler:log:" + message.substring(idx + 1);
                schedulerManager.getBroker().getSessions().stream()
                        .filter(session -> session.getId().equals(sessionId))
                        .forEach(session -> schedulerManager.getBroker().bridge(session, content));
            }
        } else {
            schedulerManager.broadcast(message);
        }
    }

}

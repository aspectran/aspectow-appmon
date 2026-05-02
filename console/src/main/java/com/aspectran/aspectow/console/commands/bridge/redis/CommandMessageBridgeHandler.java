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
package com.aspectran.aspectow.console.commands.bridge.redis;

import com.aspectran.aspectow.console.commands.bridge.CommandBroker;
import com.aspectran.aspectow.console.commands.manager.RemoteCommandManager;
import com.aspectran.aspectow.node.redis.RedisMessageListener;

/**
 * CommandMessageBridgeHandler listens to Redis relay messages and forwards
 * them to the RemoteCommandManager.
 */
public class CommandMessageBridgeHandler implements RedisMessageListener {

    private final RemoteCommandManager remoteCommandManager;

    public CommandMessageBridgeHandler(RemoteCommandManager remoteCommandManager) {
        this.remoteCommandManager = remoteCommandManager;
    }

    @Override
    public String getCategory() {
        return CommandBroker.CATEGORY_COMMANDS;
    }

    @Override
    public void onControlMessage(String nodeId, String message) {
        remoteCommandManager.handleControlMessage(nodeId, message);
    }

    @Override
    public void onRelayMessage(String nodeId, String message) {
        remoteCommandManager.process(message);
    }

}

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
package com.aspectran.aspectow.appmon.engine.relay.redis;

import com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager;
import com.aspectran.aspectow.node.redis.RedisMessageListener;

/**
 * RedisMessageRelayHandler listens to relay messages from other nodes via Redis
 * and relays them to the local MessageRelayManager.
 */
public class RedisMessageRelayHandler implements RedisMessageListener {

    private final RemoteMessageRelayer remoteMessageRelayer;

    public RedisMessageRelayHandler(RemoteMessageRelayer remoteMessageRelayer) {
        this.remoteMessageRelayer = remoteMessageRelayer;
    }

    @Override
    public String getCategory() {
        return MessageRelayManager.CATEGORY_APPMON;
    }

    @Override
    public void onControlMessage(String nodeId, String message) {
        remoteMessageRelayer.handleControlMessage(nodeId, message);
    }

    @Override
    public void onRelayMessage(String nodeId, String message) {
        remoteMessageRelayer.relay(message);
    }

}

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
package com.aspectran.aspectow.appmon.engine.relay.remote;

import com.aspectran.aspectow.appmon.engine.relay.CommandOptions;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager;
import com.aspectran.aspectow.node.redis.RedisMessageListener;
import org.jspecify.annotations.NonNull;

import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_FOCUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_JOIN;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_LOAD_PREVIOUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_REFRESH;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_RELEASE;

/**
 * RedisMessageRelayHandler listens to relay messages from other nodes via Redis
 * and relays them to the local MessageRelayManager.
 */
public class RedisMessageRelayHandler implements RedisMessageListener {

    private final MessageRelayManager messageRelayManager;

    public RedisMessageRelayHandler(MessageRelayManager messageRelayManager) {
        this.messageRelayManager = messageRelayManager;
    }

    @Override
    public String getCategory() {
        return MessageRelayManager.CATEGORY_APPMON;
    }

    @Override
    public void onControlMessage(String nodeId, String message) {
        if (messageRelayManager.isSameNode(nodeId)) {
            handleControlMessage(nodeId, message);
        }
    }

    @Override
    public void onRelayMessage(String nodeId, String message) {
        messageRelayManager.relayMessage(nodeId, message);
    }

    /**
     * Handles control messages from the cluster.
     * @param nodeId the ID of the node that sent the message
     * @param message the control message
     */
    private void handleControlMessage(String nodeId, @NonNull String message) {
        CommandOptions options = new CommandOptions(message);
        switch (options.getCommand()) {
            case COMMAND_JOIN:
                messageRelayManager.subscribe(nodeId, options.getAppId());
                break;
            case COMMAND_RELEASE:
                messageRelayManager.unsubscribe(nodeId, options.getAppId());
                break;
            case COMMAND_REFRESH:
            case COMMAND_LOAD_PREVIOUS:
                messageRelayManager.refreshData(options);
                break;
            case COMMAND_FOCUS:
                messageRelayManager.focus(options);
                break;
        }
    }

}

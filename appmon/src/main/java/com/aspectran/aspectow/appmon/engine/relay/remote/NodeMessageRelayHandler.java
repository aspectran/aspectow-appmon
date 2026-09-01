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
package com.aspectran.aspectow.appmon.engine.relay.remote;

import com.aspectran.aspectow.appmon.engine.relay.CommandOptions;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager;
import com.aspectran.aspectow.node.manager.NodeMessageListener;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_LOAD_PREVIOUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_REFRESH;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_SUBSCRIBE;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_UNSUBSCRIBE;

/**
 * RedisMessageRelayHandler listens to relay messages from other nodes via Redis
 * and relays them to the local MessageRelayManager.
 */
public class NodeMessageRelayHandler implements NodeMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeMessageRelayHandler.class);

    private final MessageRelayManager messageRelayManager;

    public NodeMessageRelayHandler(MessageRelayManager messageRelayManager) {
        this.messageRelayManager = messageRelayManager;
    }

    @Override
    public String getCategory() {
        return MessageRelayManager.CATEGORY_APPMON;
    }

    @Override
    public void onControlMessage(String nodeId, String message) {
        handleControlMessage(message);
    }

    @Override
    public void onRelayMessage(String nodeId, String message) {
        messageRelayManager.relayLocally(message);
    }

    @Override
    public void onRelayMessage(String nodeId, String sessionId, String message) {
        messageRelayManager.relayLocally(sessionId, message);
    }

    /**
     * Handles control messages from the cluster.
     * @param message the control message
     */
    private void handleControlMessage(@NonNull String message) {
        CommandOptions commandOptions = new CommandOptions();
        try {
            commandOptions.readFrom(message);
        } catch (Exception e) {
            logger.error("Failed to parse control message: {}", message, e);
            return;
        }
        switch (commandOptions.getCommand()) {
            case COMMAND_SUBSCRIBE:
                messageRelayManager.subscribeRemotely(commandOptions);
                break;
            case COMMAND_UNSUBSCRIBE:
                messageRelayManager.unsubscribeRemotely(commandOptions);
                break;
            case COMMAND_REFRESH:
            case COMMAND_LOAD_PREVIOUS:
                messageRelayManager.refreshDataRemotely(commandOptions);
                break;
        }
    }

}

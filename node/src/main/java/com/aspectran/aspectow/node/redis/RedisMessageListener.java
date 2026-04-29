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
package com.aspectran.aspectow.node.redis;

/**
 * Listener interface for receiving and processing Redis messages
 * within the Aspectow cluster.
 *
 * <p>Created: 2026-04-18</p>
 */
public interface RedisMessageListener {

    /**
     * Returns the category of the relay messages this listener is interested in.
     * @return the category string, or null to receive all messages
     */
    default String getCategory() {
        return null;
    }

    /**
     * Called when a management control message is received.
     * @param message the control message content
     */
    default void onControlMessage(String message) {
    }

    /**
     * Called when a management control message is received for a specific node.
     * @param nodeId the node ID
     * @param message the control message content
     */
    default void onControlMessage(String nodeId, String message) {
        onControlMessage(message);
    }

    /**
     * Called when an application-specific relay message is received.
     * @param message the relay message content
     */
    default void onRelayMessage(String message) {
    }

    /**
     * Called when an application-specific relay message is received for a specific node.
     * @param nodeId the node ID
     * @param message the relay message content
     */
    default void onRelayMessage(String nodeId, String message) {
        onRelayMessage(message);
    }

}

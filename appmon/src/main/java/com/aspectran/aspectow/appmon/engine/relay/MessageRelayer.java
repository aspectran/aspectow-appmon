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
package com.aspectran.aspectow.appmon.engine.relay;

/**
 * A relayer for broadcasting messages to connected clients.
 * This can be implemented using various communication protocols like WebSocket or polling.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public interface MessageRelayer {

    /**
     * Relays a message to all connected sessions.
     * @param message the message to relay
     */
    void relay(String message);

    /**
     * Relays a message to a specific session.
     * @param relaySession the session to send the message to
     * @param message the message to relay
     */
    void relay(RelaySession relaySession, String message);

    /**
     * Checks if the relayer is currently being used by a specific instance.
     * @param instanceName the name of the instance
     * @return {@code true} if the instance is using this relayer, {@code false} otherwise
     */
    boolean isUsingInstance(String instanceName);

}

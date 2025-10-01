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
package com.aspectran.appmon.service;


/**
 * A service for exporting and broadcasting messages to connected clients.
 * This service can be implemented using various communication protocols like WebSocket or polling.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public interface ExportService {

    /**
     * Broadcasts a message to all connected sessions.
     * @param message the message to broadcast
     */
    void broadcast(String message);

    /**
     * Broadcasts a message to a specific session.
     * @param serviceSession the session to send the message to
     * @param message the message to broadcast
     */
    void broadcast(ServiceSession serviceSession, String message);

    /**
     * Checks if the service is currently being used by a specific instance.
     * @param instanceName the name of the instance
     * @return {@code true} if the instance is using this service, {@code false} otherwise
     */
    boolean isUsingInstance(String instanceName);

}

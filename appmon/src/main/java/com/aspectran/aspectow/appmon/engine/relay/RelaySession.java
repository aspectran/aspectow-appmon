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
 * An interface representing a client session for an export service.
 * It provides a protocol-agnostic way to manage session state, such as joined apps.
 *
 * <p>Created: 2025. 2. 12.</p>
 */
public interface RelaySession {

    /**
     * Gets the unique identifier of this session.
     * @return the session ID
     */
    String getId();

    /**
     * Gets the names of the apps that this session has joined.
     * @return an array of app names
     */
    String[] getJoinedApps();

    /**
     * Sets the names of the apps that this session has joined.
     * @param appIds an array of app IDs
     */
    void setJoinedApps(String[] appIds);

    /**
     * Removes the joined apps from this session.
     */
    void removeJoinedApps();

    /**
     * Gets the time zone of the client.
     * @return the time zone ID string
     */
    String getTimeZone();

    /**
     * Checks if the session is still valid (e.g., open and not expired).
     * @return {@code true} if the session is valid, {@code false} otherwise
     */
    boolean isValid();

}

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
package com.aspectran.appmon.exporter.event;

import com.aspectran.appmon.exporter.ExporterType;

/**
 * An interface for reading event data from a source.
 * Implementations of this interface are responsible for collecting specific types of events.
 *
 * <p>Created: 2024-12-18</p>
 */
public interface EventReader {

    /**
     * Returns the type of the exporter, which is always {@link ExporterType#EVENT}.
     * @return the exporter type
     */
    default ExporterType getType() {
        return ExporterType.EVENT;
    }

    /**
     * Initializes the event reader.
     * @throws Exception if initialization fails
     */
    default void init() throws Exception {
    }

    /**
     * Starts the event reader.
     * @throws Exception if starting fails
     */
    void start() throws Exception;

    /**
     * Stops the event reader and releases any resources.
     */
    void stop();

    /**
     * Reads the current event data and returns it as a JSON string.
     * @return a JSON string representing the event data, or {@code null} if no data is available
     */
    String read();

    /**
     * Checks if the event data has changed since the last read.
     * @return {@code true} if the data has changed, {@code false} otherwise
     */
    default boolean hasChanges() {
        return false;
    }

}

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
package com.aspectran.appmon.exporter;

import com.aspectran.appmon.service.CommandOptions;
import com.aspectran.utils.lifecycle.LifeCycle;

import java.util.List;

/**
 * Represents a component that exports data from a specific source.
 * Exporters are responsible for reading data such as logs, metrics, or events
 * and broadcasting it to connected clients.
 * All exporters must be thread-safe.
 *
 * <p>Created: 2024-12-18</p>
 */
public interface Exporter extends LifeCycle {

    /**
     * Returns the type of the exporter.
     * @return the exporter type
     */
    ExporterType getType();

    /**
     * Returns the name of the exporter.
     * @return the exporter name
     */
    String getName();

    /**
     * Reads data from the source and adds it to the provided list of messages.
     * This method may be called to get the full content from a source.
     * @param messages a list to which the read messages will be added
     * @param commandOptions options for the command, can be {@code null}
     */
    void read(List<String> messages, CommandOptions commandOptions);

    /**
     * Reads data from the source only if it has changed since the last read,
     * and adds it to the provided list of messages.
     * @param messages a list to which the read messages will be added
     * @param commandOptions options for the command, can be {@code null}
     */
    void readIfChanged(List<String> messages, CommandOptions commandOptions);

    /**
     * Broadcasts a message to all connected clients.
     * @param message the message to broadcast
     */
    void broadcast(String message);

}

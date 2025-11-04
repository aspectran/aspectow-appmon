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
package com.aspectran.appmon.engine.exporter;

import com.aspectran.appmon.engine.service.CommandOptions;
import com.aspectran.utils.lifecycle.AbstractLifeCycle;

import java.util.List;

/**
 * Abstract base class for {@link Exporter} implementations.
 * Provides a skeletal implementation of the Exporter interface to minimize the effort required to implement it.
 *
 * <p>Created: 2025-04-07</p>
 */
public abstract class AbstractExporter extends AbstractLifeCycle implements Exporter {

    private final ExporterType type;

    /**
     * Instantiates a new AbstractExporter.
     * @param type the type of the exporter
     */
    public AbstractExporter(ExporterType type) {
        this.type = type;
    }

    @Override
    public ExporterType getType() {
        return type;
    }

    /**
     * This implementation is empty.
     * Subclasses should override this method if they need to implement logic
     * for reading data only when it has changed.
     * @param messages a list to which the read messages will be added
     * @param commandOptions options for the command, can be {@code null}
     */
    @Override
    public void readIfChanged(List<String> messages, CommandOptions commandOptions) {
    }

}

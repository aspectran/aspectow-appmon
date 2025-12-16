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
package com.aspectran.appmon.engine.exporter.event;

import com.aspectran.appmon.engine.config.EventInfo;
import com.aspectran.appmon.engine.exporter.ExporterManager;
import com.aspectran.appmon.engine.persist.counter.EventCount;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Abstract base class for {@link EventReader} implementations.
 * Provides common functionalities for event readers.
 *
 * <p>Created: 2025. 1. 27.</p>
 */
public abstract class AbstractEventReader implements EventReader {

    private final ExporterManager exporterManager;

    private final EventInfo eventInfo;

    private final EventCount eventCount;

    private volatile EventExporter eventExporter;

    /**
     * Instantiates a new AbstractEventReader.
     * @param exporterManager the exporter manager
     * @param eventInfo the event configuration
     * @param eventCount the event counter, can be {@code null}
     */
    public AbstractEventReader(@NonNull ExporterManager exporterManager,
                               @NonNull EventInfo eventInfo,
                               @Nullable EventCount eventCount) {
        this.exporterManager = exporterManager;
        this.eventInfo = eventInfo;
        this.eventCount = eventCount;
    }

    /**
     * Gets the exporter manager.
     * @return the exporter manager
     */
    public ExporterManager getExporterManager() {
        return exporterManager;
    }

    /**
     * Gets the associated event exporter.
     * @return the event exporter
     */
    protected EventExporter getEventExporter() {
        if (eventExporter == null) {
            synchronized (this) {
                if (eventExporter == null) {
                    eventExporter = exporterManager.getExporter(getEventInfo().getName());
                }
            }
        }
        return eventExporter;
    }

    /**
     * Gets the event configuration.
     * @return the event info
     */
    public EventInfo getEventInfo() {
        return eventInfo;
    }

    /**
     * Gets the event counter.
     * @return the event count, or {@code null} if not configured
     */
    public EventCount getEventCount() {
        return eventCount;
    }

}

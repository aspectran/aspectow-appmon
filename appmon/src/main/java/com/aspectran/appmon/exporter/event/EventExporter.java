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

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.appmon.exporter.AbstractExporter;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.appmon.service.CommandOptions;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.List;

/**
 * An exporter for collecting and broadcasting event data.
 * It uses an {@link EventReader} to read data and can periodically broadcast changes.
 *
 * <p>Created: 2024-12-18</p>
 */
public class EventExporter extends AbstractExporter {

    private final ExporterManager exporterManager;

    private final EventInfo eventInfo;

    private final EventReader eventReader;

    private final String prefix;

    private final int sampleInterval;

    private EventExportTimer timer;

    /**
     * Instantiates a new EventExporter.
     * @param exporterManager the exporter manager
     * @param eventInfo the event configuration
     * @param eventReader the reader for the event data
     */
    public EventExporter(@NonNull ExporterManager exporterManager,
                         @NonNull EventInfo eventInfo,
                         @NonNull EventReader eventReader) {
        super(eventReader.getType());
        this.exporterManager = exporterManager;
        this.eventInfo = eventInfo;
        this.eventReader = eventReader;
        this.prefix = eventInfo.getInstanceName() + ":" + getType() + ":" + eventInfo.getName() + ":";
        this.sampleInterval = eventInfo.getSampleInterval();
    }

    @Override
    public String getName() {
        return eventInfo.getName();
    }

    @Override
    public void read(@NonNull List<String> messages, CommandOptions commandOptions) {
        String json = eventReader.read();
        if (json != null) {
            messages.add(prefix + json);
        }
    }

    @Override
    public void readIfChanged(@NonNull List<String> messages, CommandOptions commandOptions) {
        if (eventReader.hasChanges()) {
            read(messages, commandOptions);
        }
    }

    @Override
    public void broadcast(String message) {
        if (message != null) {
            exporterManager.broadcast(prefix + message);
        }
    }

    void broadcastIfChanged() {
        if (eventReader.hasChanges()) {
            broadcast(eventReader.read());
        }
    }

    @Override
    protected void doStart() throws Exception {
        eventReader.start();
        if (sampleInterval > 0) {
            if (timer == null) {
                timer = new EventExportTimer(exporterManager, this, sampleInterval);
                timer.schedule();
            }
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (timer != null) {
            timer.destroy();
            timer = null;
        }
        eventReader.stop();
    }

    @Override
    public String toString() {
        if (isStopped()) {
            return ToStringBuilder.toString(super.toString(), eventInfo);
        } else {
            return super.toString();
        }
    }

}

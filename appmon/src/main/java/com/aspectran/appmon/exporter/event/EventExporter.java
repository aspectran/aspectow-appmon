/*
 * Copyright (c) 2020-2025 The Aspectran Project
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
import com.aspectran.appmon.exporter.Exporter;
import com.aspectran.appmon.persist.counter.EventCount;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>Created: 2024-12-18</p>
 */
public class EventExporter extends Exporter {

    private static final String TYPE = ":event:";

    private final EventExporterManager eventExporterManager;

    private final EventInfo eventInfo;

    private final EventReader eventReader;

    private final String label;

    private final int sampleInterval;

    private Timer timer;

    public EventExporter(@NonNull EventExporterManager eventExporterManager,
                         @NonNull EventInfo eventInfo,
                         @NonNull EventReader eventReader) {
        this.eventExporterManager = eventExporterManager;
        this.eventInfo = eventInfo;
        this.eventReader = eventReader;
        this.label = eventInfo.getInstanceName() + TYPE + eventInfo.getName() + ":";
        this.sampleInterval = eventInfo.getSampleInterval();
    }

    @Override
    public String getName() {
        return eventInfo.getName();
    }

    public void setEventCount(EventCount eventCount) {
        eventReader.setEventCount(eventCount);
    }

    @Override
    public void read(@NonNull List<String> messages) {
        String json = eventReader.read();
        if (json != null) {
            messages.add(label + json);
        }
    }

    @Override
    public void broadcast(String message) {
        eventExporterManager.broadcast(label + message);
    }

    private void broadcastIfChanged() {
        String data = eventReader.readIfChanged();
        if (data != null) {
            broadcast(data);
        }
    }

    @Override
    protected void doStart() throws Exception {
        if (sampleInterval > 0) {
            eventReader.start();
            if (timer == null) {
                String name = new ToStringBuilder("EventReadingTimer")
                        .append("eventReader", eventReader)
                        .append("sampleInterval", sampleInterval)
                        .toString();
                timer = new Timer(name);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        broadcastIfChanged();
                    }
                }, 0, sampleInterval);
            }
        } else {
            eventReader.start();
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (timer != null) {
            timer.cancel();
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

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
import com.aspectran.appmon.persist.counter.EventCount;
import com.aspectran.utils.annotation.jsr305.NonNull;

/**
 * <p>Created: 2025. 1. 27.</p>
 */
public abstract class AbstractEventReader implements EventReader {

    private final EventExporterManager eventExporterManager;

    private final EventInfo eventInfo;

    private EventCount eventCount;

    public AbstractEventReader(@NonNull EventExporterManager eventExporterManager, @NonNull EventInfo eventInfo) {
        this.eventExporterManager = eventExporterManager;
        this.eventInfo = eventInfo;
    }

    public EventExporterManager getEventExporterManager() {
        return eventExporterManager;
    }

    public EventInfo getEventInfo() {
        return eventInfo;
    }

    public EventCount getEventCount() {
        return eventCount;
    }

    @Override
    public void setEventCount(EventCount eventCount) {
        this.eventCount = eventCount;
    }

}

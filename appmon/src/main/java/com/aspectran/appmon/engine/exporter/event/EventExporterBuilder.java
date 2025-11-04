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
import com.aspectran.appmon.engine.exporter.event.activity.ActivityEventReader;
import com.aspectran.appmon.engine.exporter.event.session.SessionEventReader;
import com.aspectran.appmon.engine.persist.counter.EventCount;
import com.aspectran.utils.ClassUtils;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.annotation.jsr305.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder for creating {@link EventExporter} instances.
 * It determines the appropriate {@link EventReader} based on the configuration and constructs the exporter.
 *
 * <p>Created: 2024-12-18</p>
 */
public abstract class EventExporterBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EventExporterBuilder.class);

    /**
     * Builds a new {@link EventExporter}.
     * @param exporterManager the exporter manager
     * @param eventInfo the event configuration
     * @param eventCount the event counter, can be {@code null}
     * @return a new {@link EventExporter} instance
     * @throws Exception if the exporter cannot be built
     */
    @NonNull
    public static EventExporter build(
            @NonNull ExporterManager exporterManager,
            @NonNull EventInfo eventInfo,
            @Nullable EventCount eventCount) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(ToStringBuilder.toString("Create EventExporter", eventInfo));
        }
        EventReader eventReader = createEventReader(exporterManager, eventInfo, eventCount);
        eventReader.init();
        return new EventExporter(exporterManager, eventInfo, eventReader);
    }

    @NonNull
    private static EventReader createEventReader(
            @NonNull ExporterManager exporterManager,
            @NonNull EventInfo eventInfo,
            EventCount eventCount) throws Exception {
        if (!eventInfo.hasReader()) {
            if ("activity".equals(eventInfo.getName())) {
                return new ActivityEventReader(exporterManager, eventInfo, eventCount);
            } else if ("session".equals(eventInfo.getName())) {
                return new SessionEventReader(exporterManager, eventInfo, eventCount);
            } else {
                throw new IllegalArgumentException("No event reader specified for " + eventInfo.getName() + " " + eventInfo);
            }
        }
        try {
            Class<EventReader> readerType = ClassUtils.classForName(eventInfo.getReader());
            Object[] args = { exporterManager, eventInfo };
            Class<?>[] argTypes = { ExporterManager.class, EventInfo.class };
            return ClassUtils.createInstance(readerType, args, argTypes);
        } catch (Exception e) {
            throw new Exception(ToStringBuilder.toString("Failed to create event reader", eventInfo), e);
        }
    }

}

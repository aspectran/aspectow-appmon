/*
 * Copyright (c) 2018-2025 The Aspectran Project
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
package com.aspectran.aspectow.appmon.exporter.event;

import com.aspectran.aspectow.appmon.config.EventInfo;
import com.aspectran.utils.ClassUtils;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.logging.Logger;
import com.aspectran.utils.logging.LoggerFactory;

import java.util.List;

/**
 * <p>Created: 2024-12-18</p>
 */
public class EventExporterManagerBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EventExporterManagerBuilder.class);

    @NonNull
    public static void build(@NonNull EventExporterManager eventExporterManager,
                             @NonNull List<EventInfo> eventInfoList) throws Exception {
        for (EventInfo eventInfo : eventInfoList) {
            if (logger.isDebugEnabled()) {
                logger.debug(ToStringBuilder.toString("Create EventExporter", eventInfo));
            }

            eventInfo.validateRequiredParameters();

            EventReader eventReader = createEventReader(eventExporterManager, eventInfo);
            EventExporter eventExporter = new EventExporter(eventExporterManager, eventInfo, eventReader);
            eventExporterManager.addExporter(eventExporter);
        }
    }

    @NonNull
    private static EventReader createEventReader(
            @NonNull EventExporterManager eventExporterManager, @NonNull EventInfo eventInfo) throws Exception {
        try {
            Class<EventReader> readerType = ClassUtils.classForName(eventInfo.getReader());
            Object[] args = { eventExporterManager, eventInfo };
            Class<?>[] argTypes = { EventExporterManager.class, EventInfo.class };
            return ClassUtils.createInstance(readerType, args, argTypes);
        } catch (Exception e) {
            throw new Exception(ToStringBuilder.toString("Failed to create event reader", eventInfo), e);
        }
    }

}

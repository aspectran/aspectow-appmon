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
package com.aspectran.aspectow.appmon.backend.persist.counter;

import com.aspectran.aspectow.appmon.backend.config.EventInfo;
import com.aspectran.aspectow.appmon.backend.persist.PersistManager;
import com.aspectran.aspectow.appmon.backend.persist.counter.activity.ActivityCounterReader;
import com.aspectran.aspectow.appmon.backend.persist.counter.session.SessionCounterReader;
import com.aspectran.utils.ClassUtils;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.logging.Logger;
import com.aspectran.utils.logging.LoggerFactory;

import java.util.List;

/**
 * <p>Created: 2025. 2. 12.</p>
 */
public abstract class CounterReaderBuilder {

    private static final Logger logger = LoggerFactory.getLogger(CounterReaderBuilder.class);

    @NonNull
    public static void build(@NonNull PersistManager persistManager,
                             @NonNull List<EventInfo> eventInfoList) throws Exception {
        for (EventInfo eventInfo : eventInfoList) {
            if (logger.isDebugEnabled()) {
                logger.debug(ToStringBuilder.toString("Create CounterPersist", eventInfo));
            }

            eventInfo.validateRequiredParameters();

            CounterReader counterReader = createCounterReader(eventInfo);
            persistManager.getCounterPersist().addCounterReader(counterReader);
        }
    }

    @NonNull
    private static CounterReader createCounterReader(@NonNull EventInfo eventInfo) throws Exception {
        if (!eventInfo.hasReader()) {
            if ("activity".equals(eventInfo.getName())) {
                return new ActivityCounterReader(eventInfo);
            } else if ("session".equals(eventInfo.getName())) {
                return new SessionCounterReader(eventInfo);
            } else {
                throw new IllegalArgumentException("No counter reader specified for " + eventInfo.getName() + " " + eventInfo);
            }
        }
        try {
            Class<CounterReader> readerType = ClassUtils.classForName(eventInfo.getReader());
            Object[] args = { eventInfo };
            Class<?>[] argTypes = { EventInfo.class };
            return ClassUtils.createInstance(readerType, args, argTypes);
        } catch (Exception e) {
            throw new Exception(ToStringBuilder.toString("Failed to create counter reader", eventInfo), e);
        }
    }

}

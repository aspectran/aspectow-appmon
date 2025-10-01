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
package com.aspectran.appmon.persist.counter;

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.appmon.persist.counter.activity.ActivityEventCounter;
import com.aspectran.appmon.persist.counter.session.SessionEventCounter;
import com.aspectran.utils.ClassUtils;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.annotation.jsr305.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder for creating {@link EventCounter} instances.
 * It determines the appropriate {@link EventCounter} implementation based on the configuration.
 *
 * <p>Created: 2025. 2. 12.</p>
 */
public abstract class EventCounterBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EventCounterBuilder.class);

    /**
     * Builds a new {@link EventCounter}.
     * @param eventInfo the event configuration
     * @return a new {@link EventCounter} instance, or {@code null} if no counter is needed
     * @throws Exception if the counter cannot be built
     */
    @Nullable
    public static EventCounter build(EventInfo eventInfo) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(ToStringBuilder.toString("Create CounterPersist", eventInfo));
        }
        return createEventCounter(eventInfo);
    }

    @Nullable
    private static EventCounter createEventCounter(@NonNull EventInfo eventInfo) throws Exception {
        if (!eventInfo.hasCounter()) {
            if ("activity".equals(eventInfo.getName())) {
                return new ActivityEventCounter(eventInfo);
            } else if ("session".equals(eventInfo.getName())) {
                return new SessionEventCounter(eventInfo);
            } else if (eventInfo.getReader() != null) {
                return null;
            } else {
                throw new IllegalArgumentException("No event counter specified for " + eventInfo.getName() + " " + eventInfo);
            }
        }
        try {
            Class<EventCounter> counterType = ClassUtils.classForName(eventInfo.getCounter());
            Object[] args = { eventInfo };
            Class<?>[] argTypes = { EventInfo.class };
            return ClassUtils.createInstance(counterType, args, argTypes);
        } catch (Exception e) {
            throw new Exception(ToStringBuilder.toString("Failed to create event counter", eventInfo), e);
        }
    }

}

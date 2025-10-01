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
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for {@link EventCounter} implementations.
 * Provides common functionalities for event counters, including listener management and rollup logic.
 *
 * <p>Created: 2025. 1. 27.</p>
 */
public abstract class AbstractEventCounter implements EventCounter {

    private final List<EventCountRollupListener> eventCountRollupListeners = new ArrayList<>();

    private final EventInfo eventInfo;

    private final String eventName;

    private final EventCount eventCount = new EventCount();

    /**
     * Instantiates a new AbstractEventCounter.
     * @param eventInfo the event configuration
     */
    public AbstractEventCounter(@NonNull EventInfo eventInfo) {
        this.eventInfo = eventInfo;
        this.eventName = eventInfo.getName();
    }

    @Override
    public String getInstanceName() {
        return eventInfo.getInstanceName();
    }

    @Override
    public String getEventName() {
        return eventName;
    }

    /**
     * Gets the event configuration.
     * @return the event info
     */
    public EventInfo getEventInfo() {
        return eventInfo;
    }

    @Override
    public EventCount getEventCount() {
        return eventCount;
    }

    @Override
    public void addEventRollupListener(EventCountRollupListener eventRollupListener) {
        eventCountRollupListeners.add(eventRollupListener);
    }

    @Override
    public void rollup(String datetime) {
        eventCount.rollup(datetime);
        for (EventCountRollupListener eventRollupListener : eventCountRollupListeners) {
            eventRollupListener.onRolledUp(eventCount);
        }
    }

    @Override
    public void reset(String datetime, long total, long delta, long error) {
        eventCount.reset(datetime, total, delta, error);
        for (EventCountRollupListener eventRollupListener : eventCountRollupListeners) {
            eventRollupListener.onRolledUp(eventCount);
        }
    }

}

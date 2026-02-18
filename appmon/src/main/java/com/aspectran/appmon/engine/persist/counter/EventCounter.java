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
package com.aspectran.appmon.engine.persist.counter;

import java.time.LocalDateTime;

/**
 * An interface for counting events.
 * Implementations of this interface are responsible for tracking and rolling up event counts.
 *
 * <p>Created: 2025. 2. 12.</p>
 */
public interface EventCounter {

    /**
     * Initializes the event counter.
     * @throws Exception if initialization fails
     */
    void initialize() throws Exception;

    /**
     * Gets the name of the instance this counter belongs to.
     * @return the instance name
     */
    String getInstanceName();

    /**
     * Gets the name of the event being counted.
     * @return the event name
     */
    String getEventName();

    /**
     * Gets the underlying {@link EventCount} object that holds the count data.
     * @return the event count object
     */
    EventCount getEventCount();

    /**
     * Adds a listener for event count rollup events.
     * @param eventRollupListener the listener to add
     */
    void addEventRollupListener(EventCountRollupListener eventRollupListener);

    /**
     * Rolls up the current tallying counts into the tallied counts for the specified datetime.
     * @param datetime the datetime for the rollup
     */
    void rollup(LocalDateTime datetime);

    /**
     * Resets the counter with the given values.
     * @param datetime the datetime for the reset
     * @param total the total count
     * @param delta the delta count since the last reset
     * @param error the error count
     */
    void reset(LocalDateTime datetime, long total, long delta, long error);

}

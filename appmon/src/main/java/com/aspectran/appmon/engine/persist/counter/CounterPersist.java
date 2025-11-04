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

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a list of {@link EventCounter} instances.
 * This class acts as a central registry for all active event counters.
 *
 * <p>Created: 2025-02-12</p>
 */
public class CounterPersist {

    private final List<EventCounter> eventCounterList = new ArrayList<>();

    /**
     * Adds an event counter to the manager.
     * @param eventCounter the event counter to add
     */
    public void addEventCounter(EventCounter eventCounter) {
        eventCounterList.add(eventCounter);
    }

    /**
     * Gets the list of all registered event counters.
     * @return a list of {@link EventCounter} instances
     */
    public List<EventCounter> getEventCounterList() {
        return eventCounterList;
    }

}

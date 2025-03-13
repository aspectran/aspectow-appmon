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
package com.aspectran.appmon.persist.counter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>Created: 2025-02-12</p>
 */
public class CounterPersist {

    private final Map<String, EventCounter> eventCounterMap = new LinkedHashMap<>();

    public void addEventCounter(EventCounter eventCounter) {
        eventCounterMap.put(eventCounter.getInstanceName() + ":" + eventCounter.getEventName(), eventCounter);
    }

    public Collection<EventCounter> getEventCounterList() {
        return eventCounterMap.values();
    }

    public EventCounter getEventCounter(String instanceName, String eventName) {
        EventCounter eventCounter = eventCounterMap.get(instanceName + ":" + eventName);
        if (eventCounter == null) {
            throw new IllegalArgumentException("No event counter found for event '" +
                    eventName + "' of instance '" + instanceName + "'");
        }
        return eventCounter;
    }

}

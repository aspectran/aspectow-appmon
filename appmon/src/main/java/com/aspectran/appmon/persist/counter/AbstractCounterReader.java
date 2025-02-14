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

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.utils.annotation.jsr305.NonNull;

/**
 * <p>Created: 2025. 1. 27.</p>
 */
public abstract class AbstractCounterReader implements CounterReader {

    private final EventInfo eventInfo;

    private final CounterData counterData = new CounterData();

    public AbstractCounterReader(@NonNull EventInfo eventInfo) {
        this.eventInfo = eventInfo;
    }

    public EventInfo getEventInfo() {
        return eventInfo;
    }

    @Override
    public String getInstanceName() {
        return eventInfo.getInstanceName();
    }

    @Override
    public CounterData getCounterData() {
        return counterData;
    }

}

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
package com.aspectran.aspectow.appmon.exporter.event.activity;

import com.aspectran.aspectow.appmon.config.EventInfo;
import com.aspectran.aspectow.appmon.exporter.event.AbstractEventReader;
import com.aspectran.aspectow.appmon.exporter.event.EventExporterManager;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.utils.json.JsonBuilder;
import com.aspectran.utils.statistic.CounterStatistic;

/**
 * <p>Created: 2025. 1. 27.</p>
 */
public class ActivityEventReader extends AbstractEventReader {

    private CounterStatistic activityCounter;

    private long oldTotal;

    public ActivityEventReader(EventExporterManager eventExporterManager, EventInfo eventInfo) {
        super(eventExporterManager, eventInfo);
    }

    @Override
    public void start() throws Exception {
        ActivityContext context = CoreServiceHolder.findActivityContext(getTarget());
        if (context == null) {
            throw new Exception("Could not find ActivityContext named '" + getTarget() + "'");
        }
        activityCounter = context.getActivityCounter();
    }

    @Override
    public void stop() {
    }

    @Override
    public String read() {
        long current = activityCounter.getCurrent();
        long max = activityCounter.getMax();
        long total = activityCounter.getTotal();
        oldTotal = total;
        return new JsonBuilder()
            .prettyPrint(false)
            .nullWritable(false)
            .object()
                .put("current", current)
                .put("max", max)
                .put("total", total)
            .endObject()
            .toString();
    }

    @Override
    public String readIfChanged() {
        long total = activityCounter.getTotal();
        if (total != oldTotal) {
            return read();
        } else {
            return null;
        }
    }

}

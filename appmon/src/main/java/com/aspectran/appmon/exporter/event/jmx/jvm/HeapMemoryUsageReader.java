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
package com.aspectran.appmon.exporter.event.jmx.jvm;

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.appmon.exporter.event.jmx.AbstractMBeanReader;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.json.JsonBuilder;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * <p>Created: 2025-06-30</p>
 */
public class HeapMemoryUsageReader extends AbstractMBeanReader {

    private MemoryMXBean memoryMXBean;

    private long oldUsed = -1L;

    private long oldMax;

    public HeapMemoryUsageReader(
            ExporterManager exporterManager,
            EventInfo eventInfo) {
        super(exporterManager, eventInfo);
    }

    @Override
    public void start() throws Exception {
        memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public void stop() {
        if (memoryMXBean != null) {
            memoryMXBean = null;
        }
    }

    @Override
    public String read() {
        if (memoryMXBean == null) {
            return null;
        }
        MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();
        long init = memoryUsage.getInit() >> 10;
        long used = memoryUsage.getUsed() >> 10;
        long committed = memoryUsage.getCommitted() >> 10;
        long max = memoryUsage.getMax() >> 10;

        String value = StringUtils.toHumanFriendlyByteSize(memoryUsage.getUsed()) +
                "/" + StringUtils.toHumanFriendlyByteSize(memoryUsage.getMax());

        return new JsonBuilder()
                .prettyPrint(false)
                .nullWritable(false)
                .object()
                    .put("name", getEventInfo().getName())
                    .put("title", getEventInfo().getTitle())
                    .put("value", value)
                    .object("data")
                        .put("init", init)
                        .put("used", used)
                        .put("committed", committed)
                        .put("max", max)
                    .endObject()
                .endObject()
                .toString();
    }

    @Override
    public String readIfChanged() {
        if (memoryMXBean == null) {
            return null;
        }
        MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();

        boolean changed = (memoryUsage.getUsed() != oldUsed ||
                memoryUsage.getMax() != oldMax);
        if (changed) {
            oldUsed = memoryUsage.getUsed();
            oldMax = memoryUsage.getMax();
            return read();
        } else {
            return null;
        }
    }

}

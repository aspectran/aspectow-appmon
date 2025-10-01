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
package com.aspectran.appmon.exporter.metric.jvm;

import com.aspectran.appmon.config.MetricInfo;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.appmon.exporter.metric.AbstractMetricReader;
import com.aspectran.appmon.exporter.metric.MetricData;
import com.aspectran.utils.StringUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * A {@link com.aspectran.appmon.exporter.metric.MetricReader} for monitoring JVM heap memory usage.
 * It uses the {@link java.lang.management.MemoryMXBean} to get memory statistics.
 *
 * <p>Created: 2025-06-30</p>
 */
public class HeapMemoryUsageReader extends AbstractMetricReader {

    private MemoryMXBean memoryMXBean;

    private long oldUsed = -1L;

    /**
     * Instantiates a new HeapMemoryUsageReader.
     * @param exporterManager the exporter manager
     * @param metricInfo the metric configuration
     */
    public HeapMemoryUsageReader(
            ExporterManager exporterManager,
            MetricInfo metricInfo) {
        super(exporterManager, metricInfo);
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
    public MetricData getMetricData(boolean greater) {
        if (memoryMXBean == null) {
            return null;
        }

        MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();
        long used0 = memoryUsage.getUsed();
        long used = used0 >> 10;
        long usedToCompare = used >> 10;
        if (greater && usedToCompare == oldUsed) {
            return null;
        }

        long init = memoryUsage.getInit() >> 10;
        long committed = memoryUsage.getCommitted() >> 10;
        long max = memoryUsage.getMax() >> 10;

        oldUsed = usedToCompare;

        String usedKB = StringUtils.toHumanFriendlyByteSize(memoryUsage.getUsed());
        String maxKB = StringUtils.toHumanFriendlyByteSize(memoryUsage.getMax());

        return new MetricData(getMetricInfo())
                .setFormat("{usedKB}/{maxKB}")
                .putData("init", init)
                .putData("used", used)
                .putData("usedKB", usedKB)
                .putData("committed", committed)
                .putData("max", max)
                .putData("maxKB", maxKB);
    }

    @Override
    public boolean hasChanges() {
        if (memoryMXBean == null) {
            return false;
        }
        MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();
        long usedToCompare = memoryUsage.getUsed() >> 20;
        return (usedToCompare != oldUsed);
    }

}

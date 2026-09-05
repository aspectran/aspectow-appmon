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
package com.aspectran.aspectow.appmon.engine.exporter.metric.jvm;

import com.aspectran.aspectow.appmon.engine.config.MetricInfo;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.engine.exporter.metric.AbstractMetricReader;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricData;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricReader;
import com.aspectran.utils.DataSizeUtils;
import org.jspecify.annotations.NonNull;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * A {@link MetricReader} for monitoring JVM heap memory usage.
 * It uses the {@link java.lang.management.MemoryMXBean} to get memory statistics.
 *
 * <p>Collected metric data points:</p>
 * <ul>
 *   <li>{@code init} - Initial amount of memory in KB that the Java Virtual Machine initially requests from the operating system.</li>
 *   <li>{@code used} - Amount of used memory in KB.</li>
 *   <li>{@code usedKB} - Human-friendly byte size representation of used memory.</li>
 *   <li>{@code committed} - Amount of memory in KB that is committed for the Java Virtual Machine to use.</li>
 *   <li>{@code max} - Maximum amount of memory in KB that can be used for memory management.</li>
 *   <li>{@code maxKB} - Human-friendly byte size representation of maximum memory.</li>
 * </ul>
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
        memoryMXBean = ManagementFactory.getPlatformMXBean(MemoryMXBean.class);
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
        long usedToCompare = memoryUsage.getUsed() >> 20;
        if (greater && usedToCompare == oldUsed) {
            return null;
        }

        oldUsed = usedToCompare;
        return buildMetricData(memoryUsage);
    }

    @Override
    public MetricData getMetricDataIfChanged() {
        if (memoryMXBean == null) {
            return null;
        }

        MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();
        long usedToCompare = memoryUsage.getUsed() >> 20;
        if (usedToCompare == oldUsed) {
            return null;
        }

        oldUsed = usedToCompare;
        return buildMetricData(memoryUsage);
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

    private MetricData buildMetricData(@NonNull MemoryUsage memoryUsage) {
        long init = memoryUsage.getInit() >> 10;
        long used = memoryUsage.getUsed() >> 10;
        long committed = memoryUsage.getCommitted() >> 10;
        long max = memoryUsage.getMax() >> 10;

        String usedKB = DataSizeUtils.toHumanFriendlyByteSize(memoryUsage.getUsed());
        String maxKB = DataSizeUtils.toHumanFriendlyByteSize(memoryUsage.getMax());

        return new MetricData(getMetricInfo())
                .setFormat("{usedKB}/{maxKB}")
                .putData("init", init)
                .putData("used", used)
                .putData("usedKB", usedKB)
                .putData("committed", committed)
                .putData("max", max)
                .putData("maxKB", maxKB);
    }

}

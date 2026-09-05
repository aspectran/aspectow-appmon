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
import com.sun.management.OperatingSystemMXBean;
import org.jspecify.annotations.NonNull;

import java.lang.management.ManagementFactory;

/**
 * A {@link MetricReader} for monitoring JVM process and system CPU usage.
 * It uses {@link com.sun.management.OperatingSystemMXBean} to get CPU statistics.
 *
 * <p>Collected metric data points:</p>
 * <ul>
 *   <li>{@code processCpu} - Recent CPU usage of the JVM process as a percentage (0.0% to 100.0%).</li>
 *   <li>{@code systemCpu} - Recent CPU usage of the entire operating system as a percentage (0.0% to 100.0%).</li>
 *   <li>{@code processors} - The number of processors available to the Java Virtual Machine.</li>
 *   <li>{@code systemLoad} - The system load average for the last minute (-1.0 if not available).</li>
 * </ul>
 *
 * <p>Created: 2026-09-05</p>
 */
public class CpuUsageReader extends AbstractMetricReader {

    private OperatingSystemMXBean osMXBean;

    private double oldProcessCpu = -1.0;

    private double currentProcessCpu = -1.0;

    private long lastSampleTime;

    /**
     * Instantiates a new CpuUsageReader.
     * @param exporterManager the exporter manager
     * @param metricInfo the metric configuration
     */
    public CpuUsageReader(
            @NonNull ExporterManager exporterManager,
            @NonNull MetricInfo metricInfo) {
        super(exporterManager, metricInfo);
    }

    @Override
    public void start() throws Exception {
        osMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    }

    @Override
    public void stop() {
        if (osMXBean != null) {
            osMXBean = null;
        }
    }

    @Override
    public MetricData getMetricData(boolean greater) {
        if (!sample()) {
            return null;
        }

        if (greater && currentProcessCpu <= oldProcessCpu) {
            return null;
        }

        oldProcessCpu = currentProcessCpu;
        return buildMetricData(currentProcessCpu);
    }

    @Override
    public MetricData getMetricDataIfChanged() {
        if (!sample()) {
            return null;
        }

        if (oldProcessCpu >= 0.0 && Math.abs(currentProcessCpu - oldProcessCpu) < 0.5) {
            return null;
        }

        oldProcessCpu = currentProcessCpu;
        return buildMetricData(currentProcessCpu);
    }

    @Override
    public boolean hasChanges() {
        if (!sample()) {
            return false;
        }
        if (oldProcessCpu < 0.0) {
            return true;
        }
        return (Math.abs(currentProcessCpu - oldProcessCpu) >= 0.5);
    }

    private boolean sample() {
        if (osMXBean == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        // OperatingSystemMXBean.getProcessCpuLoad() distorts if polled within a few milliseconds.
        // Cache the latest sample for at least 100ms.
        if (now - lastSampleTime < 100L && currentProcessCpu >= 0.0) {
            return true;
        }

        double processCpuLoad = osMXBean.getProcessCpuLoad();
        if (!(processCpuLoad >= 0.0)) {
            return false;
        }

        lastSampleTime = now;
        currentProcessCpu = Math.round(processCpuLoad * 1000.0) / 10.0;
        return true;
    }

    private MetricData buildMetricData(double processCpu) {
        double systemCpuLoad = osMXBean.getCpuLoad();
        double systemCpu = (systemCpuLoad >= 0.0) ?
                Math.round(systemCpuLoad * 1000.0) / 10.0 : -1.0;

        double systemLoadAverage = osMXBean.getSystemLoadAverage();
        double systemLoad = (systemLoadAverage >= 0.0) ?
                Math.round(systemLoadAverage * 10.0) / 10.0 : -1.0;

        int processors = osMXBean.getAvailableProcessors();

        MetricData metricData = new MetricData(getMetricInfo())
                .setFormat("{processCpu}%")
                .putData("processCpu", processCpu)
                .putData("systemCpu", systemCpu)
                .putData("processors", processors);

        if (systemLoad >= 0.0) {
            metricData.putData("systemLoad", systemLoad);
        }

        return metricData;
    }

}

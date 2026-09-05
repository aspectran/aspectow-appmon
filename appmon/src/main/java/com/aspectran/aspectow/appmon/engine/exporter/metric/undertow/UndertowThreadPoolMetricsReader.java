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
package com.aspectran.aspectow.appmon.engine.exporter.metric.undertow;

import com.aspectran.aspectow.appmon.engine.config.MetricInfo;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.engine.exporter.metric.AbstractMetricReader;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricData;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricReader;
import com.aspectran.undertow.server.TowServer;
import io.undertow.Undertow;
import org.jspecify.annotations.NonNull;
import org.xnio.management.XnioWorkerMXBean;

/**
 * A {@link MetricReader} for monitoring Undertow's XNIO worker threads.
 * <p>It requires specific JVM system properties to be enabled for statistics collection.</p>
 * <pre>
 *   -Djboss.threads.eqe.statistics=true
 *   -Djboss.threads.eqe.statistics.active-count=true
 * </pre>
 * <p>Or to set the system parameters in aspectran-config.apon:</p>
 * <pre>
 *   system: {
 *     properties: {
 *       jboss.threads.eqe.statistics: true
 *       jboss.threads.eqe.statistics.active-count: true
 *     }
 *   }
 * </pre>
 *
 * <p>Collected metric data points:</p>
 * <ul>
 *   <li>{@code workerName} - The configured name of the Undertow worker.</li>
 *   <li>{@code active} - Number of active worker threads.</li>
 *   <li>{@code total} - Total number of worker threads in the pool.</li>
 *   <li>{@code max} - Maximum configured worker pool size.</li>
 *   <li>{@code core} - Core worker pool size.</li>
 *   <li>{@code busy} - Busy worker thread count.</li>
 *   <li>{@code queued} - Number of queued tasks awaiting execution.</li>
 * </ul>
 *
 * <p>Created: 2025-07-07</p>
 */
public class UndertowThreadPoolMetricsReader extends AbstractMetricReader {

    private String serverId;

    private XnioWorkerMXBean metrics;

    private int oldActive;

    /**
     * Instantiates a new UndertowThreadPoolMetricsReader.
     * @param exporterManager the exporter manager
     * @param metricInfo the metric configuration
     */
    public UndertowThreadPoolMetricsReader(
            @NonNull ExporterManager exporterManager,
            @NonNull MetricInfo metricInfo) {
        super(exporterManager, metricInfo);
    }

    @Override
    public void init() throws Exception {
        getMetricInfo().checkHasTargetParameter();
        serverId = getMetricInfo().getTarget();
    }

    @Override
    public void start() {
        try {
            TowServer towServer = getExporterManager().getBean(serverId);
            Undertow undertow = towServer.getUndertow();
            metrics = undertow.getWorker().getMXBean();
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve Undertow server with " + getMetricInfo().getTarget(), e);
        }
    }

    @Override
    public void stop() {
        if (metrics != null) {
            metrics = null;
        }
    }

    @Override
    public MetricData getMetricData(boolean greater) {
        if (metrics == null) {
            return null;
        }

        int active = metrics.getBusyWorkerThreadCount();
        if (greater && active <= oldActive) {
            return null;
        }

        oldActive = active;
        return buildMetricData(active);
    }

    @Override
    public MetricData getMetricDataIfChanged() {
        if (metrics == null) {
            return null;
        }

        int active = metrics.getBusyWorkerThreadCount();
        if (active == oldActive) {
            return null;
        }

        oldActive = active;
        return buildMetricData(active);
    }

    @Override
    public boolean hasChanges() {
        if (metrics == null) {
            return false;
        }
        return (metrics.getBusyWorkerThreadCount() != oldActive);
    }

    private MetricData buildMetricData(int active) {
        int total = metrics.getWorkerPoolSize();
        int max = metrics.getMaxWorkerPoolSize();

        return new MetricData(getMetricInfo())
                .setFormat("{active}/{total}")
                .putData("workerName", metrics.getName())
                .putData("active", active)
                .putData("total", total)
                .putData("max", max);
    }

}

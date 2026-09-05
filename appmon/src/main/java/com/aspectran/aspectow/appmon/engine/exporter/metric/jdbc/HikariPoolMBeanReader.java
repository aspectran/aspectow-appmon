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
package com.aspectran.aspectow.appmon.engine.exporter.metric.jdbc;

import com.aspectran.aspectow.appmon.engine.config.MetricInfo;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.engine.exporter.metric.AbstractMetricReader;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricData;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricReader;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.jspecify.annotations.NonNull;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * A {@link MetricReader} for monitoring a HikariCP connection pool via JMX.
 * It connects to the {@link com.zaxxer.hikari.HikariPoolMXBean} to read connection statistics.
 *
 * <p>Collected metric data points:</p>
 * <ul>
 *   <li>{@code poolName} - The configured pool name.</li>
 *   <li>{@code active} - Number of currently active (in-use) connections.</li>
 *   <li>{@code idle} - Number of idle connections in the pool.</li>
 *   <li>{@code total} - Total number of connections in the pool.</li>
 *   <li>{@code threadsAwaitingConnection} - Number of threads currently awaiting a connection.</li>
 * </ul>
 *
 * <p>Created: 2025-06-02</p>
 */
public class HikariPoolMBeanReader extends AbstractMetricReader {

    private String poolName;

    private HikariPoolMXBean hikariPoolMXBean;

    private int oldUsed;

    /**
     * Instantiates a new HikariPoolMBeanReader.
     * @param exporterManager the exporter manager
     * @param metricInfo the metric configuration
     */
    public HikariPoolMBeanReader(
            @NonNull ExporterManager exporterManager,
            @NonNull MetricInfo metricInfo) {
        super(exporterManager, metricInfo);
    }

    @Override
    public void init() throws Exception {
        if (!getMetricInfo().hasParameters() || !getMetricInfo().getParameters().hasValue("poolName")) {
            throw new IllegalArgumentException("Missing value of required parameter: poolName");
        }
        poolName = getMetricInfo().getParameters().getString("poolName");
    }

    @Override
    public void start() throws Exception {
        ObjectName objectName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        hikariPoolMXBean = JMX.newMBeanProxy(mBeanServer, objectName, HikariPoolMXBean.class);
    }

    @Override
    public void stop() {
        if (hikariPoolMXBean != null) {
            hikariPoolMXBean = null;
        }
    }

    @Override
    public MetricData getMetricData(boolean greater) {
        if (hikariPoolMXBean == null) {
            return null;
        }

        int total = hikariPoolMXBean.getTotalConnections();
        int idle = hikariPoolMXBean.getIdleConnections();
        int used = total - idle;
        if (greater && used <= oldUsed) {
            return null;
        }

        oldUsed = used;
        return buildMetricData(total, idle, used);
    }

    @Override
    public MetricData getMetricDataIfChanged() {
        if (hikariPoolMXBean == null) {
            return null;
        }

        int total = hikariPoolMXBean.getTotalConnections();
        int idle = hikariPoolMXBean.getIdleConnections();
        int used = total - idle;
        if (used == oldUsed) {
            return null;
        }

        oldUsed = used;
        return buildMetricData(total, idle, used);
    }

    @Override
    public boolean hasChanges() {
        if (hikariPoolMXBean == null) {
            return false;
        }
        int total = hikariPoolMXBean.getTotalConnections();
        int idle = hikariPoolMXBean.getIdleConnections();
        int used = total - idle;
        return (used != oldUsed);
    }

    private MetricData buildMetricData(int total, int idle, int used) {
        int active = hikariPoolMXBean.getActiveConnections();
        int awaiting = hikariPoolMXBean.getThreadsAwaitingConnection();

        return new MetricData(getMetricInfo())
                .setFormat("{used}/{total}")
                .putData("poolName", poolName)
                .putData("total", total)
                .putData("active", active)
                .putData("idle", idle)
                .putData("awaiting", awaiting)
                .putData("used", used);
    }

}

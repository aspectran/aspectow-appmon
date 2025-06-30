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
package com.aspectran.appmon.exporter.event.jmx.jdbc;

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.appmon.exporter.event.jmx.AbstractMBeanReader;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.json.JsonBuilder;
import com.zaxxer.hikari.HikariPoolMXBean;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * <p>Created: 2025-06-02</p>
 */
public class HikariPoolMBeanReader extends AbstractMBeanReader {

    private String poolName;

    private HikariPoolMXBean hikariPoolMXBean;

    private int oldActive = -1;

    private int oldIdle;

    private int oldAwaiting;

    public HikariPoolMBeanReader(
            @NonNull ExporterManager exporterManager,
            @NonNull EventInfo eventInfo) {
        super(exporterManager, eventInfo);
    }

    @Override
    public void init() throws Exception {
        if (!getEventInfo().hasParameters() || !getEventInfo().getParameters().hasValue("poolName")) {
            throw new IllegalArgumentException("Missing value of required parameter: poolName");
        }
        poolName = getEventInfo().getParameters().getString("poolName");
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
    public String read() {
        if (hikariPoolMXBean == null) {
            return null;
        }
        int total = hikariPoolMXBean.getTotalConnections();
        int active = hikariPoolMXBean.getActiveConnections();
        int idle = hikariPoolMXBean.getIdleConnections();
        int awaiting = hikariPoolMXBean.getThreadsAwaitingConnection();
        int used = total - idle;
        String value = used + "/" + total;

        return new JsonBuilder()
                .prettyPrint(false)
                .nullWritable(false)
                .object()
                    .put("name", getEventInfo().getName())
                    .put("title", getEventInfo().getTitle())
                    .put("value", value)
                    .object("data")
                        .put("poolName", poolName)
                        .put("total", total)
                        .put("active", active)
                        .put("idle", idle)
                        .put("awaiting", awaiting)
                    .endObject()
                .endObject()
                .toString();
    }

    @Override
    public String readIfChanged() {
        if (hikariPoolMXBean == null) {
            return null;
        }
        boolean changed = (hikariPoolMXBean.getActiveConnections() != oldActive ||
                hikariPoolMXBean.getIdleConnections() != oldIdle ||
                hikariPoolMXBean.getThreadsAwaitingConnection() != oldAwaiting);
        if (changed) {
            oldActive = hikariPoolMXBean.getActiveConnections();
            oldIdle = hikariPoolMXBean.getIdleConnections();
            oldAwaiting = hikariPoolMXBean.getThreadsAwaitingConnection();
            return read();
        } else {
            return null;
        }
    }

}

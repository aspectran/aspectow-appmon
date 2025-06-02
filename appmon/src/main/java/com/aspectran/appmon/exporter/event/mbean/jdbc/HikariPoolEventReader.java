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
package com.aspectran.appmon.exporter.event.mbean.jdbc;

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.appmon.exporter.event.mbean.AbstractMBeanEventReader;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.service.CoreServiceHolder;
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
public class HikariPoolEventReader extends AbstractMBeanEventReader {

    private final String poolName;

    private MBeanServer mBeanServer;

    private HikariPoolMXBean hikariPoolMXBean;

    private int oldActiveConnections;

    private int oldIdleConnections;

    private int oldThreadsAwaitingConnection;

    public HikariPoolEventReader(
            @NonNull ExporterManager exporterManager,
            @NonNull EventInfo eventInfo) {
        super(exporterManager, eventInfo);
        this.poolName = (eventInfo.hasParameters() ?
                eventInfo.getParameters().getString("poolName") : eventInfo.getTarget());
    }

    @Override
    public void start() throws Exception {
        ActivityContext context = CoreServiceHolder.findActivityContext(getEventInfo().getTarget());
        if (context == null) {
            throw new Exception("Could not find ActivityContext named '" + getEventInfo().getTarget() + "'");
        }

        ObjectName objectName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
        hikariPoolMXBean = JMX.newMBeanProxy(mBeanServer, objectName, HikariPoolMXBean.class);
    }

    @Override
    public void stop() {
        if (mBeanServer != null) {
            mBeanServer = null;
            hikariPoolMXBean = null;
        }
    }

    @Override
    public String read() {
        if (hikariPoolMXBean == null) {
            return null;
        }
        return new JsonBuilder()
                .prettyPrint(false)
                .nullWritable(false)
                .object()
                    .put("poolName", poolName)
                    .put("total", hikariPoolMXBean.getTotalConnections())
                    .put("active", hikariPoolMXBean.getActiveConnections())
                    .put("idle", hikariPoolMXBean.getIdleConnections())
                    .put("awaiting", hikariPoolMXBean.getThreadsAwaitingConnection())
                .endObject()
                .toString();
    }

    @Override
    public String readIfChanged() {
        if (hikariPoolMXBean == null) {
            return null;
        }
        boolean changed = (hikariPoolMXBean.getActiveConnections() != oldActiveConnections ||
                hikariPoolMXBean.getIdleConnections() != oldIdleConnections ||
                hikariPoolMXBean.getThreadsAwaitingConnection() != oldThreadsAwaitingConnection);
        if (changed) {
            oldActiveConnections = hikariPoolMXBean.getActiveConnections();
            oldIdleConnections = hikariPoolMXBean.getIdleConnections();
            oldThreadsAwaitingConnection = hikariPoolMXBean.getThreadsAwaitingConnection();
            return read();
        } else {
            return null;
        }
    }

}

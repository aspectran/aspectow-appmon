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
package com.aspectran.aspectow.appmon.engine.exporter.metric.netty;

import com.aspectran.aspectow.appmon.engine.config.MetricInfo;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterType;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricData;
import com.aspectran.core.component.session.SessionManager;
import com.aspectran.netty.server.NettyContextRouter;
import com.aspectran.netty.server.NettyServer;
import com.aspectran.utils.lifecycle.AbstractLifeCycle;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NettyThreadPoolMetricsReaderTest {

    @Test
    void testNettyThreadPoolMetricsReader() throws Exception {
        TestNettyServer nettyServer = new TestNettyServer();
        nettyServer.start();

        ExporterManager exporterManager = new ExporterManager(ExporterType.METRIC, null, "testApp") {
            @Override
            @SuppressWarnings("unchecked")
            public <V> V getBean(@NonNull String id) {
                if ("netty.server".equals(id)) {
                    return (V) nettyServer;
                }
                return null;
            }
        };

        MetricInfo metricInfo = new MetricInfo();
        metricInfo.putValue("id", "netty-tp");
        metricInfo.putValue("target", "netty.server");

        NettyThreadPoolMetricsReader reader = new NettyThreadPoolMetricsReader(exporterManager, metricInfo);
        reader.init();
        reader.start();

        assertTrue(reader.hasChanges());

        MetricData data = reader.getMetricData(false);
        assertNotNull(data);
        assertTrue(data.toJson().contains("\"format\":\"{active}/{total} (VT)\""));
        assertEquals("netty-test", data.getData("workerName"));
        assertEquals(3, data.getData("active"));
        assertEquals(5, data.getData("total"));
        assertEquals(-1, data.getData("max"));
        assertEquals(8, data.getData("workerThreads"));
        assertEquals(1, data.getData("bossThreads"));
        assertEquals(true, data.getData("virtualThreads"));

        // When greater is true and active hasn't increased, returns null
        assertNull(reader.getMetricData(true));

        assertFalse(reader.hasChanges());

        reader.stop();
        assertNull(reader.getMetricData(false));
    }

    @Test
    void testPlatformThreadsFormat() throws Exception {
        TestNettyServer nettyServer = new TestNettyServer(false);
        nettyServer.start();

        ExporterManager exporterManager = new ExporterManager(ExporterType.METRIC, null, "testApp") {
            @Override
            @SuppressWarnings("unchecked")
            public <V> V getBean(@NonNull String id) {
                if ("netty.server".equals(id)) {
                    return (V) nettyServer;
                }
                return null;
            }
        };

        MetricInfo metricInfo = new MetricInfo();
        metricInfo.putValue("id", "netty-tp");
        metricInfo.putValue("target", "netty.server");

        NettyThreadPoolMetricsReader reader = new NettyThreadPoolMetricsReader(exporterManager, metricInfo);
        reader.init();
        reader.start();

        MetricData data = reader.getMetricData(false);
        assertNotNull(data);
        assertTrue(data.toJson().contains("\"format\":\"{active}/{total}\""));
        assertEquals(false, data.getData("virtualThreads"));

        reader.stop();
        reader.stop();
    }

    private static class TestNettyServer extends AbstractLifeCycle implements NettyServer {

        private final boolean virtualThreads;

        TestNettyServer() {
            this(true);
        }

        TestNettyServer(boolean virtualThreads) {
            this.virtualThreads = virtualThreads;
        }

        @Override
        public String getWorkerName() {
            return "netty-test";
        }

        @Override
        public EventLoopGroup getBossGroup() {
            return null;
        }

        @Override
        public EventLoopGroup getWorkerGroup() {
            return null;
        }

        @Override
        public int getBossThreads() {
            return 1;
        }

        @Override
        public int getWorkerThreads() {
            return 8;
        }

        @Override
        public ExecutorService getRequestExecutor() {
            return null;
        }

        @Override
        public ThreadPoolExecutor getThreadPoolExecutor() {
            return null;
        }

        @Override
        public boolean isVirtualThreads() {
            return virtualThreads;
        }

        @Override
        public int getActiveRequests() {
            return 3;
        }

        @Override
        public int getPeakRequests() {
            return 5;
        }

        @Override
        public long getTotalRequests() {
            return 10;
        }

        @Override
        public List<Channel> getActiveChannels() {
            return Collections.emptyList();
        }

        @Override
        public int getActivePort() {
            return 8080;
        }

        @Override
        public int getActivePort(int index) {
            return 8080;
        }

        @Override
        public NettyContextRouter getContextRouter() {
            return null;
        }

        @Override
        public SessionManager getSessionManager() {
            return null;
        }

        @Override
        public SessionManager getSessionManager(String name) {
            return null;
        }

        @Override
        public SessionManager getSessionManagerByPath(String path) {
            return null;
        }

        @Override
        protected void doStart() {}

        @Override
        protected void doStop() {}

    }

}

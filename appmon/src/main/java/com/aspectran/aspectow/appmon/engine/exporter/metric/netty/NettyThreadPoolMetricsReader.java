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
import com.aspectran.aspectow.appmon.engine.exporter.metric.AbstractMetricReader;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricData;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricReader;
import com.aspectran.netty.server.NettyServer;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * A {@link MetricReader} for monitoring Netty server thread pool and worker resources.
 *
 * <p>Created: 2026-09-04</p>
 */
public class NettyThreadPoolMetricsReader extends AbstractMetricReader {

    private String serverId;

    private NettyServer nettyServer;

    private int oldActive;

    /**
     * Instantiates a new NettyThreadPoolMetricsReader.
     * @param exporterManager the exporter manager
     * @param metricInfo the metric configuration
     */
    public NettyThreadPoolMetricsReader(
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
            nettyServer = getExporterManager().getBean(serverId);
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve Netty server with " + getMetricInfo().getTarget(), e);
        }
    }

    @Override
    public void stop() {
        if (nettyServer != null) {
            nettyServer = null;
        }
    }

    @Override
    public MetricData getMetricData(boolean greater) {
        if (nettyServer == null) {
            return null;
        }

        int active = nettyServer.getActiveRequests();
        if (greater && active <= oldActive) {
            return null;
        }

        oldActive = active;

        int total;
        int max;
        int queued = 0;
        long completed = -1;

        ThreadPoolExecutor tpe = nettyServer.getThreadPoolExecutor();
        if (tpe != null) {
            total = tpe.getPoolSize();
            max = tpe.getMaximumPoolSize();
            queued = tpe.getQueue().size();
            completed = tpe.getCompletedTaskCount();
        } else if (nettyServer.isVirtualThreads()) {
            total = nettyServer.getPeakRequests();
            max = -1;
            completed = nettyServer.getTotalRequests() - active;
        } else {
            total = getWorkerThreads();
            max = total;
        }

        String format = (nettyServer.isVirtualThreads() ? "{active}/{total} (VT)" : "{active}/{total}");
        MetricData metricData = new MetricData(getMetricInfo())
                .setFormat(format)
                .putData("workerName", nettyServer.getWorkerName())
                .putData("active", active)
                .putData("total", total)
                .putData("max", max)
                .putData("workerThreads", getWorkerThreads())
                .putData("bossThreads", nettyServer.getBossThreads())
                .putData("virtualThreads", nettyServer.isVirtualThreads());
        if (queued > 0) {
            metricData.putData("queued", queued);
        }
        if (completed >= 0) {
            metricData.putData("completed", completed);
        }
        return metricData;
    }

    @Override
    public boolean hasChanges() {
        if (nettyServer == null) {
            return false;
        }
        return (nettyServer.getActiveRequests() != oldActive);
    }

    private int getWorkerThreads() {
        if (nettyServer == null) {
            return 0;
        }
        EventLoopGroup workerGroup = nettyServer.getWorkerGroup();
        if (workerGroup instanceof MultithreadEventExecutorGroup group) {
            return group.executorCount();
        }
        return nettyServer.getWorkerThreads();
    }

}

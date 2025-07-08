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
package com.aspectran.appmon.exporter.metric;

import com.aspectran.appmon.config.MetricInfo;
import com.aspectran.appmon.exporter.AbstractExporter;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.appmon.service.CommandOptions;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>Created: 2024-12-18</p>
 */
public class MetricExporter extends AbstractExporter {

    private final ExporterManager exporterManager;

    private final MetricInfo metricInfo;

    private final MetricReader metricReader;

    private final String prefix;

    private final int sampleInterval;

    private Timer timer;

    public MetricExporter(@NonNull ExporterManager exporterManager,
                          @NonNull MetricInfo metricInfo,
                          @NonNull MetricReader metricReader) {
        super(metricReader.getType());
        this.exporterManager = exporterManager;
        this.metricInfo = metricInfo;
        this.metricReader = metricReader;
        this.prefix = metricInfo.getInstanceName() + ":" + getType() + ":" + metricInfo.getName() + ":";
        this.sampleInterval = metricInfo.getSampleInterval();
    }

    @Override
    public String getName() {
        return metricInfo.getName();
    }

    @Override
    public void read(@NonNull List<String> messages, CommandOptions commandOptions) {
        String json = metricReader.read();
        if (json != null) {
            messages.add(prefix + json);
        }
    }

    @Override
    public void readIfChanged(@NonNull List<String> messages, CommandOptions commandOptions) {
        String json = metricReader.readIfChanged();
        if (json != null) {
            messages.add(prefix + json);
        }
    }

    @Override
    public void broadcast(String message) {
        exporterManager.broadcast(prefix + message);
    }

    private void broadcastIfChanged() {
        String data = metricReader.readIfChanged();
        if (data != null) {
            broadcast(data);
        }
    }

    @Override
    protected void doStart() throws Exception {
        if (sampleInterval > 0) {
            metricReader.start();
            if (timer == null) {
                String name = new ToStringBuilder("MetricReadingTimer")
                        .append("metricReader", metricReader)
                        .append("sampleInterval", sampleInterval)
                        .toString();
                timer = new Timer(name);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        broadcastIfChanged();
                    }
                }, 0, sampleInterval);
            }
        } else {
            metricReader.start();
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        metricReader.stop();
    }

    @Override
    public String toString() {
        if (isStopped()) {
            return ToStringBuilder.toString(super.toString(), metricInfo);
        } else {
            return super.toString();
        }
    }

}

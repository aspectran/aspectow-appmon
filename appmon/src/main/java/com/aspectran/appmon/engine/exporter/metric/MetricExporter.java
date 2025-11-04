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
package com.aspectran.appmon.engine.exporter.metric;

import com.aspectran.appmon.engine.config.MetricInfo;
import com.aspectran.appmon.engine.exporter.AbstractExporter;
import com.aspectran.appmon.engine.exporter.ExporterManager;
import com.aspectran.appmon.engine.service.CommandOptions;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.List;

/**
 * An exporter for collecting and broadcasting metric data.
 * It uses a {@link MetricReader} to read data and can periodically sample and export it.
 *
 * <p>Created: 2024-12-18</p>
 */
public class MetricExporter extends AbstractExporter {

    private final ExporterManager exporterManager;

    private final MetricInfo metricInfo;

    private final MetricReader metricReader;

    private final String prefix;

    private final int sampleInterval;

    private final int exportInterval;

    private MetricExportTimer timer;

    /**
     * Instantiates a new MetricExporter.
     * @param exporterManager the exporter manager
     * @param metricInfo the metric configuration
     * @param metricReader the reader for the metric data
     */
    public MetricExporter(@NonNull ExporterManager exporterManager,
                          @NonNull MetricInfo metricInfo,
                          @NonNull MetricReader metricReader) {
        super(metricReader.getType());
        this.exporterManager = exporterManager;
        this.metricInfo = metricInfo;
        this.metricReader = metricReader;
        this.prefix = metricInfo.getInstanceName() + ":" + getType() + ":" + metricInfo.getName() + ":";

        int sampleInterval = metricInfo.getSampleInterval();
        int exportInterval = metricInfo.getExportInterval();
        if (sampleInterval <= 0 && exportInterval <= 0) {
            this.sampleInterval = 0;
            this.exportInterval = 0;
        } else if (sampleInterval > exportInterval) {
            this.sampleInterval = sampleInterval;
            this.exportInterval = 0;
        } else {
            this.sampleInterval = sampleInterval;
            this.exportInterval = exportInterval;
        }
    }

    @Override
    public String getName() {
        return metricInfo.getName();
    }

    /**
     * Gets the metric reader used by this exporter.
     * @return the metric reader
     */
    public MetricReader getMetricReader() {
        return metricReader;
    }

    @Override
    public void read(@NonNull List<String> messages, CommandOptions commandOptions) {
        MetricData metricData = metricReader.getMetricData();
        if (metricData != null) {
            String json = metricData.toJson();
            if (json != null) {
                messages.add(prefix + json);
            }
        }
    }

    @Override
    public void readIfChanged(@NonNull List<String> messages, CommandOptions commandOptions) {
        if (metricReader.hasChanges()) {
            read(messages, commandOptions);
        }
    }

    @Override
    public void broadcast(String message) {
        if (message != null) {
            exporterManager.broadcast(prefix + message);
        }
    }

    @Override
    protected void doStart() throws Exception {
        metricReader.start();
        if (sampleInterval > 0) {
            timer = new MetricExportTimer(exporterManager.getScheduler(), this);
            timer.schedule(sampleInterval, exportInterval);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (timer != null) {
            timer.destroy();
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

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
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.utils.annotation.jsr305.NonNull;

/**
 * <p>Created: 2025. 1. 27.</p>
 */
public abstract class AbstractMetricReader implements MetricReader {

    private final ExporterManager exporterManager;

    private final MetricInfo metricInfo;

    private volatile MetricExporter metricExporter;

    public AbstractMetricReader(@NonNull ExporterManager exporterManager,
                                @NonNull MetricInfo metricInfo) {
        this.exporterManager = exporterManager;
        this.metricInfo = metricInfo;
    }

    public ExporterManager getExporterManager() {
        return exporterManager;
    }

    protected MetricExporter getMetricExporter() {
        if (metricExporter == null) {
            synchronized (this) {
                if (metricExporter == null) {
                    metricExporter = exporterManager.getExporter(getMetricInfo().getName());
                }
            }
        }
        return metricExporter;
    }

    public MetricInfo getMetricInfo() {
        return metricInfo;
    }

}

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
 * Abstract base class for {@link MetricReader} implementations.
 * Provides common functionalities for metric readers.
 *
 * <p>Created: 2025. 1. 27.</p>
 */
public abstract class AbstractMetricReader implements MetricReader {

    private final ExporterManager exporterManager;

    private final MetricInfo metricInfo;

    private volatile MetricExporter metricExporter;

    /**
     * Instantiates a new AbstractMetricReader.
     * @param exporterManager the exporter manager
     * @param metricInfo the metric configuration
     */
    public AbstractMetricReader(@NonNull ExporterManager exporterManager,
                                @NonNull MetricInfo metricInfo) {
        this.exporterManager = exporterManager;
        this.metricInfo = metricInfo;
    }

    /**
     * Gets the exporter manager.
     * @return the exporter manager
     */
    public ExporterManager getExporterManager() {
        return exporterManager;
    }

    /**
     * Gets the associated metric exporter.
     * @return the metric exporter
     */
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

    /**
     * Gets the metric configuration.
     * @return the metric info
     */
    public MetricInfo getMetricInfo() {
        return metricInfo;
    }

}

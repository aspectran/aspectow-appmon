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
import com.aspectran.utils.ClassUtils;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder for creating {@link MetricExporter} instances.
 * It determines the appropriate {@link MetricReader} based on the configuration and constructs the exporter.
 *
 * <p>Created: 2024-12-18</p>
 */
public abstract class MetricExporterBuilder {

    private static final Logger logger = LoggerFactory.getLogger(MetricExporterBuilder.class);

    /**
     * Builds a new {@link MetricExporter}.
     * @param exporterManager the exporter manager
     * @param metricInfo the metric configuration
     * @return a new {@link MetricExporter} instance
     * @throws Exception if the exporter cannot be built
     */
    @NonNull
    public static MetricExporter build(
            @NonNull ExporterManager exporterManager,
            @NonNull MetricInfo metricInfo) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(ToStringBuilder.toString("Create MetricExporter", metricInfo));
        }
        MetricReader metricReader = createMetricReader(exporterManager, metricInfo);
        metricReader.init();
        return new MetricExporter(exporterManager, metricInfo, metricReader);
    }

    @NonNull
    private static MetricReader createMetricReader(
            @NonNull ExporterManager exporterManager,
            @NonNull MetricInfo metricInfo) throws Exception {
        if (!metricInfo.hasReader()) {
            throw new IllegalArgumentException("No metric reader specified for " + metricInfo.getName() + " " + metricInfo);
        }
        try {
            Class<MetricReader> readerType = ClassUtils.classForName(metricInfo.getReader());
            Object[] args = { exporterManager, metricInfo };
            Class<?>[] argTypes = { ExporterManager.class, MetricInfo.class };
            return ClassUtils.createInstance(readerType, args, argTypes);
        } catch (Exception e) {
            throw new Exception(ToStringBuilder.toString("Failed to create metric reader", metricInfo), e);
        }
    }

}

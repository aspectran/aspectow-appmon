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
package com.aspectran.appmon.engine.exporter.event;

import com.aspectran.appmon.engine.config.EventInfo;
import com.aspectran.appmon.engine.exporter.ExporterManager;
import com.aspectran.utils.ToStringBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder for creating {@link ChartDataExporter} instances.
 *
 * <p>Created: 2024-12-18</p>
 */
public abstract class ChartDataExporterBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ChartDataExporterBuilder.class);

    /**
     * Builds a new {@link ChartDataExporter}.
     * @param exporterManager the exporter manager
     * @param eventInfo the event configuration
     * @return a new {@link ChartDataExporter} instance
     * @throws Exception if the exporter cannot be built
     */
    @NonNull
    public static ChartDataExporter build(
            @NonNull ExporterManager exporterManager,
            @NonNull EventInfo eventInfo) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(ToStringBuilder.toString("Create ChartDataExporter", eventInfo));
        }
        return new ChartDataExporter(exporterManager, eventInfo);
    }

}

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

import com.aspectran.appmon.exporter.ExporterType;

/**
 * An interface for reading metric data from a specific source.
 * Implementations of this interface are responsible for collecting specific metrics
 * like memory usage, connection pool status, etc.
 *
 * <p>Created: 2024-12-18</p>
 */
public interface MetricReader {

    /**
     * Returns the type of the exporter, which is always {@link ExporterType#METRIC}.
     * @return the exporter type
     */
    default ExporterType getType() {
        return ExporterType.METRIC;
    }

    /**
     * Initializes the metric reader.
     * @throws Exception if initialization fails
     */
    default void init() throws Exception {
    }

    /**
     * Starts the metric reader.
     * @throws Exception if starting fails
     */
    void start() throws Exception;

    /**
     * Stops the metric reader and releases any resources.
     */
    void stop();

    /**
     * Gets the current metric data.
     * @return a {@link MetricData} object, or {@code null} if no data is available
     */
    default MetricData getMetricData() {
        return getMetricData(false);
    }

    /**
     * Gets the metric data, optionally only if a key value has increased.
     * @param greater if {@code true}, return data only if a monitored value is greater than the last read
     * @return a {@link MetricData} object, or {@code null} if no new data is available
     */
    MetricData getMetricData(boolean greater);

    /**
     * Checks if the metric data has changed since the last read.
     * @return {@code true} if the data has changed, {@code false} otherwise
     */
    boolean hasChanges();

}

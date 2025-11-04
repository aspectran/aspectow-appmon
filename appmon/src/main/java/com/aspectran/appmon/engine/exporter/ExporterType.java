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
package com.aspectran.appmon.engine.exporter;

import com.aspectran.utils.annotation.jsr305.Nullable;

/**
 * An enumeration for the types of exporters.
 * This helps in categorizing and managing different data sources like events, logs, and metrics.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public enum ExporterType {

    /** For chart data */
    DATA("data"),

    /** For general events */
    EVENT("event"),

    /** For log tailing */
    LOG("log"),

    /** For performance metrics */
    METRIC("metric");

    private final String alias;

    ExporterType(String alias) {
        this.alias = alias;
    }

    @Override
    public String toString() {
        return this.alias;
    }

    /**
     * Returns an {@code ExporterType} with a value represented
     * by the specified {@code String}.
     * @param alias the exporter type as a {@code String}
     * @return an {@code ExporterType}, may be {@code null}
     */
    @Nullable
    public static ExporterType resolve(String alias) {
        for (ExporterType type : values()) {
            if (type.alias.equals(alias)) {
                return type;
            }
        }
        return null;
    }

}

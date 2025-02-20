/*
 * Copyright (c) 2020-2025 The Aspectran Project
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
package com.aspectran.appmon.exporter;

import com.aspectran.appmon.service.ExportServiceManager;
import com.aspectran.utils.annotation.jsr305.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Created: 2024-12-18</p>
 */
public abstract class ExporterManager {

    private static final Logger logger = LoggerFactory.getLogger(ExporterManager.class);

    private final Map<String, Exporter> exporters = new LinkedHashMap<>();

    private final ExportServiceManager exportServiceManager;

    private final String instanceName;

    public ExporterManager(ExportServiceManager exportServiceManager, String instanceName) {
        this.exportServiceManager = exportServiceManager;
        this.instanceName = instanceName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void addExporter(Exporter exporter) {
        exporters.put(exporter.getName(), exporter);
    }

    @SuppressWarnings("unchecked")
    public <V extends Exporter> V getExporter(String name) {
        Exporter exporter = exporters.get(name);
        if (exporter == null) {
            throw new IllegalArgumentException("No exporter found for name '" + name + "'");
        }
        return (V)exporter;
    }

    public void collectMessages(List<String> messages) {
        for (Exporter exporter : exporters.values()) {
            exporter.read(messages);
        }
    }

    public void start() {
        for (Exporter exporter : exporters.values()) {
            try {
                exporter.start();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public void stop() {
        for (Exporter exporter : exporters.values()) {
            try {
                exporter.stop();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public void broadcast(String message) {
        exportServiceManager.broadcast(message);
    }

    public <V> V getBean(@NonNull String id) {
        return exportServiceManager.getAppMonManager().getBean(id);
    }

    public <V> V getBean(Class<V> type) {
        return exportServiceManager.getAppMonManager().getBean(type);
    }

}

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
package com.aspectran.appmon.exporter;

import com.aspectran.appmon.manager.AppMonManager;
import com.aspectran.appmon.service.CommandOptions;
import com.aspectran.core.activity.InstantAction;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.thread.ScheduledExecutorScheduler;
import com.aspectran.utils.thread.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Created: 2024-12-18</p>
 */
public class ExporterManager {

    private static final Logger logger = LoggerFactory.getLogger(ExporterManager.class);

    private final Map<String, Exporter> exporters = new LinkedHashMap<>();

    private final ExporterType exporterType;

    private final AppMonManager appMonManager;

    private final String instanceName;

    private Scheduler scheduler;

    public ExporterManager(ExporterType exporterType, AppMonManager appMonManager, String instanceName) {
        this.exporterType = exporterType;
        this.appMonManager = appMonManager;
        this.instanceName = instanceName;
    }

    public AppMonManager getAppMonManager() {
        return appMonManager;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void addExporter(Exporter exporter) {
        exporters.put(exporter.getName(), exporter);
    }

    @SuppressWarnings("unchecked")
    public <V extends Exporter> V getExporter(String name) {
        Exporter exporter = exporters.get(name);
        if (exporter == null) {
            throw new IllegalArgumentException("No exporter named '" + name + "' found");
        }
        return (V)exporter;
    }

    public void collectMessages(List<String> messages, CommandOptions commandOptions) {
        for (Exporter exporter : exporters.values()) {
            exporter.read(messages, commandOptions);
        }
    }

    public void collectNewMessages(List<String> messages, CommandOptions commandOptions) {
        for (Exporter exporter : exporters.values()) {
            exporter.readIfChanged(messages, commandOptions);
        }
    }

    public synchronized void start() {
        scheduler = new ScheduledExecutorScheduler(exporterType + "ExportScheduler", false);
        scheduler.start();
        for (Exporter exporter : exporters.values()) {
            try {
                exporter.start();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public synchronized void stop() {
        for (Exporter exporter : exporters.values()) {
            try {
                exporter.stop();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
    }

    public void broadcast(String message) {
        appMonManager.getExportServiceManager().broadcast(message);
    }

    public <V> V instantActivity(InstantAction<V> instantAction) {
        return appMonManager.instantActivity(instantAction);
    }

    public <V> V getBean(@NonNull String id) {
        return appMonManager.getBean(id);
    }

    public <V> V getBean(Class<V> type) {
        return appMonManager.getBean(type);
    }

    public boolean containsBean(Class<?> type) {
        return appMonManager.containsBean(type);
    }

}

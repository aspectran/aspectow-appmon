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

import com.aspectran.appmon.engine.manager.AppMonManager;
import com.aspectran.appmon.engine.service.CommandOptions;
import com.aspectran.core.activity.InstantAction;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.scheduling.ScheduledExecutorScheduler;
import com.aspectran.utils.scheduling.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages a collection of {@link Exporter} instances for a specific type and instance.
 * It is responsible for the lifecycle of the exporters and for collecting data from them.
 *
 * <p>Created: 2024-12-18</p>
 */
public class ExporterManager {

    private static final Logger logger = LoggerFactory.getLogger(ExporterManager.class);

    private final Map<String, Exporter> exporters = new LinkedHashMap<>();

    private final ExporterType exporterType;

    private final AppMonManager appMonManager;

    private final String instanceName;

    private Scheduler scheduler;

    /**
     * Instantiates a new ExporterManager.
     * @param exporterType the type of exporters to manage
     * @param appMonManager the main application manager
     * @param instanceName the name of the instance this manager belongs to
     */
    public ExporterManager(ExporterType exporterType, AppMonManager appMonManager, String instanceName) {
        this.exporterType = exporterType;
        this.appMonManager = appMonManager;
        this.instanceName = instanceName;
    }

    /**
     * Gets the main application manager.
     * @return the {@link AppMonManager}
     */
    public AppMonManager getAppMonManager() {
        return appMonManager;
    }

    /**
     * Gets the name of the instance this manager belongs to.
     * @return the instance name
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Gets the scheduler used for export tasks.
     * @return the {@link Scheduler}
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Adds an exporter to this manager.
     * @param exporter the exporter to add
     */
    public void addExporter(Exporter exporter) {
        exporters.put(exporter.getName(), exporter);
    }

    /**
     * Gets an exporter by its name.
     * @param name the name of the exporter
     * @param <V> the type of the exporter
     * @return the exporter instance
     * @throws IllegalArgumentException if no exporter with the given name is found
     */
    @SuppressWarnings("unchecked")
    public <V extends Exporter> V getExporter(String name) {
        Exporter exporter = exporters.get(name);
        if (exporter == null) {
            throw new IllegalArgumentException("No exporter named '" + name + "' found");
        }
        return (V)exporter;
    }

    /**
     * Collects messages from all managed exporters.
     * @param messages a list to which the collected messages will be added
     * @param commandOptions options for the command
     */
    public void collectMessages(List<String> messages, CommandOptions commandOptions) {
        for (Exporter exporter : exporters.values()) {
            exporter.read(messages, commandOptions);
        }
    }

    /**
     * Collects new or changed messages from all managed exporters.
     * @param messages a list to which the collected messages will be added
     * @param commandOptions options for the command
     */
    public void collectNewMessages(List<String> messages, CommandOptions commandOptions) {
        for (Exporter exporter : exporters.values()) {
            exporter.readIfChanged(messages, commandOptions);
        }
    }

    /**
     * Starts this manager and all its managed exporters.
     */
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

    /**
     * Stops this manager and all its managed exporters.
     */
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

    /**
     * Broadcasts a message using the application's export service.
     * @param message the message to broadcast
     */
    public void broadcast(String message) {
        appMonManager.getExportServiceManager().broadcast(message);
    }

    /**
     * Executes an instant action within the ActivityContext.
     * @param instantAction the action to execute
     * @param <V> the type of the result
     * @return the result of the action
     */
    public <V> V instantActivity(InstantAction<V> instantAction) {
        return appMonManager.instantActivity(instantAction);
    }

    /**
     * Gets a bean from the ActivityContext by its ID.
     * @param id the ID of the bean
     * @param <V> the type of the bean
     * @return the bean instance
     */
    public <V> V getBean(@NonNull String id) {
        return appMonManager.getBean(id);
    }

    /**
     * Gets a bean from the ActivityContext by its type.
     * @param type the type of the bean
     * @param <V> the type of the bean
     * @return the bean instance
     */
    public <V> V getBean(Class<V> type) {
        return appMonManager.getBean(type);
    }

    /**
     * Checks if a bean of the given type exists in the ActivityContext.
     * @param type the type of the bean
     * @return {@code true} if the bean exists, {@code false} otherwise
     */
    public boolean containsBean(Class<?> type) {
        return appMonManager.containsBean(type);
    }

}

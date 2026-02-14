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
package com.aspectran.appmon.engine.service;

import com.aspectran.appmon.engine.config.InstanceInfoHolder;
import com.aspectran.appmon.engine.exporter.ExporterManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages all {@link ExportService} and {@link ExporterManager} instances.
 * This class is a central hub for handling client sessions (join/release),
 * collecting messages from exporters, and broadcasting them to clients.
 *
 * <p>Created: 2025-02-12</p>
 */
public class ExportServiceManager {

    private final Set<ExportService> exportServices = new CopyOnWriteArraySet<>();

    private final List<ExporterManager> exporterManagers = new CopyOnWriteArrayList<>();

    private final InstanceInfoHolder instanceInfoHolder;

    /**
     * Instantiates a new ExportServiceManager.
     * @param instanceInfoHolder the holder for instance information
     */
    public ExportServiceManager(InstanceInfoHolder instanceInfoHolder) {
        this.instanceInfoHolder = instanceInfoHolder;
    }

    /**
     * Adds an export service to the manager.
     * @param exportService the export service to add
     */
    public void addExportService(ExportService exportService) {
        exportServices.add(exportService);
    }

    /**
     * Removes an export service from the manager.
     * @param exportService the export service to remove
     */
    public void removeExportService(ExportService exportService) {
        exportServices.remove(exportService);
    }

    /**
     * Adds an exporter manager to this manager.
     * @param exporterManager the exporter manager to add
     */
    public void addExporterManager(ExporterManager exporterManager) {
        exporterManagers.add(exporterManager);
    }

    /**
     * Broadcasts a message to all registered export services.
     * @param message the message to broadcast
     */
    public void broadcast(String message) {
        for (ExportService exportService : exportServices) {
            exportService.broadcast(message);
        }
    }

    /**
     * Broadcasts a message to a specific session via all registered export services.
     * @param session the target service session
     * @param message the message to broadcast
     */
    public void broadcast(ServiceSession session, String message) {
        for (ExportService exportService : exportServices) {
            exportService.broadcast(session, message);
        }
    }

    /**
     * Handles a client joining to monitor instances.
     * Starts the necessary exporters for the joined instances.
     * @param session the client session that is joining
     * @return {@code true} if the join was successful, {@code false} otherwise
     */
    public synchronized boolean join(@NonNull ServiceSession session) {
        if (session.isValid()) {
            String[] instanceNames = session.getJoinedInstances();
            if (instanceNames != null && instanceNames.length > 0) {
                for (String instanceName : instanceNames) {
                    startExporters(instanceName);
                }
            } else {
                startExporters(null);
            }
            return true;
        } else {
            return false;
        }
    }

    private void startExporters(String instanceName) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (instanceName == null || exporterManager.getInstanceName().equals(instanceName)) {
                exporterManager.start();
            }
        }
    }

    /**
     * Handles a client releasing its monitoring session.
     * Stops exporters that are no longer being monitored by any client.
     * @param session the client session that is being released
     */
    public synchronized void release(ServiceSession session) {
        String[] instanceNames = getUnusedInstances(session);
        if (instanceNames != null) {
            for (String name : instanceNames) {
                stopExporters(name);
            }
        }
        session.removeJoinedInstances();
    }

    private void stopExporters(String instanceName) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (instanceName == null || exporterManager.getInstanceName().equals(instanceName)) {
                exporterManager.stop();
            }
        }
    }

    /**
     * Gets the last known messages for the instances joined by the session.
     * @param session the client session
     * @return a list of messages
     */
    public List<String> getLastMessages(@NonNull ServiceSession session) {
        CommandOptions commandOptions = new CommandOptions();
        commandOptions.setTimeZone(session.getTimeZone());
        List<String> messages = new ArrayList<>();
        if (session.isValid()) {
            String[] instanceNames = session.getJoinedInstances();
            if (instanceNames != null && instanceNames.length > 0) {
                for (String name : instanceNames) {
                    collectLastMessages(name, messages, commandOptions);
                }
            } else {
                collectLastMessages(null, messages, commandOptions);
            }
        }
        return messages;
    }

    private void collectLastMessages(String instanceName, List<String> messages, CommandOptions commandOptions) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (instanceName == null || exporterManager.getInstanceName().equals(instanceName)) {
                exporterManager.collectMessages(messages, commandOptions);
            }
        }
    }

    /**
     * Gets new or changed messages based on the provided command options.
     * @param session the client session
     * @param commandOptions the command options specifying what to refresh
     * @return a list of new messages
     */
    public List<String> getNewMessages(@NonNull ServiceSession session, @NonNull CommandOptions commandOptions) {
        String instanceName = commandOptions.getInstance();
        List<String> messages = new ArrayList<>();
        if (session.isValid()) {
            String[] instanceNames = session.getJoinedInstances();
            if (instanceNames != null && instanceNames.length > 0) {
                for (String name : instanceNames) {
                    if (instanceName == null || name.equals(instanceName)) {
                        collectNewMessages(name, messages, commandOptions);
                    }
                }
            } else {
                collectNewMessages(instanceName, messages, commandOptions);
            }
        }
        return messages;
    }

    private void collectNewMessages(String instanceName, List<String> messages, CommandOptions commandOptions) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (instanceName == null || exporterManager.getInstanceName().equals(instanceName)) {
                exporterManager.collectNewMessages(messages, commandOptions);
            }
        }
    }

    private String @Nullable [] getUnusedInstances(ServiceSession session) {
        String[] instanceNames = getJoinedInstances(session);
        if (instanceNames == null || instanceNames.length == 0) {
            return null;
        }
        List<String> unusedInstances = new ArrayList<>(instanceNames.length);
        for (String name : instanceNames) {
            boolean using = false;
            for (ExportService exportService : exportServices) {
                if (exportService.isUsingInstance(name)) {
                    using = true;
                    break;
                }
            }
            if (!using) {
                unusedInstances.add(name);
            }
        }
        if (!unusedInstances.isEmpty()) {
            return unusedInstances.toArray(new String[0]);
        } else {
            return null;
        }
    }

    private String @Nullable [] getJoinedInstances(@NonNull ServiceSession session) {
        String[] instanceNames = session.getJoinedInstances();
        if (instanceNames == null) {
            return null;
        }
        Set<String> validJoinedInstances = new HashSet<>();
        for (String name : instanceNames) {
            if (instanceInfoHolder.containsInstance(name)) {
                validJoinedInstances.add(name);
            }
        }
        if (!validJoinedInstances.isEmpty()) {
            return validJoinedInstances.toArray(new String[0]);
        } else {
            return null;
        }
    }

    /**
     * Destroys the manager, stopping all exporters.
     */
    public void destroy() {
        for (ExporterManager exporterManager : exporterManagers) {
            exporterManager.stop();
        }
        exporterManagers.clear();
    }

}

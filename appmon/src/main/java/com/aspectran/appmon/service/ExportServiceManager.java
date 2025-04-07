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
package com.aspectran.appmon.service;

import com.aspectran.appmon.config.InstanceInfoHolder;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.annotation.jsr305.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <p>Created: 2025-02-12</p>
 */
public class ExportServiceManager {

    private final Set<ExportService> exportServices = new CopyOnWriteArraySet<>();

    private final List<ExporterManager> exporterManagers = new ArrayList<>();

    private final InstanceInfoHolder instanceInfoHolder;

    public ExportServiceManager(InstanceInfoHolder instanceInfoHolder) {
        this.instanceInfoHolder = instanceInfoHolder;
    }

    public void addExportService(ExportService exportService) {
        exportServices.add(exportService);
    }

    public void removeExportService(ExportService exportService) {
        exportServices.remove(exportService);
    }

    public void addExporterManager(ExporterManager exporterManager) {
        exporterManagers.add(exporterManager);
    }

    public void broadcast(String message) {
        for (ExportService exportService : exportServices) {
            exportService.broadcast(message);
        }
    }

    public void broadcast(ServiceSession session, String message) {
        for (ExportService exportService : exportServices) {
            exportService.broadcast(session, message);
        }
    }

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

    public List<String> getLastMessages(@NonNull ServiceSession session) {
        List<String> messages = new ArrayList<>();
        if (session.isValid()) {
            String[] instanceNames = session.getJoinedInstances();
            if (instanceNames != null && instanceNames.length > 0) {
                for (String name : instanceNames) {
                    collectLastMessages(name, messages);
                }
            } else {
                collectLastMessages(null, messages);
            }
        }
        return messages;
    }

    private void collectLastMessages(String instanceName, List<String> messages) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (instanceName == null || exporterManager.getInstanceName().equals(instanceName)) {
                exporterManager.collectMessages(messages);
            }
        }
    }

    public List<String> getNewMessages(@NonNull ServiceSession session) {
        List<String> messages = new ArrayList<>();
        if (session.isValid()) {
            String[] instanceNames = session.getJoinedInstances();
            if (instanceNames != null && instanceNames.length > 0) {
                for (String name : instanceNames) {
                    collectNewMessages(name, messages);
                }
            } else {
                collectNewMessages(null, messages);
            }
        }
        return messages;
    }

    private void collectNewMessages(String instanceName, List<String> messages) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (instanceName == null || exporterManager.getInstanceName().equals(instanceName)) {
                exporterManager.collectNewMessages(messages);
            }
        }
    }

    @Nullable
    private String[] getUnusedInstances(ServiceSession session) {
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

    @Nullable
    private String[] getJoinedInstances(@NonNull ServiceSession session) {
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

}

package com.aspectran.aspectow.appmon.backend.service;

import com.aspectran.aspectow.appmon.backend.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.manager.AppMonManager;
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

    private final AppMonManager appMonManager;

    private final Set<ExportService> exportServices = new CopyOnWriteArraySet<>();

    private final List<ExporterManager> exporterManagers = new ArrayList<>();

    public ExportServiceManager(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    public AppMonManager getAppMonManager() {
        return appMonManager;
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
            if (appMonManager.containsInstance(name)) {
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

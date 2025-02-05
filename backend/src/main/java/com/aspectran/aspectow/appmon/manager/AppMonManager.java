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
package com.aspectran.aspectow.appmon.manager;

import com.aspectran.aspectow.appmon.backend.config.EndpointInfo;
import com.aspectran.aspectow.appmon.backend.config.EndpointInfoHolder;
import com.aspectran.aspectow.appmon.backend.config.InstanceInfo;
import com.aspectran.aspectow.appmon.backend.config.InstanceInfoHolder;
import com.aspectran.aspectow.appmon.backend.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.backend.service.BackendSession;
import com.aspectran.aspectow.appmon.backend.service.ExportService;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.Assert;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.annotation.jsr305.Nullable;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.utils.security.TimeLimitedPBTokenIssuer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Created: 4/3/2024</p>
 */
public class AppMonManager extends InstantActivitySupport {

    private final EndpointInfoHolder endpointInfoHolder;

    private final InstanceInfoHolder instanceInfoHolder;

    private final List<ExporterManager> exporterManagers = new ArrayList<>();

    private final Set<ExportService> exportServices = new HashSet<>();

    public AppMonManager(EndpointInfoHolder endpointInfoHolder, InstanceInfoHolder instanceInfoHolder) {
        this.endpointInfoHolder = endpointInfoHolder;
        this.instanceInfoHolder = instanceInfoHolder;
    }

    @Override
    @NonNull
    public ActivityContext getActivityContext() {
        return super.getActivityContext();
    }

    @Override
    @NonNull
    public ApplicationAdapter getApplicationAdapter() {
        return super.getApplicationAdapter();
    }

    void addExporterManager(ExporterManager exporterManager) {
        exporterManagers.add(exporterManager);
    }

    public void addExportService(ExportService exportService) {
        exportServices.add(exportService);
    }

    public EndpointInfo getResidentEndpointInfo() {
        EndpointInfo endpointInfo = endpointInfoHolder.getResidentEndpointInfo();
        Assert.state(endpointInfo != null, "Resident EndpointInfo not found");
        return endpointInfo;
    }

    public List<EndpointInfo> getAvailableEndpointInfoList() {
        return endpointInfoHolder.getEndpointInfoList();
    }

    public String[] getVerifiedInstanceNames(String[] instanceNames) {
        List<InstanceInfo> infoList = getInstanceInfoList(instanceNames);
        if (!infoList.isEmpty()) {
            return InstanceInfoHolder.extractInstanceNames(infoList);
        } else {
            return new String[0];
        }
    }

    public List<InstanceInfo> getInstanceInfoList() {
        return instanceInfoHolder.getInstanceInfoList();
    }

    public List<InstanceInfo> getInstanceInfoList(String[] instanceNames) {
        return instanceInfoHolder.getInstanceInfoList(instanceNames);
    }

    public synchronized boolean join(@NonNull BackendSession session) {
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

    public synchronized void release(BackendSession session) {
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

    public List<String> getLastMessages(@NonNull BackendSession session) {
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

    public void broadcast(String message) {
        for (ExportService exportService : exportServices) {
            exportService.broadcast(message);
        }
    }

    public void broadcast(BackendSession session, String message) {
        for (ExportService exportService : exportServices) {
            exportService.broadcast(session, message);
        }
    }

    @Nullable
    private String[] getUnusedInstances(BackendSession session) {
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
    private String[] getJoinedInstances(@NonNull BackendSession session) {
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

    public static String issueToken() {
        return issueToken(60); // default 60 secs.
    }

    public static String issueToken(int expirationTimeInSeconds) {
        return TimeLimitedPBTokenIssuer.getToken(1000L * expirationTimeInSeconds);
    }

    public static void validateToken(String token) throws InvalidPBTokenException {
        TimeLimitedPBTokenIssuer.validate(token);
    }

}

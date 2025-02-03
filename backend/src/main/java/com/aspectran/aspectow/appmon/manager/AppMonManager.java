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
import com.aspectran.aspectow.appmon.backend.service.BackendService;
import com.aspectran.aspectow.appmon.backend.service.BackendSession;
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

    private final Set<BackendService> backendServices = new HashSet<>();

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

    public void addBackendService(BackendService backendService) {
        backendServices.add(backendService);
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
        String[] unusedGroups = getUnusedGroups(session);
        if (unusedGroups != null) {
            for (String groupName : unusedGroups) {
                stopExporters(groupName);
            }
        }
        session.removeJoinedInstances();
    }

    private void stopExporters(String groupName) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (groupName == null || exporterManager.getInstanceName().equals(groupName)) {
                exporterManager.stop();
            }
        }
    }

    public List<String> getLastMessages(@NonNull BackendSession session) {
        List<String> messages = new ArrayList<>();
        if (session.isValid()) {
            String[] joinGroups = session.getJoinedInstances();
            if (joinGroups != null && joinGroups.length > 0) {
                for (String group : joinGroups) {
                    collectLastMessages(group, messages);
                }
            } else {
                collectLastMessages(null, messages);
            }
        }
        return messages;
    }

    private void collectLastMessages(String groupName, List<String> messages) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (groupName == null || exporterManager.getInstanceName().equals(groupName)) {
                exporterManager.collectMessages(messages);
            }
        }
    }

    public void broadcast(String message) {
        for (BackendService backendService : backendServices) {
            backendService.broadcast(message);
        }
    }

    public void broadcast(BackendSession session, String message) {
        for (BackendService backendService : backendServices) {
            backendService.broadcast(session, message);
        }
    }

    @Nullable
    private String[] getUnusedGroups(BackendSession session) {
        String[] joinedGroups = getJoinedGroups(session);
        if (joinedGroups == null || joinedGroups.length == 0) {
            return null;
        }
        List<String> unusedGroups = new ArrayList<>(joinedGroups.length);
        for (String name : joinedGroups) {
            boolean using = false;
            for (BackendService backendService : backendServices) {
                if (backendService.isUsingInstance(name)) {
                    using = true;
                    break;
                }
            }
            if (!using) {
                unusedGroups.add(name);
            }
        }
        if (!unusedGroups.isEmpty()) {
            return unusedGroups.toArray(new String[0]);
        } else {
            return null;
        }
    }

    @Nullable
    private String[] getJoinedGroups(@NonNull BackendSession session) {
        String[] joinedGroups = session.getJoinedInstances();
        if (joinedGroups == null) {
            return null;
        }
        Set<String> validJoinedGroups = new HashSet<>();
        for (String name : joinedGroups) {
            if (instanceInfoHolder.containsInstance(name)) {
                validJoinedGroups.add(name);
            }
        }
        if (!validJoinedGroups.isEmpty()) {
            return validJoinedGroups.toArray(new String[0]);
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

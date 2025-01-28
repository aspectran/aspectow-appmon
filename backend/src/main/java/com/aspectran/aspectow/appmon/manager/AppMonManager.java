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

import com.aspectran.aspectow.appmon.config.EndpointInfo;
import com.aspectran.aspectow.appmon.config.EndpointInfoHolder;
import com.aspectran.aspectow.appmon.config.GroupInfo;
import com.aspectran.aspectow.appmon.config.GroupInfoHolder;
import com.aspectran.aspectow.appmon.endpoint.AppMonEndpoint;
import com.aspectran.aspectow.appmon.endpoint.AppMonSession;
import com.aspectran.aspectow.appmon.exporter.ExporterManager;
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

    private final GroupInfoHolder groupInfoHolder;

    private final List<ExporterManager> exporterManagers = new ArrayList<>();

    private final Set<AppMonEndpoint> endpoints = new HashSet<>();

    public AppMonManager(EndpointInfoHolder endpointInfoHolder, GroupInfoHolder groupInfoHolder) {
        this.endpointInfoHolder = endpointInfoHolder;
        this.groupInfoHolder = groupInfoHolder;
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

    public void addEndpoint(AppMonEndpoint endpoint) {
        endpoints.add(endpoint);
    }

    public EndpointInfo getResidentEndpointInfo() {
        EndpointInfo endpointInfo = endpointInfoHolder.getResidentEndpointInfo();
        Assert.state(endpointInfo != null, "Resident EndpointInfo not found");
        return endpointInfo;
    }

    public List<EndpointInfo> getAvailableEndpointInfoList() {
        return endpointInfoHolder.getEndpointInfoList();
    }

    public String[] getVerifiedGroupNames(String[] joinGroupNames) {
        List<GroupInfo> groups = getGroupInfoList(joinGroupNames);
        if (!groups.isEmpty()) {
            return GroupInfoHolder.extractGroupNames(groups);
        } else {
            return new String[0];
        }
    }

    public List<GroupInfo> getGroupInfoList(String[] joinGroupNames) {
        return groupInfoHolder.getGroupInfoList(joinGroupNames);
    }

    public synchronized boolean join(@NonNull AppMonSession session) {
        if (session.isValid()) {
            String[] joinedGroups = session.getJoinedGroups();
            if (joinedGroups != null && joinedGroups.length > 0) {
                for (String groupName : joinedGroups) {
                    startExporters(groupName);
                }
            } else {
                startExporters(null);
            }
            return true;
        } else {
            return false;
        }
    }

    private void startExporters(String groupName) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (groupName == null || exporterManager.getGroupName().equals(groupName)) {
                exporterManager.start();
            }
        }
    }

    public synchronized void release(AppMonSession session) {
        String[] unusedGroups = getUnusedGroups(session);
        if (unusedGroups != null) {
            for (String groupName : unusedGroups) {
                stopExporters(groupName);
            }
        }
        session.removeJoinedGroups();
    }

    private void stopExporters(String groupName) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (groupName == null || exporterManager.getGroupName().equals(groupName)) {
                exporterManager.stop();
            }
        }
    }

    public List<String> getLastMessages(@NonNull AppMonSession session) {
        List<String> messages = new ArrayList<>();
        if (session.isValid()) {
            String[] joinGroups = session.getJoinedGroups();
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
            if (groupName == null || exporterManager.getGroupName().equals(groupName)) {
                exporterManager.collectMessages(messages);
            }
        }
    }

    public void broadcast(String message) {
        for (AppMonEndpoint endpoint : endpoints) {
            endpoint.broadcast(message);
        }
    }

    public void broadcast(AppMonSession session, String message) {
        for (AppMonEndpoint endpoint : endpoints) {
            endpoint.broadcast(session, message);
        }
    }

    @Nullable
    private String[] getUnusedGroups(AppMonSession session) {
        String[] joinedGroups = getJoinedGroups(session);
        if (joinedGroups == null || joinedGroups.length == 0) {
            return null;
        }
        List<String> unusedGroups = new ArrayList<>(joinedGroups.length);
        for (String name : joinedGroups) {
            boolean using = false;
            for (AppMonEndpoint endpoint : endpoints) {
                if (endpoint.isUsingGroup(name)) {
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
    private String[] getJoinedGroups(@NonNull AppMonSession session) {
        String[] joinedGroups = session.getJoinedGroups();
        if (joinedGroups == null) {
            return null;
        }
        Set<String> validJoinedGroups = new HashSet<>();
        for (String name : joinedGroups) {
            if (groupInfoHolder.containsGroup(name)) {
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

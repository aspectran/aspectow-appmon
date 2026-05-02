/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.appmon.engine.relay;

import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing application subscriptions from both local sessions
 * and remote nodes in a cluster.
 *
 * <p>Created: 2026-05-02</p>
 */
public class SubscriptionRegistry {

    /** appId -> Set of local session IDs */
    private final Map<String, Set<String>> localSubscriptions = new ConcurrentHashMap<>();

    /** appId -> Set of remote node IDs */
    private final Map<String, Set<String>> remoteSubscriptions = new ConcurrentHashMap<>();

    /** session ID -> Array of joined app IDs */
    private final Map<String, String[]> sessionToApps = new ConcurrentHashMap<>();

    public void addLocalSubscription(@NonNull String sessionId, String[] appIds) {
        sessionToApps.put(sessionId, appIds != null ? appIds : new String[0]);
        if (appIds != null && appIds.length > 0) {
            for (String appId : appIds) {
                localSubscriptions.computeIfAbsent(appId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
            }
        } else {
            localSubscriptions.computeIfAbsent("", k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        }
    }

    public void removeLocalSubscription(@NonNull String sessionId) {
        String[] appIds = sessionToApps.remove(sessionId);
        if (appIds != null) {
            if (appIds.length > 0) {
                for (String appId : appIds) {
                    removeLocalAppSubscription(appId, sessionId);
                }
            } else {
                removeLocalAppSubscription("", sessionId);
            }
        }
    }

    private void removeLocalAppSubscription(String appId, String sessionId) {
        Set<String> sessions = localSubscriptions.get(appId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                localSubscriptions.remove(appId);
            }
        }
    }

    public void addRemoteSubscription(String nodeId, String appId) {
        if (appId == null) {
            appId = "";
        }
        remoteSubscriptions.computeIfAbsent(appId, k -> ConcurrentHashMap.newKeySet()).add(nodeId);
    }

    public void removeRemoteSubscription(String nodeId, String appId) {
        if (appId == null) {
            appId = "";
        }
        Set<String> nodes = remoteSubscriptions.get(appId);
        if (nodes != null) {
            nodes.remove(nodeId);
            if (nodes.isEmpty()) {
                remoteSubscriptions.remove(appId);
            }
        }
    }

    public boolean isAppInUse(String appId) {
        if (appId == null) {
            appId = "";
        }
        Set<String> sessions = localSubscriptions.get(appId);
        if (sessions != null && !sessions.isEmpty()) {
            return true;
        }
        Set<String> nodes = remoteSubscriptions.get(appId);
        return (nodes != null && !nodes.isEmpty());
    }

    public boolean isAppInUseLocally(String appId) {
        if (appId == null) {
            appId = "";
        }
        Set<String> sessions = localSubscriptions.get(appId);
        return (sessions != null && !sessions.isEmpty());
    }

    public void clear() {
        localSubscriptions.clear();
        remoteSubscriptions.clear();
        sessionToApps.clear();
    }

}

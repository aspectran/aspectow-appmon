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
package com.aspectran.aspectow.appmon.engine.config;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A holder for managing a collection of {@link AppInfo} objects.
 * This class provides methods to access and filter app information.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class AppInfoHolder {

    private final Map<String, AppInfo> appInfos = new LinkedHashMap<>();

    /**
     * Instantiates a new AppInfoHolder.
     * @param nodeId the id of the node these apps belong to
     * @param appInfoList the list of app information to hold
     */
    public AppInfoHolder(String nodeId, @NonNull List<AppInfo> appInfoList) {
        for (AppInfo appInfo : appInfoList) {
            appInfo.setNodeId(nodeId);
            appInfos.put(appInfo.getAppId(), appInfo);

            List<EventInfo> eventInfoList = appInfo.getEventInfoList();
            if (eventInfoList != null) {
                for (EventInfo eventInfo : eventInfoList) {
                    eventInfo.setNodeId(nodeId);
                    eventInfo.setAppId(appInfo.getAppId());
                }
            }
            List<MetricInfo> metricInfoList = appInfo.getMetricInfoList();
            if (metricInfoList != null) {
                for (MetricInfo metricInfo : metricInfoList) {
                    metricInfo.setNodeId(nodeId);
                    metricInfo.setAppId(appInfo.getAppId());
                }
            }
            List<LogInfo> logInfoList = appInfo.getLogInfoList();
            if (logInfoList != null) {
                for (LogInfo logInfo : logInfoList) {
                    logInfo.setNodeId(nodeId);
                    logInfo.setAppId(appInfo.getAppId());
                }
            }
        }
    }

    /**
     * Gets the list of all visible app information.
     * @return a list of {@link AppInfo}
     */
    public List<AppInfo> getAppInfoList() {
        return getAppInfoList(null);
    }

    /**
     * Gets a filtered list of app information.
     * If appIds is null or empty, it returns all visible (not hidden) apps.
     * Otherwise, it returns apps matching the given IDs.
     * @param appIds an array of app IDs to filter by
     * @return a list of matching {@link AppInfo}
     */
    public List<AppInfo> getAppInfoList(String[] appIds) {
        List<AppInfo> infoList = new ArrayList<>(appInfos.size());
        if (appIds != null && appIds.length > 0) {
            for (String id : appIds) {
                for (AppInfo info : appInfos.values()) {
                    if (info.getAppId().equals(id)) {
                        infoList.add(info);
                    }
                }
            }
        } else {
            for (AppInfo info : appInfos.values()) {
                if (!info.isHidden()) {
                    infoList.add(info);
                }
            }
        }
        return infoList;
    }

    /**
     * Checks if an app with the specified name exists.
     * @param appName the name of the app
     * @return {@code true} if the app exists, {@code false} otherwise
     */
    public boolean containsApp(String appName) {
        return appInfos.containsKey(appName);
    }

    /**
     * Extracts the IDs from a list of {@link AppInfo} objects.
     * @param appInfoList the list of app information
     * @return an array of app IDs
     */
    public static String @NonNull [] extractAppIds(@NonNull List<AppInfo> appInfoList) {
        List<String> appIds = new ArrayList<>(appInfoList.size());
        for (AppInfo appInfo : appInfoList) {
            appIds.add(appInfo.getAppId());
        }
        return appIds.toArray(new String[0]);
    }

}

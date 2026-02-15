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
package com.aspectran.appmon.engine.config;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A holder for managing a collection of {@link InstanceInfo} objects.
 * This class provides methods to access and filter instance information.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class InstanceInfoHolder {

    private final Map<String, InstanceInfo> instanceInfos = new LinkedHashMap<>();

    /**
     * Instantiates a new InstanceInfoHolder.
     * @param domainName the name of the domain these instances belong to
     * @param instanceInfoList the list of instance information to hold
     */
    public InstanceInfoHolder(String domainName, @NonNull List<InstanceInfo> instanceInfoList) {
        for (InstanceInfo instanceInfo : instanceInfoList) {
            instanceInfo.setDomainName(domainName);
            instanceInfos.put(instanceInfo.getName(), instanceInfo);

            List<EventInfo> eventInfoList = instanceInfo.getEventInfoList();
            if (eventInfoList != null) {
                for (EventInfo eventInfo : eventInfoList) {
                    eventInfo.setDomainName(domainName);
                    eventInfo.setInstanceName(instanceInfo.getName());
                }
            }
            List<MetricInfo> metricInfoList = instanceInfo.getMetricInfoList();
            if (metricInfoList != null) {
                for (MetricInfo metricInfo : metricInfoList) {
                    metricInfo.setDomainName(domainName);
                    metricInfo.setInstanceName(instanceInfo.getName());
                }
            }
            List<LogInfo> logInfoList = instanceInfo.getLogInfoList();
            if (logInfoList != null) {
                for (LogInfo logInfo : logInfoList) {
                    logInfo.setDomainName(domainName);
                    logInfo.setInstanceName(instanceInfo.getName());
                }
            }
        }
    }

    /**
     * Gets the list of all visible instance information.
     * @return a list of {@link InstanceInfo}
     */
    public List<InstanceInfo> getInstanceInfoList() {
        return getInstanceInfoList(null);
    }

    /**
     * Gets a filtered list of instance information.
     * If instanceNames is null or empty, it returns all visible (not hidden) instances.
     * Otherwise, it returns instances matching the given names.
     * @param instanceNames an array of instance names to filter by
     * @return a list of matching {@link InstanceInfo}
     */
    public List<InstanceInfo> getInstanceInfoList(String[] instanceNames) {
        List<InstanceInfo> infoList = new ArrayList<>(instanceInfos.size());
        if (instanceNames != null && instanceNames.length > 0) {
            for (String name : instanceNames) {
                for (InstanceInfo info : instanceInfos.values()) {
                    if (info.getName().equals(name)) {
                        infoList.add(info);
                    }
                }
            }
        } else {
            for (InstanceInfo info : instanceInfos.values()) {
                if (!info.isHidden()) {
                    infoList.add(info);
                }
            }
        }
        return infoList;
    }

    /**
     * Checks if an instance with the specified name exists.
     * @param instanceName the name of the instance
     * @return {@code true} if the instance exists, {@code false} otherwise
     */
    public boolean containsInstance(String instanceName) {
        return instanceInfos.containsKey(instanceName);
    }

    /**
     * Extracts the names from a list of {@link InstanceInfo} objects.
     * @param instanceInfoList the list of instance information
     * @return an array of instance names
     */
    public static String @NonNull [] extractInstanceNames(@NonNull List<InstanceInfo> instanceInfoList) {
        List<String> instanceNames = new ArrayList<>(instanceInfoList.size());
        for (InstanceInfo instanceInfo : instanceInfoList) {
            instanceNames.add(instanceInfo.getName());
        }
        return instanceNames.toArray(new String[0]);
    }

}

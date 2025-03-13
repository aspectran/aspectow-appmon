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
package com.aspectran.appmon.config;

import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InstanceInfoHolder {

    private final Map<String, InstanceInfo> instanceInfos = new LinkedHashMap<>();

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
            List<LogInfo> logInfoList = instanceInfo.getLogInfoList();
            if (logInfoList != null) {
                for (LogInfo logInfo : logInfoList) {
                    logInfo.setDomainName(domainName);
                    logInfo.setInstanceName(instanceInfo.getName());
                }
            }
        }
    }

    public List<InstanceInfo> getInstanceInfoList() {
        return getInstanceInfoList(null);
    }

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

    public boolean containsInstance(String instanceName) {
        return instanceInfos.containsKey(instanceName);
    }

    @NonNull
    public static String[] extractInstanceNames(@NonNull List<InstanceInfo> instanceInfoList) {
        List<String> instanceNames = new ArrayList<>(instanceInfoList.size());
        for (InstanceInfo instanceInfo : instanceInfoList) {
            instanceNames.add(instanceInfo.getName());
        }
        return instanceNames.toArray(new String[0]);
    }

}

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
package com.aspectran.aspectow.appmon.config;

import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GroupInfoHolder {

    private final Map<String, GroupInfo> groupInfos = new LinkedHashMap<>();

    public GroupInfoHolder(@NonNull List<GroupInfo> groupInfoList) {
        for (GroupInfo info : groupInfoList) {
            groupInfos.put(info.getName(), info);

            List<EventInfo> eventInfoList = info.getEventInfoList();
            if (eventInfoList != null) {
                for (EventInfo eventInfo : eventInfoList) {
                    eventInfo.setGroup(info.getName());
                }
            }
            List<LogInfo> logInfoList = info.getLogInfoList();
            if (logInfoList != null) {
                for (LogInfo logInfo : logInfoList) {
                    logInfo.setGroup(info.getName());
                }
            }
        }
    }

    public List<GroupInfo> getGroupInfoList(String[] joinGroupNames) {
        List<GroupInfo> infoList = new ArrayList<>(groupInfos.size());
        if (joinGroupNames != null && joinGroupNames.length > 0) {
            for (String name : joinGroupNames) {
                for (GroupInfo info : groupInfos.values()) {
                    if (info.getName().equals(name)) {
                        infoList.add(info);
                    }
                }
            }
        } else {
            infoList.addAll(groupInfos.values());
        }
        return infoList;
    }

    public boolean containsGroup(String groupName) {
        return groupInfos.containsKey(groupName);
    }

    @NonNull
    public static String[] extractGroupNames(@NonNull List<GroupInfo> groupInfoList) {
        List<String> groupNames = new ArrayList<>(groupInfoList.size());
        for (GroupInfo groupInfo : groupInfoList) {
            groupNames.add(groupInfo.getName());
        }
        return groupNames.toArray(new String[0]);
    }

}

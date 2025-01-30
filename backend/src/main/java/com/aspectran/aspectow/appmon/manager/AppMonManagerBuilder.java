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

import com.aspectran.aspectow.appmon.backend.config.BackendConfig;
import com.aspectran.aspectow.appmon.backend.config.EndpointInfoHolder;
import com.aspectran.aspectow.appmon.backend.config.EventInfo;
import com.aspectran.aspectow.appmon.backend.config.GroupInfo;
import com.aspectran.aspectow.appmon.backend.config.GroupInfoHolder;
import com.aspectran.aspectow.appmon.backend.config.LogInfo;
import com.aspectran.aspectow.appmon.backend.exporter.event.EventExporterBuilder;
import com.aspectran.aspectow.appmon.backend.exporter.event.EventExporterManager;
import com.aspectran.aspectow.appmon.backend.exporter.log.LogExporterBuilder;
import com.aspectran.aspectow.appmon.backend.exporter.log.LogExporterManager;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.Assert;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.List;

/**
 * <p>Created: 2024-12-17</p>
 */
public abstract class AppMonManagerBuilder {

    @NonNull
    public static AppMonManager build(ActivityContext context, BackendConfig backendConfig) throws Exception {
        Assert.notNull(context, "context must not be null");
        Assert.notNull(backendConfig, "appMonConfig must not be null");

        EndpointInfoHolder endpointInfoHolder = new EndpointInfoHolder(backendConfig.getEndpointInfoList());
        GroupInfoHolder groupInfoHolder = new GroupInfoHolder(backendConfig.getGroupInfoList());

        AppMonManager appMonManager = new AppMonManager(endpointInfoHolder, groupInfoHolder);
        appMonManager.setActivityContext(context);

        for (GroupInfo groupInfo : backendConfig.getGroupInfoList()) {
            List<EventInfo> eventInfoList = backendConfig.getEventInfoList(groupInfo.getName());
            if (eventInfoList != null && !eventInfoList.isEmpty()) {
                EventExporterManager eventExporterManager = new EventExporterManager(appMonManager, groupInfo.getName());
                appMonManager.addExporterManager(eventExporterManager);
                EventExporterBuilder.build(eventExporterManager, eventInfoList);
            }
            List<LogInfo> logInfoList = backendConfig.getLogInfoList(groupInfo.getName());
            if (logInfoList != null && !logInfoList.isEmpty()) {
                LogExporterManager logExporterManager = new LogExporterManager(appMonManager, groupInfo.getName());
                appMonManager.addExporterManager(logExporterManager);
                LogExporterBuilder.build(logExporterManager, logInfoList, context.getApplicationAdapter());
            }
        }
        return appMonManager;
    }

}

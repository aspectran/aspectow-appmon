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

import com.aspectran.aspectow.appmon.config.AppMonConfig;
import com.aspectran.aspectow.appmon.config.EndpointInfoHolder;
import com.aspectran.aspectow.appmon.config.EventInfo;
import com.aspectran.aspectow.appmon.config.GroupInfo;
import com.aspectran.aspectow.appmon.config.GroupInfoHolder;
import com.aspectran.aspectow.appmon.config.LogInfo;
import com.aspectran.aspectow.appmon.exporter.event.EventExporterManager;
import com.aspectran.aspectow.appmon.exporter.event.EventExporterManagerBuilder;
import com.aspectran.aspectow.appmon.exporter.log.LogExporterManager;
import com.aspectran.aspectow.appmon.exporter.log.LogExporterManagerBuilder;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.Assert;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.List;

/**
 * <p>Created: 2024-12-17</p>
 */
public abstract class AppMonManagerBuilder {

    @NonNull
    public static AppMonManager build(ActivityContext context, AppMonConfig config) throws Exception {
        Assert.notNull(context, "ActivityContext is not set");
        Assert.notNull(config, "AppMonConfig is not set");

        EndpointInfoHolder endpointInfoHolder = new EndpointInfoHolder(config.getEndpointInfoList());
        GroupInfoHolder groupInfoHolder = new GroupInfoHolder(config.getGroupInfoList());
        AppMonManager appMonManager = new AppMonManager(endpointInfoHolder, groupInfoHolder);
        appMonManager.setActivityContext(context);
        for (GroupInfo groupInfo : config.getGroupInfoList()) {
            List<EventInfo> eventInfoList = config.getEventInfoList(groupInfo.getName());
            if (eventInfoList != null && !eventInfoList.isEmpty()) {
                EventExporterManager eventExporterManager = appMonManager.newEventExporterManager(groupInfo.getName());
                EventExporterManagerBuilder.build(eventExporterManager, eventInfoList);
            }
            List<LogInfo> logInfoList = config.getLogInfoList(groupInfo.getName());
            if (logInfoList != null && !logInfoList.isEmpty()) {
                LogExporterManager logExporterManager = appMonManager.newLogExporterManager(groupInfo.getName());
                LogExporterManagerBuilder.build(logExporterManager, logInfoList, context.getApplicationAdapter());
            }
        }
        return appMonManager;
    }

}

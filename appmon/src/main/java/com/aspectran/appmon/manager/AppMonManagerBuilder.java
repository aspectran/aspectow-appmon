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
package com.aspectran.appmon.manager;

import com.aspectran.appmon.config.AppMonConfig;
import com.aspectran.appmon.config.DomainInfoHolder;
import com.aspectran.appmon.config.EventInfo;
import com.aspectran.appmon.config.InstanceInfo;
import com.aspectran.appmon.config.InstanceInfoHolder;
import com.aspectran.appmon.config.LogInfo;
import com.aspectran.appmon.config.PollingConfig;
import com.aspectran.appmon.exporter.event.EventExporterBuilder;
import com.aspectran.appmon.exporter.event.EventExporterManager;
import com.aspectran.appmon.exporter.log.LogExporterBuilder;
import com.aspectran.appmon.exporter.log.LogExporterManager;
import com.aspectran.appmon.persist.PersistManager;
import com.aspectran.appmon.persist.counter.EventCounterBuilder;
import com.aspectran.appmon.service.ExportServiceManager;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.Assert;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.List;

/**
 * <p>Created: 2024-12-17</p>
 */
public abstract class AppMonManagerBuilder {

    @NonNull
    public static AppMonManager build(ActivityContext context, AppMonConfig appMonConfig) throws Exception {
        Assert.notNull(context, "context must not be null");
        Assert.notNull(appMonConfig, "appMonConfig must not be null");

        AppMonManager appMonManager = createAppMonManager(context, appMonConfig);
        ExportServiceManager exportServiceManager = appMonManager.getExportServiceManager();
        PersistManager persistManager = appMonManager.getPersistManager();

        for (InstanceInfo instanceInfo : appMonConfig.getInstanceInfoList()) {
            String instanceName = instanceInfo.getName();
            List<EventInfo> eventInfoList = appMonConfig.getEventInfoList(instanceName);
            if (eventInfoList != null && !eventInfoList.isEmpty()) {
                EventExporterManager eventExporterManager = new EventExporterManager(exportServiceManager, instanceName);
                EventExporterBuilder.build(eventExporterManager, eventInfoList);
                EventCounterBuilder.build(persistManager, eventInfoList);
            }
            List<LogInfo> logInfoList = appMonConfig.getLogInfoList(instanceName);
            if (logInfoList != null && !logInfoList.isEmpty()) {
                LogExporterManager logExporterManager = new LogExporterManager(exportServiceManager, instanceName);
                LogExporterBuilder.build(logExporterManager, logInfoList, context.getApplicationAdapter());
            }
        }

        return appMonManager;
    }

    @NonNull
    private static AppMonManager createAppMonManager(ActivityContext context, @NonNull AppMonConfig appMonConfig) {
        PollingConfig pollingConfig = appMonConfig.getPollingConfig();
        if (pollingConfig == null) {
            pollingConfig = new PollingConfig();
        }

        DomainInfoHolder domainInfoHolder = new DomainInfoHolder(appMonConfig.getDomainInfoList());
        InstanceInfoHolder instanceInfoHolder = new InstanceInfoHolder(appMonConfig.getInstanceInfoList());

        AppMonManager appMonManager = new AppMonManager(pollingConfig, domainInfoHolder, instanceInfoHolder);
        appMonManager.setActivityContext(context);
        return appMonManager;
    }

}

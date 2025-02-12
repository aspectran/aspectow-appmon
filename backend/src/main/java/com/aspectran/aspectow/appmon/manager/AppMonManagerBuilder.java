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
import com.aspectran.aspectow.appmon.backend.config.InstanceInfo;
import com.aspectran.aspectow.appmon.backend.config.InstanceInfoHolder;
import com.aspectran.aspectow.appmon.backend.config.LogInfo;
import com.aspectran.aspectow.appmon.backend.config.PollingConfig;
import com.aspectran.aspectow.appmon.backend.exporter.event.EventExporterBuilder;
import com.aspectran.aspectow.appmon.backend.exporter.event.EventExporterManager;
import com.aspectran.aspectow.appmon.backend.exporter.log.LogExporterBuilder;
import com.aspectran.aspectow.appmon.backend.exporter.log.LogExporterManager;
import com.aspectran.aspectow.appmon.backend.persist.PersistManager;
import com.aspectran.aspectow.appmon.backend.persist.counter.CounterPersistBuilder;
import com.aspectran.aspectow.appmon.backend.service.ExportServiceManager;
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
        Assert.notNull(backendConfig, "backendConfig must not be null");

        AppMonManager appMonManager = createAppMonManager(context, backendConfig);
        ExportServiceManager exportServiceManager = appMonManager.getExportServiceManager();
        PersistManager persistManager = appMonManager.getPersistManager();

        for (InstanceInfo instanceInfo : backendConfig.getInstanceInfoList()) {
            String instanceName = instanceInfo.getName();
            List<EventInfo> eventInfoList = backendConfig.getEventInfoList(instanceName);
            if (eventInfoList != null && !eventInfoList.isEmpty()) {
                EventExporterManager eventExporterManager = new EventExporterManager(exportServiceManager, instanceName);
                EventExporterBuilder.build(eventExporterManager, eventInfoList);
                CounterPersistBuilder.build(persistManager, eventInfoList);
            }
            List<LogInfo> logInfoList = backendConfig.getLogInfoList(instanceName);
            if (logInfoList != null && !logInfoList.isEmpty()) {
                LogExporterManager logExporterManager = new LogExporterManager(exportServiceManager, instanceName);
                LogExporterBuilder.build(logExporterManager, logInfoList, context.getApplicationAdapter());
            }
        }

        return appMonManager;
    }

    @NonNull
    private static AppMonManager createAppMonManager(ActivityContext context, @NonNull BackendConfig backendConfig) {
        PollingConfig pollingConfig = backendConfig.getPollingConfig();
        if (pollingConfig == null) {
            pollingConfig = new PollingConfig();
        }

        EndpointInfoHolder endpointInfoHolder = new EndpointInfoHolder(backendConfig.getEndpointInfoList());
        InstanceInfoHolder instanceInfoHolder = new InstanceInfoHolder(backendConfig.getInstanceInfoList());

        AppMonManager appMonManager = new AppMonManager(pollingConfig, endpointInfoHolder, instanceInfoHolder);
        appMonManager.setActivityContext(context);
        return appMonManager;
    }

}

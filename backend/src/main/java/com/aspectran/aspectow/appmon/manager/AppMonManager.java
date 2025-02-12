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

import com.aspectran.aspectow.appmon.backend.config.EndpointInfo;
import com.aspectran.aspectow.appmon.backend.config.EndpointInfoHolder;
import com.aspectran.aspectow.appmon.backend.config.InstanceInfo;
import com.aspectran.aspectow.appmon.backend.config.InstanceInfoHolder;
import com.aspectran.aspectow.appmon.backend.config.PollingConfig;
import com.aspectran.aspectow.appmon.backend.persist.PersistManager;
import com.aspectran.aspectow.appmon.backend.service.ExportServiceManager;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.utils.security.TimeLimitedPBTokenIssuer;

import java.util.List;

/**
 * <p>Created: 4/3/2024</p>
 */
public class AppMonManager extends InstantActivitySupport {

    private final PollingConfig pollingConfig;

    private final EndpointInfoHolder endpointInfoHolder;

    private final InstanceInfoHolder instanceInfoHolder;

    private final ExportServiceManager exportServiceManager;

    private final PersistManager persistManager;

    public AppMonManager(PollingConfig pollingConfig,
                         EndpointInfoHolder endpointInfoHolder,
                         InstanceInfoHolder instanceInfoHolder) {
        this.pollingConfig = pollingConfig;
        this.endpointInfoHolder = endpointInfoHolder;
        this.instanceInfoHolder = instanceInfoHolder;
        this.exportServiceManager = new ExportServiceManager(this);
        this.persistManager = new PersistManager(this);
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

    public PollingConfig getPollingConfig() {
        return pollingConfig;
    }

    public List<EndpointInfo> getEndpointInfoList() {
        return endpointInfoHolder.getEndpointInfoList();
    }

    public List<InstanceInfo> getInstanceInfoList() {
        return instanceInfoHolder.getInstanceInfoList();
    }

    public List<InstanceInfo> getInstanceInfoList(String[] instanceNames) {
        return instanceInfoHolder.getInstanceInfoList(instanceNames);
    }

    public boolean containsInstance(String instanceName) {
        return instanceInfoHolder.containsInstance(instanceName);
    }

    public String[] getVerifiedInstanceNames(String[] instanceNames) {
        List<InstanceInfo> infoList = getInstanceInfoList(instanceNames);
        if (!infoList.isEmpty()) {
            return InstanceInfoHolder.extractInstanceNames(infoList);
        } else {
            return new String[0];
        }
    }

    public ExportServiceManager getExportServiceManager() {
        return exportServiceManager;
    }

    public PersistManager getPersistManager() {
        return persistManager;
    }

    public <V> V getBean(@NonNull String id) {
        return getActivityContext().getBeanRegistry().getBean(id);
    }

    public <V> V getBean(Class<V> type) {
        return getActivityContext().getBeanRegistry().getBean(type);
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

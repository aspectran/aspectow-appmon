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

import com.aspectran.utils.Assert;
import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * <p>Created: 2024/12/17</p>
 */
public class BackendConfig extends AbstractParameters {

    private static final ParameterKey pollingConfig;
    private static final ParameterKey domain;
    private static final ParameterKey instance;

    private static final ParameterKey[] parameterKeys;

    static {
        pollingConfig = new ParameterKey("pollingConfig", PollingConfig.class);
        domain = new ParameterKey("domains", new String[] {"domain"}, DomainInfo.class, true, true);
        instance = new ParameterKey("instances", new String[] {"instance"}, InstanceInfo.class, true, true);

        parameterKeys = new ParameterKey[] {
                pollingConfig,
                domain,
                instance
        };
    }

    public BackendConfig() {
        super(parameterKeys);
    }

    public BackendConfig(Reader reader) throws IOException {
        this();
        readFrom(reader);
    }

    public BackendConfig(File configFile) throws IOException {
        this();
        readFrom(configFile);
    }

    public PollingConfig getPollingConfig() {
        return getParameters(pollingConfig);
    }

    public void setPollingConfig(PollingConfig pollingConfig) {
        putValue(BackendConfig.pollingConfig, pollingConfig);
    }

    public List<DomainInfo> getDomainInfoList() {
        return getParametersList(domain);
    }

    public List<InstanceInfo> getInstanceInfoList() {
        return getParametersList(instance);
    }

    public List<EventInfo> getEventInfoList(String instanceName) {
        Assert.notNull(instanceName, "instanceName must not be null");
        for (InstanceInfo instanceInfo : getInstanceInfoList()) {
            if (instanceName.equals(instanceInfo.getName())) {
                return instanceInfo.getEventInfoList();
            }
        }
        return null;
    }

    public List<LogInfo> getLogInfoList(String instanceName) {
        Assert.notNull(instanceName, "instanceName must not be null");
        for (InstanceInfo instanceInfo : getInstanceInfoList()) {
            if (instanceName.equals(instanceInfo.getName())) {
                return instanceInfo.getLogInfoList();
            }
        }
        return null;
    }

}

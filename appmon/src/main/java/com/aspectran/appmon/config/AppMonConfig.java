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
package com.aspectran.appmon.config;

import com.aspectran.utils.Assert;
import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * The main configuration class for Aspectow AppMon.
 * It defines and holds all the necessary settings for the monitoring application,
 * such as polling configuration, domain information, and instance details.
 * This class is based on APON (Aspectran Object Notation).
 *
 * <p>Created: 2024/12/17</p>
 */
public class AppMonConfig extends AbstractParameters {

    private static final ParameterKey pollingConfig;
    private static final ParameterKey counterPersistInterval;
    private static final ParameterKey domain;
    private static final ParameterKey instance;

    private static final ParameterKey[] parameterKeys;

    static {
        pollingConfig = new ParameterKey("pollingConfig", PollingConfig.class);
        counterPersistInterval = new ParameterKey("counterPersistInterval", ValueType.INT);
        domain = new ParameterKey("domains", new String[] {"domain"}, DomainInfo.class, true, true);
        instance = new ParameterKey("instances", new String[] {"instance"}, InstanceInfo.class, true, true);

        parameterKeys = new ParameterKey[] {
                pollingConfig,
                counterPersistInterval,
                domain,
                instance
        };
    }

    /**
     * Instantiates a new AppMonConfig.
     */
    public AppMonConfig() {
        super(parameterKeys);
    }

    /**
     * Instantiates a new AppMonConfig and reads configuration from the given reader.
     * @param reader the reader to read the configuration from
     * @throws IOException if an I/O error occurs
     */
    public AppMonConfig(Reader reader) throws IOException {
        this();
        readFrom(reader);
    }

    /**
     * Instantiates a new AppMonConfig and reads configuration from the given file.
     * @param configFile the file to read the configuration from
     * @throws IOException if an I/O error occurs
     */
    public AppMonConfig(File configFile) throws IOException {
        this();
        readFrom(configFile);
    }

    /**
     * Gets the polling configuration.
     * @return the polling configuration
     */
    public PollingConfig getPollingConfig() {
        return getParameters(pollingConfig);
    }

    /**
     * Sets the polling configuration.
     * @param pollingConfig the polling configuration
     */
    public void setPollingConfig(PollingConfig pollingConfig) {
        putValue(AppMonConfig.pollingConfig, pollingConfig);
    }

    /**
     * Gets the counter persistence interval in minutes.
     * @param defaultValue the default value to return if the interval is not set
     * @return the interval in minutes
     */
    public int getCounterPersistInterval(int defaultValue) {
        return getInt(counterPersistInterval, defaultValue);
    }

    /**
     * Gets the list of domain information.
     * @return the list of domain information
     */
    public List<DomainInfo> getDomainInfoList() {
        return getParametersList(domain);
    }

    /**
     * Gets the list of instance information.
     * @return the list of instance information
     */
    public List<InstanceInfo> getInstanceInfoList() {
        return getParametersList(instance);
    }

    /**
     * Gets the list of event information for a specific instance.
     * @param instanceName the name of the instance
     * @return the list of event information, or {@code null} if the instance is not found
     */
    public List<EventInfo> getEventInfoList(String instanceName) {
        Assert.notNull(instanceName, "instanceName must not be null");
        for (InstanceInfo instanceInfo : getInstanceInfoList()) {
            if (instanceName.equals(instanceInfo.getName())) {
                return instanceInfo.getEventInfoList();
            }
        }
        return null;
    }

    /**
     * Gets the list of metric information for a specific instance.
     * @param instanceName the name of the instance
     * @return the list of metric information, or {@code null} if the instance is not found
     */
    public List<MetricInfo> getMetricInfoList(String instanceName) {
        Assert.notNull(instanceName, "instanceName must not be null");
        for (InstanceInfo instanceInfo : getInstanceInfoList()) {
            if (instanceName.equals(instanceInfo.getName())) {
                return instanceInfo.getMetricInfoList();
            }
        }
        return null;
    }

    /**
     * Gets the list of log information for a specific instance.
     * @param instanceName the name of the instance
     * @return the list of log information, or {@code null} if the instance is not found
     */
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

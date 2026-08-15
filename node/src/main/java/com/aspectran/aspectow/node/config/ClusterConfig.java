/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.node.config;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Defines cluster-wide settings for node management, including identification,
 * communication mode, and shared security settings.
 */
public class ClusterConfig extends DefaultParameters {

    private static final ParameterKey id;
    private static final ParameterKey mode;
    private static final ParameterKey secret;
    private static final ParameterKey pulseInterval;
    private static final ParameterKey pulseTimeout;
    private static final ParameterKey endpoint;
    private static final ParameterKey scheduler;

    private static final ParameterKey[] parameterKeys;

    static {
        id = new ParameterKey("id", ValueType.STRING);
        mode = new ParameterKey("mode", ValueType.STRING);
        secret = new ParameterKey("secret", SecretConfig.class);
        pulseInterval = new ParameterKey("pulseInterval", ValueType.LONG);
        pulseTimeout = new ParameterKey("pulseTimeout", ValueType.LONG);
        endpoint = new ParameterKey("endpoint", EndpointConfig.class);
        scheduler = new ParameterKey("scheduler", SchedulerConfig.class);

        parameterKeys = new ParameterKey[] {
                id,
                mode,
                secret,
                pulseInterval,
                pulseTimeout,
                endpoint,
                scheduler
        };
    }

    /**
     * Instantiates a new ClusterConfig.
     */
    public ClusterConfig() {
        super(parameterKeys);
    }

    /**
     * Returns the unique identifier for the cluster.
     * @return the cluster ID
     */
    public String getId() {
        return getString(id);
    }

    /**
     * Sets the unique identifier for the cluster.
     * @param id the cluster ID
     */
    public void setId(String id) {
        putValue(ClusterConfig.id, id);
    }

    /**
     * Returns the communication mode of the cluster (e.g., "gateway", "direct").
     * @return the cluster mode
     */
    public String getMode() {
        return getString(mode);
    }

    /**
     * Sets the communication mode of the cluster.
     * @param mode the cluster mode
     */
    public void setMode(String mode) {
        putValue(ClusterConfig.mode, mode);
    }

    /**
     * Returns whether the cluster is in direct communication mode.
     * @return true if in direct mode, false otherwise
     */
    public boolean isDirectMode() {
        return (!isGatewayMode());
    }

    /**
     * Returns whether the cluster is in gateway communication mode.
     * @return true if in gateway mode, false otherwise
     */
    public boolean isGatewayMode() {
        return "gateway".equals(getString(mode));
    }

    /**
     * Returns the security configuration for shared secrets.
     * @return the secret configuration
     */
    public SecretConfig getSecretConfig() {
        return getParameters(secret);
    }

    /**
     * Returns the interval between pulse signals in milliseconds.
     * @return the interval between pulse signals
     */
    public Long getPulseInterval() {
        return getLong(pulseInterval);
    }

    /**
     * Returns the interval between pulse signals with a fallback default value.
     * @param defaultValue the default interval to return if not specified
     * @return the interval between pulse signals
     */
    public long getPulseInterval(long defaultValue) {
        return getLong(pulseInterval, defaultValue);
    }

    /**
     * Sets the interval between pulse signals in milliseconds.
     * @param pulseInterval the interval between pulse signals
     */
    public void setPulseInterval(long pulseInterval) {
        putValue(ClusterConfig.pulseInterval, pulseInterval);
    }

    /**
     * Returns the timeout threshold for pulse signals in milliseconds.
     * @return the timeout threshold for pulse signals
     */
    public Long getPulseTimeout() {
        return getLong(pulseTimeout);
    }

    /**
     * Returns the timeout threshold for pulse signals with a fallback default value.
     * @param defaultValue the default timeout to return if not specified
     * @return the timeout threshold for pulse signals
     */
    public long getPulseTimeout(long defaultValue) {
        return getLong(pulseTimeout, defaultValue);
    }

    /**
     * Sets the timeout threshold for pulse signals in milliseconds.
     * @param pulseTimeout the timeout threshold for pulse signals
     */
    public void setPulseTimeout(long pulseTimeout) {
        putValue(ClusterConfig.pulseTimeout, pulseTimeout);
    }

    /**
     * Returns the configuration for communication endpoints.
     * @return the endpoint configuration
     */
    public EndpointConfig getEndpointConfig() {
        return getParameters(endpoint);
    }

    /**
     * Returns the endpoint configuration, creating it if it does not exist.
     * @return the endpoint configuration
     */
    public EndpointConfig touchEndpointConfig() {
        return touchParameters(endpoint);
    }

    /**
     * Sets the configuration for communication endpoints.
     * @param endpointConfig the endpoint configuration
     */
    public void setEndpointConfig(EndpointConfig endpointConfig) {
        putValue(endpoint, endpointConfig);
    }

    /**
     * Returns the configuration for the distributed scheduler.
     * @return the scheduler configuration
     */
    public SchedulerConfig getSchedulerConfig() {
        return getParameters(scheduler);
    }

    /**
     * Returns the scheduler configuration, creating it if it does not exist.
     * @return the scheduler configuration
     */
    public SchedulerConfig touchSchedulerConfig() {
        return touchParameters(scheduler);
    }

    /**
     * Sets the configuration for the distributed scheduler.
     * @param schedulerConfig the scheduler configuration
     */
    public void setSchedulerConfig(SchedulerConfig schedulerConfig) {
        putValue(scheduler, schedulerConfig);
    }

}

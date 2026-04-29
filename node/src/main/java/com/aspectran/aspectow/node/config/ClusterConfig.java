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
    private static final ParameterKey heartbeatInterval;
    private static final ParameterKey endpoint;
    private static final ParameterKey scheduler;

    private static final ParameterKey[] parameterKeys;

    static {
        id = new ParameterKey("id", ValueType.STRING);
        mode = new ParameterKey("mode", ValueType.STRING);
        secret = new ParameterKey("secret", SecretConfig.class);
        heartbeatInterval = new ParameterKey("heartbeatInterval", ValueType.LONG);
        endpoint = new ParameterKey("endpoint", EndpointConfig.class);
        scheduler = new ParameterKey("scheduler", SchedulerConfig.class);

        parameterKeys = new ParameterKey[] {
                id,
                mode,
                secret,
                heartbeatInterval,
                endpoint,
                scheduler
        };
    }

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
     * Returns the communication mode of the cluster (e.g., "gateway", "direct", "autoscaling").
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
        return (!isGatewayMode() && !isAutoscalingMode());
    }

    /**
     * Returns whether the cluster is in gateway communication mode.
     * @return true if in gateway mode, false otherwise
     */
    public boolean isGatewayMode() {
        return "gateway".equals(getString(mode));
    }

    /**
     * Returns whether the cluster is in autoscaling communication mode.
     * @return true if in autoscaling mode, false otherwise
     */
    public boolean isAutoscalingMode() {
        return "autoscaling".equals(getString(mode));
    }

    /**
     * Returns the security configuration for shared secrets.
     * @return the secret configuration
     */
    public SecretConfig getSecretConfig() {
        return getParameters(secret);
    }

    /**
     * Returns the interval between heartbeat signals in milliseconds.
     * @return the heartbeat interval
     */
    public long getHeartbeatInterval() {
        return getLong(heartbeatInterval);
    }

    /**
     * Returns the interval between heartbeat signals with a fallback default value.
     * @param defaultValue the default interval to return if not specified
     * @return the heartbeat interval
     */
    public long getHeartbeatInterval(long defaultValue) {
        return getLong(heartbeatInterval, defaultValue);
    }

    /**
     * Sets the interval between heartbeat signals in milliseconds.
     * @param heartbeatInterval the heartbeat interval
     */
    public void setHeartbeatInterval(long heartbeatInterval) {
        putValue(ClusterConfig.heartbeatInterval, heartbeatInterval);
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

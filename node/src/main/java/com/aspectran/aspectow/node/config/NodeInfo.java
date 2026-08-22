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
import org.jspecify.annotations.NonNull;

/**
 * Defines the properties and metadata of a single node within the cluster.
 */
public class NodeInfo extends DefaultParameters {

    private static final ParameterKey id;
    private static final ParameterKey group;
    private static final ParameterKey title;
    private static final ParameterKey console;
    private static final ParameterKey host;
    private static final ParameterKey port;
    private static final ParameterKey startTime;
    private static final ParameterKey status;
    private static final ParameterKey pulseInterval;
    private static final ParameterKey endpoint;
    private static final ParameterKey token;
    private static final ParameterKey hasNodeManager;
    private static final ParameterKey hasSchedulerManager;
    private static final ParameterKey hasCommandManager;

    private static final ParameterKey[] parameterKeys;

    static {
        id = new ParameterKey("id", ValueType.STRING);
        group = new ParameterKey("group", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        console = new ParameterKey("console", ValueType.BOOLEAN);
        host = new ParameterKey("host", ValueType.STRING);
        port = new ParameterKey("port", ValueType.INT);
        startTime = new ParameterKey("startTime", ValueType.STRING);
        status = new ParameterKey("status", ValueType.STRING);
        pulseInterval = new ParameterKey("pulseInterval", ValueType.LONG);
        endpoint = new ParameterKey("endpoint", EndpointConfig.class);
        token = new ParameterKey("token", ValueType.STRING);
        hasNodeManager = new ParameterKey("hasNodeManager", ValueType.BOOLEAN);
        hasSchedulerManager = new ParameterKey("hasSchedulerManager", ValueType.BOOLEAN);
        hasCommandManager = new ParameterKey("hasCommandManager", ValueType.BOOLEAN);

        parameterKeys = new ParameterKey[] {
                id,
                group,
                title,
                console,
                host,
                port,
                startTime,
                status,
                pulseInterval,
                endpoint,
                token,
                hasNodeManager,
                hasSchedulerManager,
                hasCommandManager
        };
    }

    /**
     * Instantiates a new NodeInfo.
     */
    public NodeInfo() {
        super(parameterKeys);
    }

    /**
     * Returns the unique identifier of the node.
     * @return the node ID
     */
    public String getId() {
        return getString(id);
    }

    /**
     * Sets the unique identifier of the node.
     * @param id the node ID
     */
    public void setId(String id) {
        putValue(NodeInfo.id, id);
    }

    /**
     * Returns the group to which the node belongs.
     * @return the group ID
     */
    public String getGroup() {
        return getString(group);
    }

    /**
     * Sets the group to which the node belongs.
     * @param group the group ID
     */
    public void setGroup(String group) {
        putValue(NodeInfo.group, group);
    }

    /**
     * Returns the display title of the node.
     * @return the node title
     */
    public String getTitle() {
        return getString(title);
    }

    /**
     * Sets the display title of the node.
     * @param title the node title
     */
    public void setTitle(String title) {
        putValue(NodeInfo.title, title);
    }

    /**
     * Returns whether this node is a console-dedicated node.
     * @return true if console node, false otherwise
     */
    public Boolean isConsole() {
        return getBoolean(console);
    }

    /**
     * Returns whether this node is a console-dedicated node with a fallback default.
     * @param defaultValue the default value
     * @return true if console node, false otherwise
     */
    public boolean isConsole(boolean defaultValue) {
        return getBoolean(console, defaultValue);
    }

    /**
     * Sets whether this node is a console-dedicated node.
     * @param isConsole true if console node, false otherwise
     */
    public void setConsole(Boolean isConsole) {
        putValue(NodeInfo.console, isConsole);
    }

    /**
     * Returns the host name or IP address of the node.
     * @return the host name or IP address
     */
    public String getHost() {
        return getString(host);
    }

    /**
     * Sets the host name or IP address of the node.
     * @param host the host name or IP address
     */
    public void setHost(String host) {
        putValue(NodeInfo.host, host);
    }

    /**
     * Returns the communication port of the node.
     * @return the port number
     */
    public Integer getPort() {
        return getInt(port);
    }

    /**
     * Sets the communication port of the node.
     * @param port the port number
     */
    public void setPort(Integer port) {
        putValue(NodeInfo.port, port);
    }

    /**
     * Returns the start time of the node.
     * @return the start time as a string
     */
    public String getStartTime() {
        return getString(startTime);
    }

    /**
     * Sets the start time of the node.
     * @param startTime the start time as a string
     */
    public void setStartTime(String startTime) {
        putValue(NodeInfo.startTime, startTime);
    }

    /**
     * Returns the current status of the node (e.g., "active", "inactive").
     * @return the node status
     */
    public String getStatus() {
        return getString(status);
    }

    /**
     * Sets the current status of the node.
     * @param status the node status
     */
    public void setStatus(String status) {
        putValue(NodeInfo.status, status);
    }

    /**
     * Returns the heartbeat pulse interval of the node in milliseconds.
     * @return the heartbeat pulse interval
     */
    public Long getPulseInterval() {
        return getLong(pulseInterval);
    }

    /**
     * Returns the heartbeat pulse interval of the node with a fallback default value.
     * @param defaultValue the default interval to return if not specified
     * @return the heartbeat pulse interval
     */
    public long getPulseInterval(long defaultValue) {
        return getLong(pulseInterval, defaultValue);
    }

    /**
     * Sets the heartbeat pulse interval of the node in milliseconds.
     * @param pulseInterval the heartbeat pulse interval
     */
    public void setPulseInterval(Long pulseInterval) {
        putValue(NodeInfo.pulseInterval, pulseInterval);
    }

    /**
     * Returns the configuration for the communication endpoint.
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
     * Sets the configuration for the communication endpoint.
     * @param endpointConfig the endpoint configuration
     */
    public void setEndpointConfig(EndpointConfig endpointConfig) {
        putValue(endpoint, endpointConfig);
    }

    /**
     * Returns the security token of the node.
     * @return the security token
     */
    public String getToken() {
        return getString(token);
    }

    /**
     * Sets the security token of the node.
     * @param token the security token
     */
    public void setToken(String token) {
        putValue(NodeInfo.token, token);
    }

    /**
     * Returns whether the node has node management capability.
     * @return true if enabled, false otherwise
     */
    public Boolean hasNodeManager() {
        return getBoolean(hasNodeManager);
    }

    /**
     * Returns whether the node has node management capability with a fallback.
     * @param defaultValue the default value
     * @return true if enabled, false otherwise
     */
    public boolean hasNodeManager(boolean defaultValue) {
        return getBoolean(hasNodeManager, defaultValue);
    }

    /**
     * Sets whether the node has node management capability.
     * @param value true if enabled, false otherwise
     */
    public void setHasNodeManager(Boolean value) {
        putValue(NodeInfo.hasNodeManager, value);
    }

    /**
     * Returns whether the node has scheduler management capability.
     * @return true if enabled, false otherwise
     */
    public Boolean hasSchedulerManager() {
        return getBoolean(hasSchedulerManager);
    }

    /**
     * Returns whether the node has scheduler management capability with a fallback.
     * @param defaultValue the default value
     * @return true if enabled, false otherwise
     */
    public boolean hasSchedulerManager(boolean defaultValue) {
        return getBoolean(hasSchedulerManager, defaultValue);
    }

    /**
     * Sets whether the node has scheduler management capability.
     * @param value true if enabled, false otherwise
     */
    public void setHasSchedulerManager(Boolean value) {
        putValue(NodeInfo.hasSchedulerManager, value);
    }

    /**
     * Returns whether the node has command management capability.
     * @return true if enabled, false otherwise
     */
    public Boolean hasCommandManager() {
        return getBoolean(hasCommandManager);
    }

    /**
     * Returns whether the node has command management capability with a fallback.
     * @param defaultValue the default value
     * @return true if enabled, false otherwise
     */
    public boolean hasCommandManager(boolean defaultValue) {
        return getBoolean(hasCommandManager, defaultValue);
    }

    /**
     * Sets whether the node has command management capability.
     * @param value true if enabled, false otherwise
     */
    public void setHasCommandManager(Boolean value) {
        putValue(NodeInfo.hasCommandManager, value);
    }

    /**
     * Create a copy of the current NodeInfo with updated values from the new dynamic state of the node
     * @param nodeInfo the new dynamic state to update for the node
     * @return the updated NodeInfo instance
     */
    public NodeInfo copyWithUpdatedState(@NonNull NodeInfo nodeInfo) {
        NodeInfo newInfo = new NodeInfo();

        // keep static config
        newInfo.setId(getId());
        newInfo.setGroup(getGroup());
        newInfo.setTitle(getTitle());
        newInfo.setConsole(nodeInfo.isConsole() != null ? nodeInfo.isConsole() : isConsole());

        // update dynamic state
        newInfo.setHost(nodeInfo.getHost());
        newInfo.setPort(nodeInfo.getPort());
        newInfo.setStartTime(nodeInfo.getStartTime());
        newInfo.setStatus(nodeInfo.getStatus());
        newInfo.setPulseInterval(nodeInfo.getPulseInterval());
        newInfo.setEndpointConfig(nodeInfo.getEndpointConfig());
        newInfo.setToken(nodeInfo.getToken());
        newInfo.setHasNodeManager(nodeInfo.hasNodeManager());
        newInfo.setHasSchedulerManager(nodeInfo.hasSchedulerManager());
        newInfo.setHasCommandManager(nodeInfo.hasCommandManager());

        return newInfo;
    }

}

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
 * Defines the properties and metadata of a single node within the cluster.
 */
public class NodeInfo extends DefaultParameters {

    private static final ParameterKey id;
    private static final ParameterKey group;
    private static final ParameterKey title;
    private static final ParameterKey host;
    private static final ParameterKey port;
    private static final ParameterKey startTime;
    private static final ParameterKey status;
    private static final ParameterKey heartbeatInterval;
    private static final ParameterKey endpoint;
    private static final ParameterKey token;

    private static final ParameterKey[] parameterKeys;

    static {
        id = new ParameterKey("id", ValueType.STRING);
        group = new ParameterKey("group", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        host = new ParameterKey("host", ValueType.STRING);
        port = new ParameterKey("port", ValueType.INT);
        startTime = new ParameterKey("startTime", ValueType.STRING);
        status = new ParameterKey("status", ValueType.STRING);
        heartbeatInterval = new ParameterKey("heartbeatInterval", ValueType.LONG);
        endpoint = new ParameterKey("endpoint", EndpointConfig.class);
        token = new ParameterKey("token", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                id,
                group,
                title,
                host,
                port,
                startTime,
                status,
                heartbeatInterval,
                endpoint,
                token
        };
    }

    public NodeInfo() {
        super(parameterKeys);
    }

    public String getNodeId() {
        return getString(id);
    }

    public void setNodeId(String nodeId) {
        putValue(id, nodeId);
    }

    public String getGroup() {
        return getString(group);
    }

    public void setGroup(String group) {
        putValue(NodeInfo.group, group);
    }

    public String getTitle() {
        return getString(title);
    }

    public void setTitle(String title) {
        putValue(NodeInfo.title, title);
    }

    public String getHost() {
        return getString(host);
    }

    public void setHost(String host) {
        putValue(NodeInfo.host, host);
    }

    public Integer getPort() {
        return getInt(port);
    }

    public void setPort(Integer port) {
        putValue(NodeInfo.port, port);
    }

    public String getStartTime() {
        return getString(startTime);
    }

    public void setStartTime(String startTime) {
        putValue(NodeInfo.startTime, startTime);
    }

    public String getStatus() {
        return getString(status);
    }

    public void setStatus(String status) {
        putValue(NodeInfo.status, status);
    }

    public Long getHeartbeatInterval() {
        return getLong(heartbeatInterval);
    }

    public long getHeartbeatInterval(long defaultValue) {
        return getLong(heartbeatInterval, defaultValue);
    }

    public void setHeartbeatInterval(Long heartbeatInterval) {
        putValue(NodeInfo.heartbeatInterval, heartbeatInterval);
    }

    public EndpointConfig getEndpointConfig() {
        return getParameters(endpoint);
    }

    public EndpointConfig touchEndpointConfig() {
        return touchParameters(endpoint);
    }

    public void setEndpointConfig(EndpointConfig endpointConfig) {
        putValue(endpoint, endpointConfig);
    }

    public String getToken() {
        return getString(token);
    }

    public void setToken(String token) {
        putValue(NodeInfo.token, token);
    }

}

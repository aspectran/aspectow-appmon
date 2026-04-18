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
package com.aspectran.aspectow.appmon.engine.config;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

import java.util.List;

/**
 * Contains configuration for a monitored application instance.
 *
 * <p>An instance represents a logical application unit within a node,
 * aggregating various monitoring components such as events, metrics, and logs.</p>
 *
 * <p>Created: 2020/02/12</p>
 */
public class InstanceInfo extends DefaultParameters {

    private static final ParameterKey id;
    private static final ParameterKey title;
    private static final ParameterKey hidden;
    private static final ParameterKey event;
    private static final ParameterKey metric;
    private static final ParameterKey log;

    private static final ParameterKey[] parameterKeys;

    static {
        id = new ParameterKey("id", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        hidden = new ParameterKey("hidden", ValueType.BOOLEAN);
        event = new ParameterKey("events", new String[] {"event"}, EventInfo.class, true, true);
        metric = new ParameterKey("metrics", new String[] {"metric"}, MetricInfo.class, true, true);
        log = new ParameterKey("logs", new String[] {"log"}, LogInfo.class, true, true);

        parameterKeys = new ParameterKey[] {
                id,
                title,
                hidden,
                event,
                metric,
                log
        };
    }

    private String nodeId;

    /**
     * Instantiates a new InstanceInfo.
     */
    public InstanceInfo() {
        super(parameterKeys);
    }

    /**
     * Returns the identifier of the node to which this instance belongs.
     * @return the node identifier
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Sets the identifier of the node to which this instance belongs.
     * @param nodeId the node identifier
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Returns the unique identifier of the application instance.
     * @return the instance identifier
     */
    public String getInstanceId() {
        return getString(id);
    }

    /**
     * Sets the unique identifier of the application instance.
     * @param instanceId the instance identifier
     */
    public void setInstanceId(String instanceId) {
        putValue(InstanceInfo.id, instanceId);
    }

    /**
     * Returns the display title of the application instance.
     * @return the instance title
     */
    public String getTitle() {
        return getString(title);
    }

    /**
     * Sets the display title of the application instance.
     * @param name the instance title
     */
    public void setTitle(String name) {
        putValue(InstanceInfo.title, name);
    }

    /**
     * Returns whether the application instance should be hidden from the monitoring dashboard.
     * @return {@code true} if hidden, {@code false} otherwise
     */
    public boolean isHidden() {
        return getBoolean(hidden, false);
    }

    /**
     * Sets whether the application instance should be hidden from the monitoring dashboard.
     * @param hidden {@code true} to hide, {@code false} to show
     */
    public void setHidden(boolean hidden) {
        putValue(InstanceInfo.hidden, hidden);
    }

    /**
     * Returns the list of event configurations defined for this application instance.
     * @return a list of {@link EventInfo}
     */
    public List<EventInfo> getEventInfoList() {
        return getParametersList(event);
    }

    /**
     * Sets the list of event configurations defined for this application instance.
     * @param eventInfoList a list of {@link EventInfo}
     */
    public void setEventInfoList(List<EventInfo> eventInfoList) {
        putValue(InstanceInfo.event, eventInfoList);
    }

    /**
     * Returns the list of metric configurations defined for this application instance.
     * @return a list of {@link MetricInfo}
     */
    public List<MetricInfo> getMetricInfoList() {
        return getParametersList(metric);
    }

    /**
     * Sets the list of metric configurations defined for this application instance.
     * @param metricInfoList a list of {@link MetricInfo}
     */
    public void setMetricInfoList(List<MetricInfo> metricInfoList) {
        putValue(InstanceInfo.metric, metricInfoList);
    }

    /**
     * Returns the list of log configurations defined for this application instance.
     * @return a list of {@link LogInfo}
     */
    public List<LogInfo> getLogInfoList() {
        return getParametersList(log);
    }

    /**
     * Sets the list of log configurations defined for this application instance.
     * @param logInfoList a list of {@link LogInfo}
     */
    public void setLogInfoList(List<LogInfo> logInfoList) {
        putValue(InstanceInfo.log, logInfoList);
    }

}

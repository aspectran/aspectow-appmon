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
package com.aspectran.appmon.engine.config;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

import java.util.List;

/**
 * Contains detailed information about a monitored application instance.
 * An instance belongs to a domain and holds configurations for events, metrics, and logs.
 *
 * <p>Created: 2020/02/12</p>
 */
public class InstanceInfo extends DefaultParameters {

    private static final ParameterKey name;
    private static final ParameterKey title;
    private static final ParameterKey hidden;
    private static final ParameterKey event;
    private static final ParameterKey metric;
    private static final ParameterKey log;

    private static final ParameterKey[] parameterKeys;

    static {
        name = new ParameterKey("name", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        hidden = new ParameterKey("hidden", ValueType.BOOLEAN);
        event = new ParameterKey("events", new String[] {"event"}, EventInfo.class, true, true);
        metric = new ParameterKey("metrics", new String[] {"metric"}, MetricInfo.class, true, true);
        log = new ParameterKey("logs", new String[] {"log"}, LogInfo.class, true, true);

        parameterKeys = new ParameterKey[] {
                name,
                title,
                hidden,
                event,
                metric,
                log
        };
    }

    private String domainName;

    /**
     * Instantiates a new InstanceInfo.
     */
    public InstanceInfo() {
        super(parameterKeys);
    }

    /**
     * Gets the name of the domain this instance belongs to.
     * @return the domain name
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Sets the name of the domain this instance belongs to.
     * @param domainName the domain name
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * Gets the name of the instance.
     * @return the instance name
     */
    public String getName() {
        return getString(name);
    }

    /**
     * Sets the name of the instance.
     * @param name the instance name
     */
    public void setName(String name) {
        putValue(InstanceInfo.name, name);
    }

    /**
     * Gets the display title of the instance.
     * @return the instance title
     */
    public String getTitle() {
        return getString(title);
    }

    /**
     * Sets the display title of the instance.
     * @param name the instance title
     */
    public void setTitle(String name) {
        putValue(InstanceInfo.title, name);
    }

    /**
     * Checks if the instance should be hidden from the main view.
     * @return {@code true} if hidden, {@code false} otherwise
     */
    public boolean isHidden() {
        return getBoolean(hidden, false);
    }

    /**
     * Sets whether the instance should be hidden.
     * @param hidden {@code true} to hide, {@code false} to show
     */
    public void setHidden(boolean hidden) {
        putValue(InstanceInfo.hidden, hidden);
    }

    /**
     * Gets the list of event configurations for this instance.
     * @return a list of {@link EventInfo}
     */
    public List<EventInfo> getEventInfoList() {
        return getParametersList(event);
    }

    /**
     * Sets the list of event configurations for this instance.
     * @param eventInfoList a list of {@link EventInfo}
     */
    public void setEventInfoList(List<EventInfo> eventInfoList) {
        putValue(InstanceInfo.event, eventInfoList);
    }

    /**
     * Gets the list of metric configurations for this instance.
     * @return a list of {@link MetricInfo}
     */
    public List<MetricInfo> getMetricInfoList() {
        return getParametersList(metric);
    }

    /**
     * Sets the list of metric configurations for this instance.
     * @param metricInfoList a list of {@link MetricInfo}
     */
    public void setMetricInfoList(List<MetricInfo> metricInfoList) {
        putValue(InstanceInfo.metric, metricInfoList);
    }

    /**
     * Gets the list of log configurations for this instance.
     * @return a list of {@link LogInfo}
     */
    public List<LogInfo> getLogInfoList() {
        return getParametersList(log);
    }

    /**
     * Sets the list of log configurations for this instance.
     * @param logInfoList a list of {@link LogInfo}
     */
    public void setLogInfoList(List<LogInfo> logInfoList) {
        putValue(InstanceInfo.log, logInfoList);
    }

}

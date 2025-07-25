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

import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

import java.util.List;

/**
 * <p>Created: 2020/02/12</p>
 */
public class InstanceInfo extends AbstractParameters {

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

    public InstanceInfo() {
        super(parameterKeys);
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getName() {
        return getString(name);
    }

    public void setName(String name) {
        putValue(InstanceInfo.name, name);
    }

    public String getTitle() {
        return getString(title);
    }

    public void setTitle(String name) {
        putValue(InstanceInfo.title, name);
    }

    public boolean isHidden() {
        return getBoolean(hidden, false);
    }

    public void setHidden(boolean hidden) {
        putValue(InstanceInfo.hidden, hidden);
    }

    public List<EventInfo> getEventInfoList() {
        return getParametersList(event);
    }

    public void setEventInfoList(List<EventInfo> eventInfoList) {
        putValue(InstanceInfo.event, eventInfoList);
    }

    public List<MetricInfo> getMetricInfoList() {
        return getParametersList(metric);
    }

    public void setMetricInfoList(List<MetricInfo> metricInfoList) {
        putValue(InstanceInfo.metric, metricInfoList);
    }

    public List<LogInfo> getLogInfoList() {
        return getParametersList(log);
    }

    public void setLogInfoList(List<LogInfo> logInfoList) {
        putValue(InstanceInfo.log, logInfoList);
    }

}

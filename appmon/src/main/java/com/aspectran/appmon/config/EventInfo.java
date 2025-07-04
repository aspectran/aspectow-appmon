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
import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.apon.ValueType;

/**
 * <p>Created: 2020/02/12</p>
 */
public class EventInfo extends AbstractParameters {

    private static final ParameterKey name;
    private static final ParameterKey title;
    private static final ParameterKey description;
    private static final ParameterKey reader;
    private static final ParameterKey counter;
    private static final ParameterKey target;
    private static final ParameterKey parameters;
    private static final ParameterKey sampleInterval;
    private static final ParameterKey leading;

    private static final ParameterKey[] parameterKeys;

    static {
        name = new ParameterKey("name", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        description = new ParameterKey("description", ValueType.STRING);
        reader = new ParameterKey("reader", ValueType.STRING);
        counter = new ParameterKey("counter", ValueType.STRING);
        target = new ParameterKey("target", ValueType.STRING);
        parameters = new ParameterKey("parameters", ValueType.PARAMETERS);
        sampleInterval = new ParameterKey("sampleInterval", ValueType.INT);
        leading = new ParameterKey("leading", ValueType.BOOLEAN);

        parameterKeys = new ParameterKey[] {
                name,
                title,
                description,
                reader,
                counter,
                target,
                parameters,
                sampleInterval,
                leading
        };
    }

    private String domainName;

    private String instanceName;

    public EventInfo() {
        super(parameterKeys);
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getName() {
        return getString(name);
    }

    public void setName(String name) {
        putValue(EventInfo.name, name);
    }

    public String getTitle() {
        return getString(title);
    }

    public void setTitle(String title) {
        putValue(EventInfo.title, title);
    }

    public String getDescription() {
        return getString(description);
    }

    public void setDescription(String description) {
        putValue(EventInfo.description, description);
    }

    public String getReader() {
        return getString(reader);
    }

    public void setReader(String reader) {
        putValue(EventInfo.reader, reader);
    }

    public boolean hasReader() {
        return hasValue(EventInfo.reader);
    }

    public String getCounter() {
        return getString(counter);
    }

    public void setCounter(String counter) {
        putValue(EventInfo.counter, counter);
    }

    public boolean hasCounter() {
        return hasValue(EventInfo.counter);
    }

    public String getTarget() {
        return getString(target);
    }

    public void setTarget(String target) {
        putValue(EventInfo.target, target);
    }

    public boolean hasParameters() {
        return hasValue(parameters);
    }

    public Parameters getParameters() {
        return getParameters(parameters);
    }

    public void setParameters(Parameters parameters) {
        putValue(EventInfo.parameters, parameters);
    }

    public int getSampleInterval() {
        return getInt(sampleInterval, 0);
    }

    public void setSampleInterval(int sampleInterval) {
        putValue(EventInfo.sampleInterval, sampleInterval);
    }

    public boolean isLeading() {
        return getBoolean(leading, false);
    }

    public Boolean getLeading() {
        return getBoolean(leading);
    }

    public void setLeading(boolean leading) {
        putValue(EventInfo.leading, leading);
    }

    public void validateRequiredParameters() {
        Assert.hasLength(getString(name), "Missing value of required parameter: " + getQualifiedName(name));
    }

    public void checkHasTargetParameter() {
        Assert.hasLength(getString(target), "Missing value of required parameter: " + getQualifiedName(target));
    }

}

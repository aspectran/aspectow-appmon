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
public class MetricInfo extends AbstractParameters {

    private static final ParameterKey name;
    private static final ParameterKey title;
    private static final ParameterKey description;
    private static final ParameterKey reader;
    private static final ParameterKey target;
    private static final ParameterKey parameters;
    private static final ParameterKey sampleInterval;
    private static final ParameterKey heading;
    private static final ParameterKey format;

    private static final ParameterKey[] parameterKeys;

    static {
        name = new ParameterKey("name", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        description = new ParameterKey("description", ValueType.STRING);
        reader = new ParameterKey("reader", ValueType.STRING);
        target = new ParameterKey("target", ValueType.STRING);
        parameters = new ParameterKey("parameters", ValueType.PARAMETERS);
        sampleInterval = new ParameterKey("sampleInterval", ValueType.INT);
        heading = new ParameterKey("heading", ValueType.BOOLEAN);
        format = new ParameterKey("format", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                name,
                title,
                description,
                reader,
                target,
                parameters,
                sampleInterval,
                heading,
                format
        };
    }

    private String domainName;

    private String instanceName;

    public MetricInfo() {
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
        putValue(MetricInfo.name, name);
    }

    public String getTitle() {
        return getString(title);
    }

    public void setTitle(String title) {
        putValue(MetricInfo.title, title);
    }

    public String getDescription() {
        return getString(description);
    }

    public void setDescription(String description) {
        putValue(MetricInfo.description, description);
    }

    public String getReader() {
        return getString(reader);
    }

    public void setReader(String reader) {
        putValue(MetricInfo.reader, reader);
    }

    public boolean hasReader() {
        return hasValue(MetricInfo.reader);
    }

    public String getTarget() {
        return getString(target);
    }

    public void setTarget(String target) {
        putValue(MetricInfo.target, target);
    }

    public boolean hasParameters() {
        return hasValue(parameters);
    }

    public Parameters getParameters() {
        return getParameters(parameters);
    }

    public void setParameters(Parameters parameters) {
        putValue(MetricInfo.parameters, parameters);
    }

    public int getSampleInterval() {
        return getInt(sampleInterval, 0);
    }

    public void setSampleInterval(int sampleInterval) {
        putValue(MetricInfo.sampleInterval, sampleInterval);
    }

    public boolean isHeading() {
        return getBoolean(heading, false);
    }

    public Boolean getHeading() {
        return getBoolean(heading);
    }

    public void setHeading(boolean heading) {
        putValue(MetricInfo.heading, heading);
    }

    public boolean hasFormat() {
        return hasValue(format);
    }

    public String getFormat() {
        return getString(format);
    }

    public void setFormat(String format) {
        putValue(MetricInfo.format, format);
    }

    public void validateRequiredParameters() {
        Assert.hasLength(getString(name), "Missing value of required parameter: " + getQualifiedName(name));
        Assert.hasLength(getString(title), "Missing value of required parameter: " + getQualifiedName(title));
        Assert.hasLength(getString(reader), "Missing value of required parameter: " + getQualifiedName(reader));
    }

    public void checkHasTargetParameter() {
        Assert.hasLength(getString(target), "Missing value of required parameter: " + getQualifiedName(target));
    }

}

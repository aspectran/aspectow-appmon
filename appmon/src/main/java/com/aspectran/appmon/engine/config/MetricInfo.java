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

import com.aspectran.utils.Assert;
import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.apon.ValueType;

/**
 * Contains configuration for a specific metric to be collected.
 * This includes details about the metric's reader, target, and formatting options.
 *
 * <p>Created: 2020/02/12</p>
 */
public class MetricInfo extends DefaultParameters {

    private static final ParameterKey name;
    private static final ParameterKey title;
    private static final ParameterKey description;
    private static final ParameterKey reader;
    private static final ParameterKey target;
    private static final ParameterKey parameters;
    private static final ParameterKey sampleInterval;
    private static final ParameterKey exportInterval;
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
        exportInterval = new ParameterKey("exportInterval", ValueType.INT);
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
                exportInterval,
                heading,
                format
        };
    }

    private String domainName;

    private String instanceName;

    /**
     * Instantiates a new MetricInfo.
     */
    public MetricInfo() {
        super(parameterKeys);
    }

    /**
     * Gets the name of the domain this metric belongs to.
     * @return the domain name
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Sets the name of the domain this metric belongs to.
     * @param domainName the domain name
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * Gets the name of the instance this metric belongs to.
     * @return the instance name
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Sets the name of the instance this metric belongs to.
     * @param instanceName the instance name
     */
    void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * Gets the name of the metric.
     * @return the metric name
     */
    public String getName() {
        return getString(name);
    }

    /**
     * Sets the name of the metric.
     * @param name the metric name
     */
    public void setName(String name) {
        putValue(MetricInfo.name, name);
    }

    /**
     * Gets the display title of the metric.
     * @return the metric title
     */
    public String getTitle() {
        return getString(title);
    }

    /**
     * Sets the display title of the metric.
     * @param title the metric title
     */
    public void setTitle(String title) {
        putValue(MetricInfo.title, title);
    }

    /**
     * Gets the description of the metric.
     * @return the metric description
     */
    public String getDescription() {
        return getString(description);
    }

    /**
     * Sets the description of the metric.
     * @param description the metric description
     */
    public void setDescription(String description) {
        putValue(MetricInfo.description, description);
    }

    /**
     * Gets the identifier of the metric reader bean.
     * @return the metric reader bean ID
     */
    public String getReader() {
        return getString(reader);
    }

    /**
     * Sets the identifier of the metric reader bean.
     * @param reader the metric reader bean ID
     */
    public void setReader(String reader) {
        putValue(MetricInfo.reader, reader);
    }

    /**
     * Checks if a metric reader is configured.
     * @return {@code true} if a reader is set, {@code false} otherwise
     */
    public boolean hasReader() {
        return hasValue(MetricInfo.reader);
    }

    /**
     * Gets the target for exporting metric data.
     * @return the export target
     */
    public String getTarget() {
        return getString(target);
    }

    /**
     * Sets the target for exporting metric data.
     * @param target the export target
     */
    public void setTarget(String target) {
        putValue(MetricInfo.target, target);
    }

    /**
     * Checks if there are additional parameters for the metric.
     * @return {@code true} if parameters exist, {@code false} otherwise
     */
    public boolean hasParameters() {
        return hasValue(parameters);
    }

    /**
     * Gets additional parameters for the metric.
     * @return the parameters
     */
    public Parameters getParameters() {
        return getParameters(parameters);
    }

    /**
     * Sets additional parameters for the metric.
     * @param parameters the parameters
     */
    public void setParameters(Parameters parameters) {
        putValue(MetricInfo.parameters, parameters);
    }

    /**
     * Gets the sample interval in seconds for this metric.
     * @return the sample interval
     */
    public int getSampleInterval() {
        return getInt(sampleInterval, 0);
    }

    /**
     * Sets the sample interval in seconds for this metric.
     * @param sampleInterval the sample interval
     */
    public void setSampleInterval(int sampleInterval) {
        putValue(MetricInfo.sampleInterval, sampleInterval);
    }

    /**
     * Gets the export interval in seconds for this metric.
     * @return the export interval
     */
    public int getExportInterval() {
        return getInt(exportInterval, 0);
    }

    /**
     * Sets the export interval in seconds for this metric.
     * @param exportInterval the export interval
     */
    public void setExportInterval(int exportInterval) {
        putValue(MetricInfo.exportInterval, exportInterval);
    }

    /**
     * Checks if a heading should be included in the metric output.
     * @return {@code true} to include a heading, {@code false} otherwise
     */
    public boolean isHeading() {
        return getBoolean(heading, false);
    }

    /**
     * Gets the raw boolean value for the heading.
     * @return the heading flag, or {@code null} if not set
     */
    public Boolean getHeading() {
        return getBoolean(heading);
    }

    /**
     * Sets whether to include a heading in the metric output.
     * @param heading {@code true} to include a heading, {@code false} otherwise
     */
    public void setHeading(boolean heading) {
        putValue(MetricInfo.heading, heading);
    }

    /**
     * Checks if a format string is configured for the metric output.
     * @return {@code true} if a format is set, {@code false} otherwise
     */
    public boolean hasFormat() {
        return hasValue(format);
    }

    /**
     * Gets the format string for the metric output.
     * @return the format string
     */
    public String getFormat() {
        return getString(format);
    }

    /**
     * Sets the format string for the metric output.
     * @param format the format string
     */
    public void setFormat(String format) {
        putValue(MetricInfo.format, format);
    }

    /**
     * Validates that all required parameters are present.
     */
    public void validateRequiredParameters() {
        Assert.hasLength(getString(name), "Missing value of required parameter: " + getQualifiedName(name));
        Assert.hasLength(getString(title), "Missing value of required parameter: " + getQualifiedName(title));
        Assert.hasLength(getString(reader), "Missing value of required parameter: " + getQualifiedName(reader));
    }

    /**
     * Checks that the target parameter is present.
     */
    public void checkHasTargetParameter() {
        Assert.hasLength(getString(target), "Missing value of required parameter: " + getQualifiedName(target));
    }

}

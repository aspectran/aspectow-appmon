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

import com.aspectran.utils.Assert;
import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.apon.ValueType;

/**
 * Contains configuration for a specific metric to be collected and analyzed.
 *
 * <p>A metric provides quantitative measurements of system or application performance
 * at specific points in time.</p>
 *
 * <p>Created: 2020/02/12</p>
 */
public class MetricInfo extends DefaultParameters {

    private static final ParameterKey id;
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
        id = new ParameterKey("id", ValueType.STRING);
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
                id,
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

    private String nodeId;

    private String appId;

    /**
     * Instantiates a new MetricInfo.
     */
    public MetricInfo() {
        super(parameterKeys);
    }

    /**
     * Returns the identifier of the node to which this metric belongs.
     * @return the node identifier
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Sets the identifier of the node to which this metric belongs.
     * @param nodeId the node identifier
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Returns the identifier of the application to which this metric belongs.
     * @return the app identifier
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Sets the identifier of the application to which this metric belongs.
     * @param appId the app identifier
     */
    void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * Returns the unique identifier of the metric.
     * @return the metric identifier
     */
    public String getMetricId() {
        return getString(id);
    }

    /**
     * Sets the unique identifier of the metric.
     * @param name the metric identifier
     */
    public void setMetricId(String name) {
        putValue(MetricInfo.id, name);
    }

    /**
     * Returns the display title for the metric.
     * @return the metric title
     */
    public String getTitle() {
        return getString(title);
    }

    /**
     * Sets the display title for the metric.
     * @param title the metric title
     */
    public void setTitle(String title) {
        putValue(MetricInfo.title, title);
    }

    /**
     * Returns the description of the metric.
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
     * Returns the bean identifier of the reader responsible for reading metric data.
     * @return the reader bean identifier
     */
    public String getReader() {
        return getString(reader);
    }

    /**
     * Sets the bean identifier of the reader responsible for reading metric data.
     * @param reader the reader bean identifier
     */
    public void setReader(String reader) {
        putValue(MetricInfo.reader, reader);
    }

    /**
     * Returns whether a metric reader is configured.
     * @return {@code true} if a reader is set, {@code false} otherwise
     */
    public boolean hasReader() {
        return hasValue(MetricInfo.reader);
    }

    /**
     * Returns the target destination for exporting or analyzing metric data.
     * @return the data export target
     */
    public String getTarget() {
        return getString(target);
    }

    /**
     * Sets the target destination for exporting or analyzing metric data.
     * @param target the data export target
     */
    public void setTarget(String target) {
        putValue(MetricInfo.target, target);
    }

    /**
     * Returns whether additional parameters are configured for this metric.
     * @return {@code true} if parameters exist, {@code false} otherwise
     */
    public boolean hasParameters() {
        return hasValue(parameters);
    }

    /**
     * Returns additional parameters for the metric configuration.
     * @return the additional parameters
     */
    public Parameters getParameters() {
        return getParameters(parameters);
    }

    /**
     * Sets additional parameters for the metric configuration.
     * @param parameters the additional parameters
     */
    public void setParameters(Parameters parameters) {
        putValue(MetricInfo.parameters, parameters);
    }

    /**
     * Returns the interval (in milliseconds) at which the metric should be sampled.
     * @return the sample interval
     */
    public int getSampleInterval() {
        return getInt(sampleInterval, 0);
    }

    /**
     * Sets the interval (in milliseconds) at which the metric should be sampled.
     * @param sampleInterval the sample interval
     */
    public void setSampleInterval(int sampleInterval) {
        putValue(MetricInfo.sampleInterval, sampleInterval);
    }

    /**
     * Returns the interval (in milliseconds) at which the collected metric data should be exported.
     * @return the export interval
     */
    public int getExportInterval() {
        return getInt(exportInterval, 0);
    }

    /**
     * Sets the interval (in milliseconds) at which the collected metric data should be exported.
     * @param exportInterval the export interval
     */
    public void setExportInterval(int exportInterval) {
        putValue(MetricInfo.exportInterval, exportInterval);
    }

    /**
     * Returns whether a heading should be included when displaying or exporting the metric data.
     * @return {@code true} to include a heading, {@code false} otherwise
     */
    public boolean isHeading() {
        return getBoolean(heading, false);
    }

    /**
     * Returns the raw boolean value for the heading configuration.
     * @return the heading flag, or {@code null} if not set
     */
    public Boolean getHeading() {
        return getBoolean(heading);
    }

    /**
     * Sets whether a heading should be included when displaying or exporting the metric data.
     * @param heading {@code true} to include a heading, {@code false} otherwise
     */
    public void setHeading(boolean heading) {
        putValue(MetricInfo.heading, heading);
    }

    /**
     * Returns whether a format string is configured for the metric output.
     * @return {@code true} if a format is set, {@code false} otherwise
     */
    public boolean hasFormat() {
        return hasValue(format);
    }

    /**
     * Returns the format string used for displaying or exporting the metric data.
     * @return the format string
     */
    public String getFormat() {
        return getString(format);
    }

    /**
     * Sets the format string used for displaying or exporting the metric data.
     * @param format the format string
     */
    public void setFormat(String format) {
        putValue(MetricInfo.format, format);
    }

    /**
     * Validates that all required configuration parameters for the metric are present.
     * @throws IllegalArgumentException if any required parameter is missing
     */
    public void validateRequiredParameters() {
        Assert.hasLength(getString(id), "Missing value of required parameter: " + getQualifiedName(id));
        Assert.hasLength(getString(title), "Missing value of required parameter: " + getQualifiedName(title));
        Assert.hasLength(getString(reader), "Missing value of required parameter: " + getQualifiedName(reader));
    }

    /**
     * Validates that the target destination parameter is present.
     * @throws IllegalArgumentException if the target parameter is missing
     */
    public void checkHasTargetParameter() {
        Assert.hasLength(getString(target), "Missing value of required parameter: " + getQualifiedName(target));
    }

}

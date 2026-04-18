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
 * Contains configuration for a specific event to be monitored.
 *
 * <p>An event represents a discrete occurrence or a continuous activity within an application
 * instance that can be tracked, counted, and analyzed.</p>
 *
 * <p>Created: 2020/02/12</p>
 */
public class EventInfo extends DefaultParameters {

    private static final ParameterKey id;
    private static final ParameterKey title;
    private static final ParameterKey description;
    private static final ParameterKey reader;
    private static final ParameterKey counter;
    private static final ParameterKey target;
    private static final ParameterKey parameters;
    private static final ParameterKey sampleInterval;

    private static final ParameterKey[] parameterKeys;

    static {
        id = new ParameterKey("id", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        description = new ParameterKey("description", ValueType.STRING);
        reader = new ParameterKey("reader", ValueType.STRING);
        counter = new ParameterKey("counter", ValueType.STRING);
        target = new ParameterKey("target", ValueType.STRING);
        parameters = new ParameterKey("parameters", ValueType.PARAMETERS);
        sampleInterval = new ParameterKey("sampleInterval", ValueType.INT);

        parameterKeys = new ParameterKey[] {
                id,
                title,
                description,
                reader,
                counter,
                target,
                parameters,
                sampleInterval
        };
    }

    private String nodeId;

    private String instanceId;

    /**
     * Instantiates a new EventInfo.
     */
    public EventInfo() {
        super(parameterKeys);
    }

    /**
     * Returns the identifier of the node to which this event belongs.
     * @return the node identifier
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Sets the identifier of the node to which this event belongs.
     * @param nodeId the node identifier
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Returns the identifier of the application instance to which this event belongs.
     * @return the instance identifier
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the identifier of the application instance to which this event belongs.
     * @param instanceId the instance identifier
     */
    void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Returns the unique identifier of the event.
     * @return the event identifier
     */
    public String getEventId() {
        return getString(id);
    }

    /**
     * Sets the unique identifier of the event.
     * @param name the event identifier
     */
    public void setEventId(String name) {
        putValue(EventInfo.id, name);
    }

    /**
     * Returns the display title of the event.
     * @return the event title
     */
    public String getTitle() {
        return getString(title);
    }

    /**
     * Sets the display title of the event.
     * @param title the event title
     */
    public void setTitle(String title) {
        putValue(EventInfo.title, title);
    }

    /**
     * Returns the description of the event.
     * @return the event description
     */
    public String getDescription() {
        return getString(description);
    }

    /**
     * Sets the description of the event.
     * @param description the event description
     */
    public void setDescription(String description) {
        putValue(EventInfo.description, description);
    }

    /**
     * Returns the bean identifier of the reader responsible for reading event data.
     * @return the reader bean identifier
     */
    public String getReader() {
        return getString(reader);
    }

    /**
     * Sets the bean identifier of the reader responsible for reading event data.
     * @param reader the reader bean identifier
     */
    public void setReader(String reader) {
        putValue(EventInfo.reader, reader);
    }

    /**
     * Returns whether an event reader is configured.
     * @return {@code true} if a reader is set, {@code false} otherwise
     */
    public boolean hasReader() {
        return hasValue(EventInfo.reader);
    }

    /**
     * Returns the bean identifier of the counter responsible for tracking event occurrences.
     * @return the counter bean identifier
     */
    public String getCounter() {
        return getString(counter);
    }

    /**
     * Sets the bean identifier of the counter responsible for tracking event occurrences.
     * @param counter the counter bean identifier
     */
    public void setCounter(String counter) {
        putValue(EventInfo.counter, counter);
    }

    /**
     * Returns whether an event counter is configured.
     * @return {@code true} if a counter is set, {@code false} otherwise
     */
    public boolean hasCounter() {
        return hasValue(EventInfo.counter);
    }

    /**
     * Returns the target destination for exporting or analyzing event data.
     * @return the data export target
     */
    public String getTarget() {
        return getString(target);
    }

    /**
     * Sets the target destination for exporting or analyzing event data.
     * @param target the data export target
     */
    public void setTarget(String target) {
        putValue(EventInfo.target, target);
    }

    /**
     * Returns whether additional parameters are configured for this event.
     * @return {@code true} if parameters exist, {@code false} otherwise
     */
    public boolean hasParameters() {
        return hasValue(parameters);
    }

    /**
     * Returns additional parameters for the event configuration.
     * @return the additional parameters
     */
    public Parameters getParameters() {
        return getParameters(parameters);
    }

    /**
     * Sets additional parameters for the event configuration.
     * @param parameters the additional parameters
     */
    public void setParameters(Parameters parameters) {
        putValue(EventInfo.parameters, parameters);
    }

    /**
     * Returns the interval (in milliseconds) at which the event should be sampled.
     * @return the sample interval
     */
    public int getSampleInterval() {
        return getInt(sampleInterval, 0);
    }

    /**
     * Sets the interval (in milliseconds) at which the event should be sampled.
     * @param sampleInterval the sample interval
     */
    public void setSampleInterval(int sampleInterval) {
        putValue(EventInfo.sampleInterval, sampleInterval);
    }

    /**
     * Validates that all required configuration parameters for the event are present.
     * @throws IllegalArgumentException if any required parameter is missing
     */
    public void validateRequiredParameters() {
        Assert.hasLength(getString(id), "Missing value of required parameter: " + getQualifiedName(id));
        Assert.hasLength(getString(target), "Missing value of required parameter: " + getQualifiedName(target));
    }

}

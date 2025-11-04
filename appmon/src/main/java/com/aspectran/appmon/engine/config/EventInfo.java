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
import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.apon.ValueType;

/**
 * Contains configuration for a specific event to be monitored.
 * This includes details about the event's reader, counter, and target for data export.
 *
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

        parameterKeys = new ParameterKey[] {
                name,
                title,
                description,
                reader,
                counter,
                target,
                parameters,
                sampleInterval
        };
    }

    private String domainName;

    private String instanceName;

    /**
     * Instantiates a new EventInfo.
     */
    public EventInfo() {
        super(parameterKeys);
    }

    /**
     * Gets the name of the domain this event belongs to.
     * @return the domain name
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Sets the name of the domain this event belongs to.
     * @param domainName the domain name
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * Gets the name of the instance this event belongs to.
     * @return the instance name
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Sets the name of the instance this event belongs to.
     * @param instanceName the instance name
     */
    void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * Gets the name of the event.
     * @return the event name
     */
    public String getName() {
        return getString(name);
    }

    /**
     * Sets the name of the event.
     * @param name the event name
     */
    public void setName(String name) {
        putValue(EventInfo.name, name);
    }

    /**
     * Gets the display title of the event.
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
     * Gets the description of the event.
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
     * Gets the identifier of the event reader bean.
     * @return the event reader bean ID
     */
    public String getReader() {
        return getString(reader);
    }

    /**
     * Sets the identifier of the event reader bean.
     * @param reader the event reader bean ID
     */
    public void setReader(String reader) {
        putValue(EventInfo.reader, reader);
    }

    /**
     * Checks if an event reader is configured.
     * @return {@code true} if a reader is set, {@code false} otherwise
     */
    public boolean hasReader() {
        return hasValue(EventInfo.reader);
    }

    /**
     * Gets the identifier of the event counter bean.
     * @return the event counter bean ID
     */
    public String getCounter() {
        return getString(counter);
    }

    /**
     * Sets the identifier of the event counter bean.
     * @param counter the event counter bean ID
     */
    public void setCounter(String counter) {
        putValue(EventInfo.counter, counter);
    }

    /**
     * Checks if an event counter is configured.
     * @return {@code true} if a counter is set, {@code false} otherwise
     */
    public boolean hasCounter() {
        return hasValue(EventInfo.counter);
    }

    /**
     * Gets the target for exporting event data.
     * @return the export target
     */
    public String getTarget() {
        return getString(target);
    }

    /**
     * Sets the target for exporting event data.
     * @param target the export target
     */
    public void setTarget(String target) {
        putValue(EventInfo.target, target);
    }

    /**
     * Checks if there are additional parameters for the event.
     * @return {@code true} if parameters exist, {@code false} otherwise
     */
    public boolean hasParameters() {
        return hasValue(parameters);
    }

    /**
     * Gets additional parameters for the event.
     * @return the parameters
     */
    public Parameters getParameters() {
        return getParameters(parameters);
    }

    /**
     * Sets additional parameters for the event.
     * @param parameters the parameters
     */
    public void setParameters(Parameters parameters) {
        putValue(EventInfo.parameters, parameters);
    }

    /**
     * Gets the sample interval in seconds for this event.
     * @return the sample interval
     */
    public int getSampleInterval() {
        return getInt(sampleInterval, 0);
    }

    /**
     * Sets the sample interval in seconds for this event.
     * @param sampleInterval the sample interval
     */
    public void setSampleInterval(int sampleInterval) {
        putValue(EventInfo.sampleInterval, sampleInterval);
    }

    /**
     * Validates that all required parameters are present.
     */
    public void validateRequiredParameters() {
        Assert.hasLength(getString(name), "Missing value of required parameter: " + getQualifiedName(name));
        Assert.hasLength(getString(target), "Missing value of required parameter: " + getQualifiedName(target));
    }

}

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

/**
 * Contains information about a monitored domain.
 * A domain represents a logical grouping of application instances.
 *
 * <p>Created: 2020/02/12</p>
 */
public class DomainInfo extends DefaultParameters {

    private static final ParameterKey name;
    private static final ParameterKey title;
    private static final ParameterKey endpoint;

    private static final ParameterKey[] parameterKeys;

    static {
        name = new ParameterKey("name", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        endpoint = new ParameterKey("endpoint", EndpointInfo.class);

        parameterKeys = new ParameterKey[] {
                name,
                title,
                endpoint
        };
    }

    /**
     * Instantiates a new DomainInfo.
     */
    public DomainInfo() {
        super(parameterKeys);
    }

    /**
     * Gets the name of the domain.
     * @return the domain name
     */
    public String getName() {
        return getString(name);
    }

    /**
     * Sets the name of the domain.
     * @param name the domain name
     */
    public void setName(String name) {
        putValue(DomainInfo.name, name);
    }

    /**
     * Gets the display title of the domain.
     * @return the domain title
     */
    public String getTitle() {
        return getString(title);
    }

    /**
     * Sets the display title of the domain.
     * @param title the domain title
     */
    public void setTitle(String title) {
        putValue(DomainInfo.title, title);
    }

    /**
     * Gets the endpoint information for the domain.
     * @return the endpoint information
     */
    public EndpointInfo getEndpointInfo() {
        return getParameters(endpoint);
    }

    /**
     * Sets the endpoint information for the domain.
     * @param endpointInfo the endpoint information
     */
    public void setEndpointInfo(EndpointInfo endpointInfo) {
        putValue(DomainInfo.endpoint, endpointInfo);
    }

}

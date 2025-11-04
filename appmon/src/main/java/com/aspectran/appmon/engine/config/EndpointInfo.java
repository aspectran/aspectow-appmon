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

import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Contains information about a service endpoint.
 * This typically includes the mode of operation (e.g., 'polling', 'websocket') and the URL.
 *
 * <p>Created: 2025-02-13</p>
 */
public class EndpointInfo extends AbstractParameters {

    private static final ParameterKey mode;
    private static final ParameterKey url;

    private static final ParameterKey[] parameterKeys;

    static {
        mode = new ParameterKey("mode", ValueType.STRING);
        url = new ParameterKey("url", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
            mode,
            url
        };
    }

    /**
     * Instantiates a new EndpointInfo.
     */
    public EndpointInfo() {
        super(parameterKeys);
    }

    /**
     * Gets the mode of the endpoint (e.g., 'polling', 'websocket').
     * @return the endpoint mode
     */
    public String getMode() {
        return getString(EndpointInfo.mode);
    }

    /**
     * Sets the mode of the endpoint.
     * @param mode the endpoint mode
     */
    public void setMode(String mode) {
        putValue(EndpointInfo.mode, mode);
    }

    /**
     * Gets the URL of the endpoint.
     * @return the endpoint URL
     */
    public String getUrl() {
        return getString(url);
    }

    /**
     * Sets the URL of the endpoint.
     * @param url the endpoint URL
     */
    public void setUrl(String url) {
        putValue(EndpointInfo.url, url);
    }

}

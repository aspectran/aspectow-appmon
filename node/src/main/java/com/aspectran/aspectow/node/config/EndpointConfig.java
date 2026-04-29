/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.node.config;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Defines the configuration for a communication endpoint, including
 * the preferred communication mode and the base path for the endpoint.
 *
 * <p>Created: 2026-04-18</p>
 */
public class EndpointConfig extends DefaultParameters {

    private static final ParameterKey mode;
    private static final ParameterKey path;

    private static final ParameterKey[] parameterKeys;

    static {
        mode = new ParameterKey("mode", ValueType.STRING);
        path = new ParameterKey("path", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                mode,
                path
        };
    }

    public EndpointConfig() {
        super(parameterKeys);
    }

    /**
     * Returns the preferred communication mode (e.g., "auto", "websocket", "polling").
     * @return the communication mode
     */
    public String getMode() {
        return getString(mode);
    }

    /**
     * Sets the preferred communication mode.
     * @param mode the communication mode
     */
    public void setMode(String mode) {
        putValue(EndpointConfig.mode, mode);
    }

    /**
     * Returns the base path of the communication endpoint.
     * @return the endpoint path
     */
    public String getPath() {
        return getString(path);
    }

    /**
     * Sets the base path of the communication endpoint.
     * @param path the endpoint path
     */
    public void setPath(String path) {
        putValue(EndpointConfig.path, path);
    }

}

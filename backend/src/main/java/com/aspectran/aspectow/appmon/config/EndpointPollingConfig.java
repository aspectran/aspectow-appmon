/*
 * Copyright (c) 2020-2025 The Aspectran Project
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
package com.aspectran.aspectow.appmon.config;

import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

public class EndpointPollingConfig extends AbstractParameters {

    private static final ParameterKey pollingInterval;
    private static final ParameterKey sessionTimeout;
    private static final ParameterKey initialBufferSize;
    private static final ParameterKey enabled;

    private static final ParameterKey[] parameterKeys;

    static {
        pollingInterval = new ParameterKey("pollingInterval", ValueType.INT);
        sessionTimeout = new ParameterKey("sessionTimeout", ValueType.INT);
        initialBufferSize = new ParameterKey("initialBufferSize", ValueType.INT);
        enabled = new ParameterKey("enabled", ValueType.BOOLEAN);

        parameterKeys = new ParameterKey[] {
                pollingInterval,
                sessionTimeout,
                initialBufferSize,
                enabled
        };
    }

    public EndpointPollingConfig() {
        super(parameterKeys);
    }

    public int getPollingInterval() {
        return getInt(pollingInterval, 0);
    }

    public void setPollingInterval(int pollingInterval) {
        putValue(EndpointPollingConfig.pollingInterval, pollingInterval);
    }

    public int getSessionTimeout() {
        return getInt(sessionTimeout, 0);
    }

    public void setSessionTimeout(int sessionTimeout) {
        putValue(EndpointPollingConfig.sessionTimeout, sessionTimeout);
    }

    public int getInitialBufferSize() {
        return getInt(initialBufferSize, 0);
    }

    public void setInitialBufferSize(int initialBufferSize) {
        putValue(EndpointPollingConfig.initialBufferSize, initialBufferSize);
    }

    public boolean isEnabled() {
        return getBoolean(enabled, true);
    }

    public void setEnabled(boolean enabled) {
        putValue(EndpointPollingConfig.enabled, enabled);
    }

}

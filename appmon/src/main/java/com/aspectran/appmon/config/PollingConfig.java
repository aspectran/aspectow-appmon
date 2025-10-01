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

import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Configuration for the polling service.
 * This class holds settings that control the behavior of the polling mechanism,
 * such as intervals and timeouts.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class PollingConfig extends AbstractParameters {

    private static final ParameterKey pollingInterval;
    private static final ParameterKey sessionTimeout;
    private static final ParameterKey initialBufferSize;

    private static final ParameterKey[] parameterKeys;

    static {
        pollingInterval = new ParameterKey("pollingInterval", ValueType.INT);
        sessionTimeout = new ParameterKey("sessionTimeout", ValueType.INT);
        initialBufferSize = new ParameterKey("initialBufferSize", ValueType.INT);

        parameterKeys = new ParameterKey[] {
                pollingInterval,
                sessionTimeout,
                initialBufferSize
        };
    }

    public PollingConfig() {
        super(parameterKeys);
    }

    /**
     * Gets the polling interval in seconds.
     * @return the polling interval
     */
    public int getPollingInterval() {
        return getInt(pollingInterval, 0);
    }

    /**
     * Sets the polling interval in seconds.
     * @param pollingInterval the polling interval
     */
    public void setPollingInterval(int pollingInterval) {
        putValue(PollingConfig.pollingInterval, pollingInterval);
    }

    /**
     * Gets the session timeout in seconds.
     * @return the session timeout
     */
    public int getSessionTimeout() {
        return getInt(sessionTimeout, 0);
    }

    /**
     * Sets the session timeout in seconds.
     * @param sessionTimeout the session timeout
     */
    public void setSessionTimeout(int sessionTimeout) {
        putValue(PollingConfig.sessionTimeout, sessionTimeout);
    }

    /**
     * Gets the initial buffer size for polling messages.
     * @return the initial buffer size
     */
    public int getInitialBufferSize() {
        return getInt(initialBufferSize, 0);
    }

    /**
     * Sets the initial buffer size for polling messages.
     * @param initialBufferSize the initial buffer size
     */
    public void setInitialBufferSize(int initialBufferSize) {
        putValue(PollingConfig.initialBufferSize, initialBufferSize);
    }

}

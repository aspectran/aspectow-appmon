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

    public EndpointInfo() {
        super(parameterKeys);
    }

    public String getMode() {
        return getString(EndpointInfo.mode);
    }

    public void setMode(String mode) {
        putValue(EndpointInfo.mode, mode);
    }

    public String getUrl() {
        return getString(url);
    }

    public void setUrl(String url) {
        putValue(EndpointInfo.url, url);
    }

}

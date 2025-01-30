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
package com.aspectran.aspectow.appmon.backend.config;

import com.aspectran.utils.Assert;
import com.aspectran.utils.ResourceUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;

public abstract class BackendConfigBuilder {

    private static final String DEFAULT_APPMON_CONFIG_RESOURCE = "com/aspectran/aspectow/appmon/backend/config/backend-config.apon";

    @NonNull
    public static BackendConfig build() throws IOException {
        Reader reader = ResourceUtils.getResourceAsReader(DEFAULT_APPMON_CONFIG_RESOURCE);
        return new BackendConfig(reader);
    }

    @NonNull
    public static BackendConfig build(URI configLocation, String encoding) throws IOException {
        Assert.notNull(configLocation, "configLocation must not be null");
        Reader reader = ResourceUtils.getReader(configLocation.toURL(), encoding);
        return new BackendConfig(reader);
    }

    @NonNull
    public static BackendConfig build(File configFile) throws IOException {
        return new BackendConfig(configFile);
    }

}

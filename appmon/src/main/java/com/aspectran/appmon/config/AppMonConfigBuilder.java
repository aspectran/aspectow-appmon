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

import com.aspectran.utils.Assert;
import com.aspectran.utils.ResourceUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;

public abstract class AppMonConfigBuilder {

    private static final String DEFAULT_BACKEND_CONFIG_RESOURCE = "com/aspectran/appmon/context/appmon-config.apon";

    @NonNull
    public static AppMonConfig build() throws IOException {
        Reader reader = ResourceUtils.getResourceAsReader(DEFAULT_BACKEND_CONFIG_RESOURCE);
        return new AppMonConfig(reader);
    }

    @NonNull
    public static AppMonConfig build(URI configLocation, String encoding) throws IOException {
        Assert.notNull(configLocation, "configLocation must not be null");
        Reader reader = ResourceUtils.getReader(configLocation.toURL(), encoding);
        return new AppMonConfig(reader);
    }

    @NonNull
    public static AppMonConfig build(File configFile) throws IOException {
        return new AppMonConfig(configFile);
    }

}

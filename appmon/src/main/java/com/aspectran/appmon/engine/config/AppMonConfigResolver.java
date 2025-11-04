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

import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.core.component.bean.aware.ApplicationAdapterAware;
import com.aspectran.utils.Assert;
import com.aspectran.utils.ResourceUtils;
import com.aspectran.utils.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.aspectran.utils.ResourceUtils.CLASSPATH_URL_PREFIX;

/**
 * Resolves the location of the AppMon configuration file and builds the {@link AppMonConfig} object.
 * It can handle both classpath resources and file system paths.
 *
 * <p>Created: 2025. 1. 18.</p>
 */
public class AppMonConfigResolver implements ApplicationAdapterAware {

    private ApplicationAdapter applicationAdapter;

    private String configLocation;

    private String encoding;

    /**
     * Gets the application adapter.
     * @return the application adapter
     */
    public ApplicationAdapter getApplicationAdapter() {
        Assert.state(applicationAdapter != null, "ApplicationAdapter is not set");
        return applicationAdapter;
    }

    @Override
    public void setApplicationAdapter(ApplicationAdapter applicationAdapter) {
        this.applicationAdapter = applicationAdapter;
    }

    /**
     * Sets the location of the configuration file.
     * It can be a classpath resource (e.g., "classpath:config.apon") or a file path.
     * @param configLocation the configuration file location
     */
    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }

    /**
     * Sets the character encoding of the configuration file.
     * @param encoding the character encoding
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Resolves the configuration from the specified location and builds the {@link AppMonConfig}.
     * If no location is specified, it loads the default configuration.
     * @return a new {@link AppMonConfig} instance
     * @throws IOException if the configuration file cannot be read
     * @throws URISyntaxException if the configuration location is invalid
     */
    public AppMonConfig resolveConfig() throws IOException, URISyntaxException {
        if (StringUtils.hasLength(configLocation)) {
            URI uri;
            if (configLocation.startsWith(CLASSPATH_URL_PREFIX)) {
                uri = ResourceUtils.getResource(configLocation.substring(CLASSPATH_URL_PREFIX.length())).toURI();
            } else {
                uri = getApplicationAdapter().getRealPath(configLocation).toUri();
            }
            return AppMonConfigBuilder.build(uri, encoding);
        } else {
            return AppMonConfigBuilder.build();
        }
    }

}

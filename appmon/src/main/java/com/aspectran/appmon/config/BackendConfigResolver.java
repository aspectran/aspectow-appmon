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
package com.aspectran.appmon.config;

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
 * <p>Created: 2025. 1. 18.</p>
 */
public class BackendConfigResolver implements ApplicationAdapterAware {

    private ApplicationAdapter applicationAdapter;

    private String configLocation;

    private String encoding;

    public ApplicationAdapter getApplicationAdapter() {
        Assert.state(applicationAdapter != null, "ApplicationAdapter is not set");
        return applicationAdapter;
    }

    @Override
    public void setApplicationAdapter(ApplicationAdapter applicationAdapter) {
        this.applicationAdapter = applicationAdapter;
    }

    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public BackendConfig resolveConfig() throws IOException, URISyntaxException {
        if (StringUtils.hasLength(configLocation)) {
            URI uri;
            if (configLocation.startsWith(CLASSPATH_URL_PREFIX)) {
                uri = ResourceUtils.getResource(configLocation.substring(CLASSPATH_URL_PREFIX.length())).toURI();
            } else {
                uri = getApplicationAdapter().getRealPath(configLocation).toUri();
            }
            return BackendConfigBuilder.build(uri, encoding);
        } else {
            return BackendConfigBuilder.build();
        }
    }

}

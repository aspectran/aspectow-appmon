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
package com.aspectran.aspectow.appmon.manager;

import com.aspectran.aspectow.appmon.config.AppMonConfig;
import com.aspectran.aspectow.appmon.config.AppMonConfigBuilder;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.core.component.bean.aware.ApplicationAdapterAware;
import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * <p>Created: 2025. 1. 18.</p>
 */
public class AppMonConfigResolver implements ApplicationAdapterAware {

    private ApplicationAdapter applicationAdapter;

    private String configLocation;

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

    public AppMonConfig resolveConfig() throws IOException, URISyntaxException {
        if (StringUtils.hasLength(configLocation)) {
            URI uri = applicationAdapter.toRealPathAsURI(configLocation);
            return AppMonConfigBuilder.build(uri);
        } else {
            return AppMonConfigBuilder.build();
        }
    }

}

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

import com.aspectran.aspectow.appmon.backend.config.BackendConfig;
import com.aspectran.aspectow.appmon.backend.config.BackendConfigBuilder;
import com.aspectran.aspectow.appmon.backend.config.BackendConfigResolver;
import com.aspectran.core.component.bean.ablility.FactoryBean;
import com.aspectran.core.component.bean.annotation.AvoidAdvice;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.core.component.bean.aware.ActivityContextAware;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.annotation.jsr305.NonNull;

/**
 * <p>Created: 2024-12-17</p>
 */
@Component
@Bean("appMonManager")
public class AppMonManagerFactoryBean implements ActivityContextAware, FactoryBean<AppMonManager> {

    private ActivityContext context;

    private AppMonManager appMonManager;

    @Override
    @AvoidAdvice
    public void setActivityContext(@NonNull ActivityContext context) {
        this.context = context;
    }

    @Initialize
    public void createAppMonManager() throws Exception {
        BackendConfig backendConfig;
        if (context.getBeanRegistry().containsBean(BackendConfigResolver.class)) {
            BackendConfigResolver backendConfigResolver = context.getBeanRegistry().getBean(BackendConfigResolver.class);
            backendConfig = backendConfigResolver.resolveConfig();
        } else {
            backendConfig = BackendConfigBuilder.build();
        }
        appMonManager = AppMonManagerBuilder.build(context, backendConfig);
    }

    @Override
    public AppMonManager getObject() {
        return appMonManager;
    }

}

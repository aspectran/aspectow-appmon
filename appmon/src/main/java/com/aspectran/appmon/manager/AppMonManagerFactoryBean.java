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
package com.aspectran.appmon.manager;

import com.aspectran.appmon.config.AppMonConfig;
import com.aspectran.appmon.config.AppMonConfigBuilder;
import com.aspectran.appmon.config.AppMonConfigResolver;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.FactoryBean;
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
@Bean(id = "appMonManager", lazyDestroy = true)
public class AppMonManagerFactoryBean implements ActivityContextAware, FactoryBean<AppMonManager>, DisposableBean {

    private ActivityContext context;

    private AppMonManager appMonManager;

    @Override
    public void setActivityContext(@NonNull ActivityContext context) {
        this.context = context;
    }

    @Initialize
    public void createAppMonManager() throws Exception {
        AppMonConfig appMonConfig;
        if (context.getBeanRegistry().containsBean(AppMonConfigResolver.class)) {
            AppMonConfigResolver appMonConfigResolver = context.getBeanRegistry().getBean(AppMonConfigResolver.class);
            appMonConfig = appMonConfigResolver.resolveConfig();
        } else {
            appMonConfig = AppMonConfigBuilder.build();
        }
        appMonManager = AppMonManagerBuilder.build(context, appMonConfig);
    }

    @Override
    public AppMonManager getObject() {
        return appMonManager;
    }

    @Override
    public void destroy() throws Exception {
        if (appMonManager != null) {
            appMonManager.getExportServiceManager().destroy();
        }
    }

}

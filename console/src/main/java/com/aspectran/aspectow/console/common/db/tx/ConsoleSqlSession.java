/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.console.common.db.tx;

import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.DefaultSqlSessionAgent;

/**
 * A {@link DefaultSqlSessionAgent} for handling simple.
 *
 * <p>Created: 2025. 2. 15.</p>
 */
@Component
@Bean(id = "consoleSqlSession", lazyDestroy = true)
public class ConsoleSqlSession extends DefaultSqlSessionAgent {

    /**
     * Instantiates a new ConsoleSqlSession, targeting the "consoleTxAspect".
     */
    public ConsoleSqlSession() {
        super("consoleTxAspect");
        setSqlSessionFactoryBeanId("consoleSqlSessionFactory");
    }

}

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
package com.aspectran.appmon.engine.persist.db.tx;

import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlMapperProvider;
import org.apache.ibatis.session.SqlSession;

/**
 * The default provider for {@link SqlSession} instances.
 * It supplies the simple, auto-committing SqlSession for database operations.
 *
 * <p>Created: 2025. 2. 15.</p>
 */
@Component
@Bean(lazyDestroy = true)
public class DefaultSqlMapperProvider implements SqlMapperProvider {

    private final SqlSession simpleSqlSession;

    @Autowired
    public DefaultSqlMapperProvider(SimpleSqlSession simpleSqlSession) {
        this.simpleSqlSession = simpleSqlSession;
    }

    @Override
    public SqlSession getSimpleSqlSession() {
        return simpleSqlSession;
    }

    @Override
    public SqlSession getBatchSqlSession() {
        throw new UnsupportedOperationException("Batch operation is not supported");
    }

    @Override
    public SqlSession getReuseSqlSession() {
        throw new UnsupportedOperationException("Reuse operation is not supported");
    }

}

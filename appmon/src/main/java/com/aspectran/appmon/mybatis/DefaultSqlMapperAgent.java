/*
 * Copyright (c) 2018-2025 The Aspectran Project
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
package com.aspectran.appmon.mybatis;

import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.aware.ActivityContextAware;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.mybatis.SqlMapperAgent;
import org.apache.ibatis.session.SqlSession;

@Component
@Bean(lazyDestroy = true)
public class DefaultSqlMapperAgent implements SqlMapperAgent, ActivityContextAware {

    private final SqlSession simpleSqlSession;

    private final SqlSession batchSqlSession;

    private final SqlSession reuseSqlSession;

    private ActivityContext context;

    @Autowired
    public DefaultSqlMapperAgent(
            SimpleSqlSession simpleSqlSession,
            BatchSqlSession batchSqlSession,
            ReuseSqlSession reuseSqlSession) {
        this.simpleSqlSession = simpleSqlSession;
        this.batchSqlSession = batchSqlSession;
        this.reuseSqlSession = reuseSqlSession;
    }

    @Override
    public void setActivityContext(ActivityContext context) {
        this.context = context;
    }

    @Override
    public SqlSession getSimpleSqlSession() {
        checkHasActivity();
        return simpleSqlSession;
    }

    @Override
    public SqlSession getBatchSqlSession() {
        checkHasActivity();
        return batchSqlSession;
    }

    @Override
    public SqlSession getReuseSqlSession() {
        checkHasActivity();
        return reuseSqlSession;
    }

    @Override
    public <T> T simple(Class<T> type) {
        return getSimpleSqlSession().getMapper(type);
    }

    @Override
    public <T> T batch(Class<T> type) {
        return getBatchSqlSession().getMapper(type);
    }

    @Override
    public <T> T reuse(Class<T> type) {
        return getReuseSqlSession().getMapper(type);
    }

    private void checkHasActivity() {
        if (!context.hasCurrentActivity()) {
            throw new IllegalStateException("No activity");
        }
    }

}

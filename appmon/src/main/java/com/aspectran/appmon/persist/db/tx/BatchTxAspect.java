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
package com.aspectran.appmon.persist.db.tx;

import com.aspectran.core.component.bean.annotation.After;
import com.aspectran.core.component.bean.annotation.Aspect;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Before;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Finally;
import com.aspectran.core.component.bean.annotation.Joinpoint;
import com.aspectran.core.component.bean.annotation.Scope;
import com.aspectran.core.context.rule.type.ScopeType;
import com.aspectran.mybatis.SqlSessionTxAdvice;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Advice to handle database transactions in batch mode.
 * <ul>
 * <li>Batches all updates (including inserts and deletes), SELECTs can be run as needed.
 * </ul>
 */
@Component
@Bean(lazyDestroy = true)
@Scope(ScopeType.PROTOTYPE)
@Aspect(
        id = "batchTxAspect",
        order = 0
)
@Joinpoint(
        pointcut = {
                "+: **@batchSqlSession"
        }
)
public class BatchTxAspect extends SqlSessionTxAdvice {

    @Autowired
    public BatchTxAspect(SqlSessionFactory sqlSessionFactory) {
        super(sqlSessionFactory);
        setAutoCommit(true);
    }

    @Before
    public void open() {
        super.open(ExecutorType.BATCH);
    }

    @After
    public void commit() {
        super.commit();
    }

    @Finally
    public void close() {
        super.close();
    }

}

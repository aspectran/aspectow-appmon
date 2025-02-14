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
package com.aspectran.appmon.mybatis.mapper;

import com.aspectran.appmon.persist.counter.CounterVO;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlMapperAgent;
import com.aspectran.mybatis.SqlMapperDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * The Interface CounterMapper.
 *
 * @author Juho Jeong
 */
@Mapper
public interface CounterMapper {

    int insertCounterData(CounterVO counterVO);

    @Component
    class Dao extends SqlMapperDao<CounterMapper> implements CounterMapper {

        @Autowired
        public Dao(SqlMapperAgent mapperAgent) {
            super(mapperAgent, CounterMapper.class);
        }

        @Override
        public int insertCounterData(CounterVO counterVO) {
            return simple().insertCounterData(counterVO);
        }

    }

}

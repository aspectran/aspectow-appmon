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
package com.aspectran.appmon.persist.db.mapper;

import com.aspectran.appmon.persist.counter.EventCountVO;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlMapperAgent;
import com.aspectran.mybatis.SqlMapperDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * The Interface CounterMapper.
 *
 * @author Juho Jeong
 */
@Mapper
public interface EventCountMapper {

    EventCountVO getLastEventCount(String domain, String instance, String event);

    void updateLastEventCount(EventCountVO eventCountVO);

    int insertEventCount(EventCountVO eventCountVO);

    List<EventCountVO> getChartData(String domain, String instance, String event);

    List<EventCountVO> getChartDataByHour(String domain, String instance, String event);

    List<EventCountVO> getChartDataByDay(String domain, String instance, String event);

    List<EventCountVO> getChartDataByMonth(String domain, String instance, String event);

    List<EventCountVO> getChartDataByYear(String domain, String instance, String event);

    @Component
    class Dao extends SqlMapperDao<EventCountMapper> implements EventCountMapper {

        @Autowired
        public Dao(SqlMapperAgent mapperAgent) {
            super(mapperAgent, EventCountMapper.class);
        }

        @Override
        public EventCountVO getLastEventCount(String domain, String instance, String event) {
            return simple().getLastEventCount(domain, instance, event);
        }

        @Override
        public void updateLastEventCount(EventCountVO eventCountVO) {
            simple().updateLastEventCount(eventCountVO);
        }

        @Override
        public int insertEventCount(EventCountVO eventCountVO) {
            return simple().insertEventCount(eventCountVO);
        }

        @Override
        public List<EventCountVO> getChartData(String domain, String instance, String event) {
            return simple().getChartData(domain, instance, event);
        }

        @Override
        public List<EventCountVO> getChartDataByHour(String domain, String instance, String event) {
            return simple().getChartDataByHour(domain, instance, event);
        }

        @Override
        public List<EventCountVO> getChartDataByDay(String domain, String instance, String event) {
            return simple().getChartDataByDay(domain, instance, event);
        }

        @Override
        public List<EventCountVO> getChartDataByMonth(String domain, String instance, String event) {
            return simple().getChartDataByMonth(domain, instance, event);
        }

        @Override
        public List<EventCountVO> getChartDataByYear(String domain, String instance, String event) {
            return simple().getChartDataByYear(domain, instance, event);
        }

    }

}

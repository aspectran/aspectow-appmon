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
package com.aspectran.appmon.engine.persist.db.mapper;

import com.aspectran.appmon.engine.persist.counter.EventCountVO;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlMapperAccess;
import com.aspectran.mybatis.SqlMapperProvider;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The MyBatis mapper interface for event count data.
 * Defines methods for CRUD operations on event count records in the database.
 */
@Mapper
public interface EventCountMapper {

    EventCountVO getLastEventCount(String domain, String instance, String event);

    void updateLastEventCount(EventCountVO eventCountVO);

    void insertEventCount(EventCountVO eventCountVO);

    void insertEventCountHourly(EventCountVO eventCountVO);

    void insertEventCountDaily(EventCountVO eventCountVO);

    List<EventCountVO> getChartData(String domain, String instance, String event, LocalDateTime dateOffset);

    List<EventCountVO> getChartDataByHour(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset);

    List<EventCountVO> getChartDataByDay(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset);

    List<EventCountVO> getChartDataByMonth(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset);

    List<EventCountVO> getChartDataByYear(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Data Access Object (DAO) for {@link EventCountMapper}.
     * Provides a convenient way to access the mapper methods.
     */
    @Component
    class Dao extends SqlMapperAccess<EventCountMapper> implements EventCountMapper {

        @Autowired
        public Dao(SqlMapperProvider sqlMapperProvider) {
            super(sqlMapperProvider, EventCountMapper.class);
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
        public void insertEventCount(EventCountVO eventCountVO) {
            simple().insertEventCount(eventCountVO);
        }

        @Override
        public void insertEventCountHourly(EventCountVO eventCountVO) {
            simple().insertEventCountHourly(eventCountVO);
        }

        @Override
        public void insertEventCountDaily(EventCountVO eventCountVO) {
            simple().insertEventCountDaily(eventCountVO);
        }

        @Override
        public List<EventCountVO> getChartData(String domain, String instance, String event, LocalDateTime dateOffset) {
            return simple().getChartData(domain, instance, event, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByHour(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset) {
            return simple().getChartDataByHour(domain, instance, event, zoneOffset, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByDay(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset) {
            return simple().getChartDataByDay(domain, instance, event, zoneOffset, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByMonth(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset) {
            return simple().getChartDataByMonth(domain, instance, event, zoneOffset, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByYear(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset) {
            return simple().getChartDataByYear(domain, instance, event, zoneOffset, dateOffset);
        }

    }

}

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
package com.aspectran.aspectow.appmon.engine.persist.db.mapper;

import com.aspectran.aspectow.appmon.engine.persist.counter.EventCountVO;
import com.aspectran.aspectow.appmon.engine.persist.db.tx.AppMonSqlMapperProvider;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.mybatis.SqlMapperAccess;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The MyBatis mapper interface for event count data.
 * Defines methods for CRUD operations on event count records in the database.
 */
@Mapper
public interface EventCountMapper {

    /**
     * Retrieves the last recorded event count for the specified domain, instance, and event.
     * @param domain the domain name
     * @param instance the instance name
     * @param event the event name
     * @return the last event count VO, or null if not found
     */
    EventCountVO getLastEventCount(String domain, String instance, String event);

    /**
     * Updates the last recorded event count.
     * @param eventCountVO the event count data to update
     */
    void updateLastEventCount(EventCountVO eventCountVO);

    /**
     * Inserts a new raw event count record.
     * @param eventCountVO the event count data to insert
     */
    void insertEventCount(EventCountVO eventCountVO);

    /**
     * Inserts an hourly aggregated event count record.
     * @param eventCountVO the event count data to insert
     */
    void insertEventCountHourly(EventCountVO eventCountVO);

    /**
     * Retrieves raw chart data for the specified criteria.
     * @param domain the domain name
     * @param instance the instance name
     * @param event the event name
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records
     */
    List<EventCountVO> getChartData(String domain, String instance, String event, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by hour for the specified criteria.
     * @param domain the domain name
     * @param instance the instance name
     * @param event the event name
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by hour
     */
    List<EventCountVO> getChartDataByHour(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by day for the specified criteria.
     * @param domain the domain name
     * @param instance the instance name
     * @param event the event name
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by day
     */
    List<EventCountVO> getChartDataByDay(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by month for the specified criteria.
     * @param domain the domain name
     * @param instance the instance name
     * @param event the event name
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by month
     */
    List<EventCountVO> getChartDataByMonth(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by year for the specified criteria.
     * @param domain the domain name
     * @param instance the instance name
     * @param event the event name
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by year
     */
    List<EventCountVO> getChartDataByYear(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Data Access Object (DAO) for {@link EventCountMapper}.
     * Provides a convenient way to access the mapper methods using Aspectran's bean container.
     */
    @Component
    @Bean("appmon.eventCountDao")
    @Profile("!appmon.ext-persistence")
    class Dao extends SqlMapperAccess<EventCountMapper> implements EventCountMapper {

        /**
         * Constructs a new Dao.
         * @param sqlMapperProvider the SQL mapper provider
         */
        @Autowired
        public Dao(AppMonSqlMapperProvider sqlMapperProvider) {
            super(sqlMapperProvider);
        }

        @Override
        public EventCountVO getLastEventCount(String domain, String instance, String event) {
            return mapper().getLastEventCount(domain, instance, event);
        }

        @Override
        public void updateLastEventCount(EventCountVO eventCountVO) {
            mapper().updateLastEventCount(eventCountVO);
        }

        @Override
        public void insertEventCount(EventCountVO eventCountVO) {
            mapper().insertEventCount(eventCountVO);
        }

        @Override
        public void insertEventCountHourly(EventCountVO eventCountVO) {
            mapper().insertEventCountHourly(eventCountVO);
        }

        @Override
        public List<EventCountVO> getChartData(String domain, String instance, String event, LocalDateTime dateOffset) {
            return mapper().getChartData(domain, instance, event, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByHour(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset) {
            return mapper().getChartDataByHour(domain, instance, event, zoneOffset, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByDay(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset) {
            return mapper().getChartDataByDay(domain, instance, event, zoneOffset, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByMonth(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset) {
            return mapper().getChartDataByMonth(domain, instance, event, zoneOffset, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByYear(String domain, String instance, String event, int zoneOffset, LocalDateTime dateOffset) {
            return mapper().getChartDataByYear(domain, instance, event, zoneOffset, dateOffset);
        }

    }

}

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
     * Retrieves the last recorded event count for the specified node, instance, and event.
     * @param nodeId the node identifier
     * @param instanceId the instance identifier
     * @param eventId the event identifier
     * @return the last event count VO, or null if not found
     */
    EventCountVO getLastEventCount(String nodeId, String instanceId, String eventId);

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
     * @param nodeId the node identifier
     * @param instanceId the instance identifier
     * @param eventId the event identifier
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records
     */
    List<EventCountVO> getChartData(String nodeId, String instanceId, String eventId, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by hour for the specified criteria.
     * @param nodeId the node identifier
     * @param instanceId the instance identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by hour
     */
    List<EventCountVO> getChartDataByHour(String nodeId, String instanceId, String eventId, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by day for the specified criteria.
     * @param nodeId the node identifier
     * @param instanceId the instance identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by day
     */
    List<EventCountVO> getChartDataByDay(String nodeId, String instanceId, String eventId, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by month for the specified criteria.
     * @param nodeId the node identifier
     * @param instanceId the instance identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by month
     */
    List<EventCountVO> getChartDataByMonth(String nodeId, String instanceId, String eventId, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by year for the specified criteria.
     * @param nodeId the node identifier
     * @param instanceId the instance identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by year
     */
    List<EventCountVO> getChartDataByYear(String nodeId, String instanceId, String eventId, int zoneOffset, LocalDateTime dateOffset);

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
        public EventCountVO getLastEventCount(String nodeId, String instanceId, String eventId) {
            return mapper().getLastEventCount(nodeId, instanceId, eventId);
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
        public List<EventCountVO> getChartData(String nodeId, String instanceId, String eventId, LocalDateTime dateOffset) {
            return mapper().getChartData(nodeId, instanceId, eventId, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByHour(String nodeId, String instanceId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
            return mapper().getChartDataByHour(nodeId, instanceId, eventId, zoneOffset, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByDay(String nodeId, String instanceId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
            return mapper().getChartDataByDay(nodeId, instanceId, eventId, zoneOffset, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByMonth(String nodeId, String instanceId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
            return mapper().getChartDataByMonth(nodeId, instanceId, eventId, zoneOffset, dateOffset);
        }

        @Override
        public List<EventCountVO> getChartDataByYear(String nodeId, String instanceId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
            return mapper().getChartDataByYear(nodeId, instanceId, eventId, zoneOffset, dateOffset);
        }

    }

}

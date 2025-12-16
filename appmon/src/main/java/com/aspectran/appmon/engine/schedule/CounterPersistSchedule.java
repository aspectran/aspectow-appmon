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
package com.aspectran.appmon.engine.schedule;

import com.aspectran.appmon.engine.manager.AppMonManager;
import com.aspectran.appmon.engine.persist.counter.CounterPersist;
import com.aspectran.appmon.engine.persist.counter.EventCount;
import com.aspectran.appmon.engine.persist.counter.EventCountVO;
import com.aspectran.appmon.engine.persist.counter.EventCounter;
import com.aspectran.appmon.engine.persist.db.mapper.EventCountMapper;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.CronTrigger;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.core.component.bean.annotation.Job;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Schedule;
import com.aspectran.core.context.rule.ScheduleRule;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * A scheduled job that periodically persists event counter data to the database.
 * It also initializes the counters from the database on startup and saves them on shutdown.
 *
 * <p>Created: 2025-02-12</p>
 */
@Component
@Bean
@Schedule(
    id = "counterPersistSchedule",
    scheduler = "appmonScheduler",
    cronTrigger = @CronTrigger(
        expression = "0 */" + CounterPersistSchedule.DEFAULT_SAMPLE_INTERVAL_IN_MINUTES + " * * * ?"
    ),
    jobs = {
        @Job(translet = "appmon/persist/counter/rollup.job")
    }
)
public class CounterPersistSchedule {

    private static final Logger logger = LoggerFactory.getLogger(CounterPersistSchedule.class);

    public static final int DEFAULT_SAMPLE_INTERVAL_IN_MINUTES = 5; // every 5 minutes

    private final AppMonManager appMonManager;

    private final String currentDomain;

    private final CounterPersist counterPersist;

    private final EventCountMapper.Dao dao;

    @Autowired
    public CounterPersistSchedule(@NonNull AppMonManager appMonManager,
                                  EventCountMapper.Dao dao) {
        this.appMonManager = appMonManager;
        this.currentDomain = appMonManager.getCurrentDomain();
        this.counterPersist = appMonManager.getPersistManager().getCounterPersist();
        this.dao = dao;
    }

    /**
     * Initializes the event counters by loading their last state from the database.
     * @throws Exception if initialization fails
     */
    @Initialize
    public void initialize() throws Exception {
        appMonManager.instantActivity(() -> {
            for (EventCounter eventCounter : counterPersist.getEventCounterList()) {
                EventCountVO vo = dao.getLastEventCount(
                        currentDomain, eventCounter.getInstanceName(), eventCounter.getEventName());
                if (vo != null) {
                    eventCounter.reset(vo.getDatetime(), vo.getTotal(), vo.getDelta(), vo.getError());
                } else {
                    String datetime = getDatetime(false);
                    eventCounter.reset(datetime, 0L, 0L, 0L);
                }
                eventCounter.initialize();
            }
            return null;
        });

        int interval = appMonManager.getCounterPersistInterval();
        if (interval > 0) {
            ScheduleRule scheduleRule = appMonManager.getActivityContext()
                    .getScheduleRuleRegistry().getScheduleRule("counterPersistSchedule");
            if (scheduleRule != null && scheduleRule.getTriggerExpressionParameters() != null) {
                String cronExpression = "0 */" + interval + " * * * ?";
                scheduleRule.getTriggerExpressionParameters().putValue("expression", cronExpression);
                logger.info("CounterPersistSchedule is dynamically set to run every {} minutes", interval);
            }
        }
    }

    /**
     * Persists the final event counts to the database on shutdown.
     */
    @Destroy
    public void destroy() {
        try {
            appMonManager.instantActivity(() -> {
                rollupAndSave(false);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to save last event count", e);
        }
    }

    /**
     * The main job method, called by the scheduler to roll up and save the counters.
     */
    @Request("appmon/persist/counter/rollup.job")
    public void rollup() {
        rollupAndSave(true);
    }

    private void rollupAndSave(boolean scheduled) {
        String datetime = getDatetime(scheduled);
        EventCountVO eventCountVO = null;
        for (EventCounter eventCounter : counterPersist.getEventCounterList()) {
            eventCounter.rollup(datetime);
            EventCount eventCount = eventCounter.getEventCount();
            if (eventCount.isUpdated()) {
                if (eventCountVO == null) {
                    eventCountVO = createEventCountVO(datetime);
                }
                eventCountVO.setInstance(eventCounter.getInstanceName());
                eventCountVO.setEvent(eventCounter.getEventName());
                eventCountVO.setTotal(eventCount.getTallied().getTotal());
                eventCountVO.setDelta(eventCount.getTallied().getDelta());
                eventCountVO.setError(eventCount.getTallied().getError());
                dao.updateLastEventCount(eventCountVO);
                dao.insertEventCount(eventCountVO);
            }
        }
    }

    @NonNull
    private String getDatetime(boolean scheduled) {
        Instant instant = Instant.now();
        int interval = appMonManager.getCounterPersistInterval();
        if (!scheduled && interval > 0) {
            int next = instant.atZone(ZoneOffset.UTC).getMinute() + interval;
            int offset = interval - next % interval;
            instant = instant.plus(offset, ChronoUnit.MINUTES);
        }
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        return formatter.format(localDateTime);
    }

    @NonNull
    private EventCountVO createEventCountVO(@NonNull String datetime) {
        EventCountVO eventCountVO = new EventCountVO();
        eventCountVO.setDomain(currentDomain);
        eventCountVO.setDatetime(datetime);
        return eventCountVO;
    }

}

/*
 * Copyright (c) 2020-2025 The Aspectran Project
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
package com.aspectran.appmon.agent.schedule;

import com.aspectran.appmon.manager.AppMonManager;
import com.aspectran.appmon.persist.counter.CounterPersist;
import com.aspectran.appmon.persist.counter.EventCount;
import com.aspectran.appmon.persist.counter.EventCountVO;
import com.aspectran.appmon.persist.counter.EventCounter;
import com.aspectran.appmon.persist.db.mapper.EventCountMapper;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.CronTrigger;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.core.component.bean.annotation.Job;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Schedule;
import com.aspectran.utils.annotation.jsr305.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * <p>Created: 2025-02-12</p>
 */
@Component
@Bean
@Schedule(
    id = "counterPersist1m",
    scheduler = "appmonScheduler",
    cronTrigger = @CronTrigger(
        expression = "0 * * * * ?"
    ),
    jobs = {
        @Job(translet = "appmon/persist/counter/rollup.job")
    }
)
public class CounterPersistSchedule {

    private static final Logger logger = LoggerFactory.getLogger(CounterPersistSchedule.class);

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

    @Initialize
    public void initialize() throws Exception {
        appMonManager.instantActivity(() -> {
            for (EventCounter eventCounter : counterPersist.getEventCounterList()) {
                EventCountVO eventCountVO = dao.getLastEventCount(
                        currentDomain, eventCounter.getInstanceName(), eventCounter.getEventName());
                if (eventCountVO != null) {
                    eventCounter.reset(eventCountVO.getTotal(), eventCountVO.getDelta());
                } else {
                    eventCounter.reset(0L, 0L);
                }
                eventCounter.initialize();
            }
            return null;
        });
    }

    @Destroy
    public void destroy() {
        try {
            appMonManager.instantActivity(() -> {
                EventCountVO eventCountVO = createEventCountVO(true);
                save(eventCountVO);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed save last event count", e);
        }
    }

    @Request("appmon/persist/counter/rollup.job")
    public void rollup() {
        for (EventCounter eventCounter : counterPersist.getEventCounterList()) {
            EventCountVO eventCountVO = createEventCountVO(false);
            eventCounter.rollup(eventCountVO.getDatetime());
            save(eventCountVO);
        }
    }

    private void save(EventCountVO eventCountVO) {
        for (EventCounter eventCounter : counterPersist.getEventCounterList()) {
            EventCount eventCount = eventCounter.getEventCount();
            if (eventCount.getDelta() > 0) {
                eventCountVO.setInstance(eventCounter.getInstanceName());
                eventCountVO.setEvent(eventCounter.getEventName());
                eventCountVO.setTotal(eventCount.getTotal());
                eventCountVO.setDelta(eventCount.getDelta());
                dao.updateLastEventCount(eventCountVO);
                dao.insertEventCount(eventCountVO);
            }
        }
    }

    @NonNull
    private EventCountVO createEventCountVO(boolean aborted) {
        Instant instant = Instant.now();
        if (!aborted) {
            instant = instant.minus(1, ChronoUnit.MINUTES);
        }
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        String yyyyMMddHHmm = formatter.format(localDateTime);
        String ymd = yyyyMMddHHmm.substring(0, 8);
        String hh = yyyyMMddHHmm.substring(8, 10);
        String mm = yyyyMMddHHmm.substring(10, 12);

        EventCountVO eventCountVO = new EventCountVO();
        eventCountVO.setDomain(currentDomain);
        eventCountVO.setDatetime(yyyyMMddHHmm);
        eventCountVO.setYmd(ymd);
        eventCountVO.setHh(hh);
        eventCountVO.setMm(mm);
        return eventCountVO;
    }

}

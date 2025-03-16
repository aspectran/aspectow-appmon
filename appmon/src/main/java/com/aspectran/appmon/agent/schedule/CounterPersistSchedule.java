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
    id = "counterPersistSchedule",
    scheduler = "appmonScheduler",
    cronTrigger = @CronTrigger(
        expression = "0 */" + CounterPersistSchedule.SAMPLE_INTERVAL_IN_MINUTES + " * * * ?"
    ),
    jobs = {
        @Job(translet = "appmon/persist/counter/rollup.job")
    }
)
public class CounterPersistSchedule {

    private static final Logger logger = LoggerFactory.getLogger(CounterPersistSchedule.class);

    protected static final int SAMPLE_INTERVAL_IN_MINUTES = 5; // every 5 minutes

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
            String datetime = getDatetime(false);
            for (EventCounter eventCounter : counterPersist.getEventCounterList()) {
                EventCountVO vo = dao.getLastEventCount(
                        currentDomain, eventCounter.getInstanceName(), eventCounter.getEventName());
                if (vo != null && datetime.compareTo(vo.getDatetime()) >= 0) {
                    eventCounter.reset(vo.getDatetime(), vo.getTotal(), vo.getDelta());
                } else {
                    eventCounter.reset(datetime, 0L, 0L);
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
                rollupAndSave(false);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to save last event count", e);
        }
    }

    @Request("appmon/persist/counter/rollup.job")
    public void rollup() {
        rollupAndSave(true);
    }

    private void rollupAndSave(boolean scheduled) {
        for (EventCounter eventCounter : counterPersist.getEventCounterList()) {
            String datetime = getDatetime(scheduled);
            eventCounter.rollup(datetime);
        }
        save();
    }

    private void save() {
        EventCountVO eventCountVO = null;
        for (EventCounter eventCounter : counterPersist.getEventCounterList()) {
            EventCount eventCount = eventCounter.getEventCount();
            if (eventCount.isTallied()) {
                if (eventCountVO == null) {
                    eventCountVO = createEventCountVO(eventCount.getDatetime());
                }
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
    private String getDatetime(boolean scheduled) {
        Instant instant = Instant.now();
        if (!scheduled) {
            int minute = instant.atZone(ZoneOffset.UTC).getMinute();
            int plus = SAMPLE_INTERVAL_IN_MINUTES - minute % SAMPLE_INTERVAL_IN_MINUTES;
            instant = instant.minus(plus, ChronoUnit.MINUTES);
        }
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        return formatter.format(localDateTime);
    }

    @NonNull
    private EventCountVO createEventCountVO(@NonNull String datetime) {
        String ymd = datetime.substring(0, 8);
        String hh = datetime.substring(8, 10);
        String mm = datetime.substring(10, 12);
        EventCountVO eventCountVO = new EventCountVO();
        eventCountVO.setDomain(currentDomain);
        eventCountVO.setDatetime(datetime);
        eventCountVO.setYmd(ymd);
        eventCountVO.setHh(hh);
        eventCountVO.setMm(mm);
        return eventCountVO;
    }

}

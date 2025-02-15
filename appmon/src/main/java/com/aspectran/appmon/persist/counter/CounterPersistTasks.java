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
package com.aspectran.appmon.persist.counter;

import com.aspectran.appmon.manager.AppMonManager;
import com.aspectran.appmon.mybatis.mapper.CounterMapper;
import com.aspectran.core.activity.InstantActivity;
import com.aspectran.core.activity.InstantActivityException;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.CronTrigger;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.core.component.bean.annotation.Job;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Schedule;
import com.aspectran.core.component.bean.aware.ActivityContextAware;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
        @Job(translet = "appmon/persist/counter/save.job")
    }
)
public class CounterPersistTasks implements ActivityContextAware {

    private final CounterPersist counterPersist;

    private final CounterMapper.Dao dao;

    private ActivityContext context;

    @Autowired
    public CounterPersistTasks(@NonNull AppMonManager appMonManager,
                               CounterMapper.Dao dao) {
        this.counterPersist = appMonManager.getPersistManager().getCounterPersist();
        this.dao = dao;
    }

    @Override
    public void setActivityContext(ActivityContext context) {
        this.context = context;
    }

    @Initialize
    public void initialize() throws Exception {
        List<CounterReader> counterReaderList = counterPersist.getCounterReaderList();
        for (CounterReader counterReader : counterReaderList) {
            counterReader.initialize();
        }
    }

    @Destroy
    public void destroy() {
        try {
            InstantActivity activity = new InstantActivity(context);
            activity.perform(() -> {
                save(true);
                return null;
            });
        } catch (Exception e) {
            throw new InstantActivityException(e);
        }
    }

    @Request("appmon/persist/counter/save.job")
    public void save(boolean aborted) {
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

        CounterVO counterVO = new CounterVO();
        counterVO.setYmd(ymd);
        counterVO.setHh(hh);
        counterVO.setMm(mm);

        List<CounterReader> counterReaderList = counterPersist.getCounterReaderList();
        for (CounterReader counterReader : counterReaderList) {
            long current = counterReader.getCounterData().getCurrent();
            long acquired = counterReader.getCounterData().acquire(current);
//            if (acquired > 0) {
                counterVO.setInst(counterReader.getInstanceName());
                counterVO.setEvt(counterReader.getEventName());
                counterVO.setCnt1(current);
                counterVO.setCnt2(acquired);
                dao.insertCounterData(counterVO);
//            }
        }
    }

}

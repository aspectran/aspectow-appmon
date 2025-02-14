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
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.CronTrigger;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.core.component.bean.annotation.Job;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Schedule;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.List;

/**
 * <p>Created: 2025-02-12</p>
 */
@Component
@Bean
@Schedule(
    id = "counterPersist10m",
    scheduler = "appmonScheduler",
    cronTrigger = @CronTrigger(
        expression = "0/10 * * * * ?"
    ),
    jobs = {
        @Job(translet = "appmon/persist/counter/save.job", disabled = true)
    }
)
public class CounterPersistTasks {

    private final CounterPersist counterPersist;

    private final CounterPersistDao counterPersistDao;

    @Autowired
    public CounterPersistTasks(@NonNull AppMonManager appMonManager, CounterPersistDao counterPersistDao) {
        this.counterPersist = appMonManager.getPersistManager().getCounterPersist();
        this.counterPersistDao = counterPersistDao;
    }

    @Initialize
    public void initialize() throws Exception {
        List<CounterReader> counterReaderList = counterPersist.getCounterReaderList();
        for (CounterReader counterReader : counterReaderList) {
            counterReader.initialize();
        }
    }

    @Request("appmon/persist/counter/save.job")
    public void save() {
        List<CounterReader> counterReaderList = counterPersist.getCounterReaderList();
        for (CounterReader counterReader : counterReaderList) {
            long count = counterReader.getCounterData().check();
            counterPersistDao.insert(count);
        }
    }

}

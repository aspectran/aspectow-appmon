package com.aspectran.aspectow.appmon.backend.persist.counter;

import com.aspectran.aspectow.appmon.manager.AppMonManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.CronTrigger;
import com.aspectran.core.component.bean.annotation.Job;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Schedule;

import java.util.List;

/**
 * <p>Created: 2025-02-12</p>
 */
@Component
@Bean
@Schedule(
    id = "counterPersist",
    scheduler = "appmonScheduler",
    cronTrigger = @CronTrigger(
        expression = "0/10 * * * * ?"
    ),
    jobs = {
        @Job("appmon/persist/counter/save.job")
    }
)
public class CounterPersistTasks {

    private final AppMonManager appMonManager;

    @Autowired
    public CounterPersistTasks(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    @Request("appmon/persist/counter/save.job")
    public void save() {
        List<CounterPersist> counterPersists = appMonManager.getPersistManager().getCounterPersists();
        for (CounterPersist counterPersist : counterPersists) {
            counterPersist.saveCounterData();
        }

    }

}

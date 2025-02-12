package com.aspectran.aspectow.appmon.backend.persist.counter.activity;

import com.aspectran.aspectow.appmon.backend.persist.counter.CounterData;
import com.aspectran.aspectow.appmon.backend.persist.counter.CounterPersist;

/**
 * <p>Created: 2025-02-12</p>
 */
public class ActivityCounterPersist implements CounterPersist {

    private final ActivityCounterReader activityCounterReader;

    private final CounterData counterData;

    public ActivityCounterPersist(ActivityCounterReader activityCounterReader, CounterData counterData) {
        this.activityCounterReader = activityCounterReader;
        this.counterData = counterData;
    }

    public ActivityCounterReader getActivityCounterReader() {
        return activityCounterReader;
    }

    @Override
    public CounterData getCounterData() {
        return counterData;
    }

    @Override
    public void saveCounterData() {

    }

}

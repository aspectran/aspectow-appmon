package com.aspectran.aspectow.appmon.backend.persist;

import com.aspectran.aspectow.appmon.backend.persist.counter.CounterPersist;
import com.aspectran.aspectow.appmon.manager.AppMonManager;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created: 2025-02-12</p>
 */
public class PersistManager {

    private final List<CounterPersist> counterPersists = new ArrayList<>();

    private final AppMonManager appMonManager;

    public PersistManager(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    public AppMonManager getAppMonManager() {
        return appMonManager;
    }

    public void addCounterPersist(CounterPersist counterPersist) {
        counterPersists.add(counterPersist);
    }

    public List<CounterPersist> getCounterPersists() {
        return counterPersists;
    }

}

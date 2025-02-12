package com.aspectran.aspectow.appmon.backend.persist.counter;

/**
 * <p>Created: 2025-02-12</p>
 */
public interface CounterPersist {

    CounterData getCounterData();

    void saveCounterData();

}

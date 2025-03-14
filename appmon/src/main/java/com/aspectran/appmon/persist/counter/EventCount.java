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

import java.util.concurrent.atomic.LongAdder;

/**
 * <p>Created: 2025-02-12</p>
 */
public class EventCount {

    private final LongAdder tally = new LongAdder();

    private volatile String datetime;

    private volatile long oldTotal;

    private volatile long total;

    private volatile long delta;

    public void hit() {
        tally.increment();
    }

    public String getDatetime() {
        return datetime;
    }

    public long getTally() {
        return tally.sum();
    }

    public long getTotal() {
        return total;
    }

    public long getDelta() {
        return delta;
    }

    public synchronized long getGrandTotal() {
        return (total + tally.sum());
    }

    synchronized void rollup(String datetime) {
        this.datetime = datetime;
        long sum = tally.sum();
        tally.reset();
        total += sum;
        delta = total - oldTotal;
        oldTotal = total;
    }

    synchronized void reset(long total, long delta) {
        tally.reset();
        oldTotal = total - delta;
        this.total = total;
        this.delta = delta;
    }

}

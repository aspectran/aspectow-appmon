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

import com.aspectran.utils.Assert;

import java.util.concurrent.atomic.LongAdder;

/**
 * <p>Created: 2025-02-12</p>
 */
public class EventCount {

    private final LongAdder tally = new LongAdder();

    private volatile boolean tallied;

    private volatile String datetime;

    private volatile long total;

    private volatile long delta;

    public void hit() {
        tally.increment();
    }

    public boolean isTallied() {
        return tallied;
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
        Assert.notNull(datetime, "datetime must not be null");
        Assert.isTrue(datetime.length() == 12, "datetime length must be 12");
        long sum = tally.sum();
        tally.reset();
        tallied = (sum > 0);
        if (datetime.equals(this.datetime)) {
            total += sum;
            delta += sum;
        } else {
            this.datetime = datetime;
            total += sum;
            delta = sum;
        }
    }

    synchronized void reset(String datetime, long total, long delta) {
        Assert.isTrue(total >= 0, "total must be positive");
        Assert.isTrue(delta >= 0, "total must be positive");
        this.datetime = datetime;
        tally.reset();
        tallied = false;
        this.total = total;
        this.delta = delta;
    }

}

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

    private final TallyCounter counter = new TallyCounter();

    private volatile boolean talliedUp;

    private volatile String datetime;

    private volatile long total;

    private volatile long delta;

    private volatile long error;

    public void count() {
        counter.count();
    }

    public void error() {
        counter.error();
    }

    public long getTallied() {
        return counter.getTallied();
    }

    public boolean hasTalliedUp() {
        return talliedUp;
    }

    public String getDatetime() {
        return datetime;
    }

    public long getTotal() {
        return total;
    }

    public long getDelta() {
        return delta;
    }

    public long getError() {
        return error;
    }

    public synchronized long getGrandTotal() {
        return (total + counter.getTallied());
    }

    synchronized void rollup(String datetime) {
        Assert.notNull(datetime, "datetime must not be null");
        Assert.isTrue(datetime.length() == 12, "datetime length must be 12");
        long tallied = counter.getTallied();
        long error = counter.getError();
        counter.reset();
        talliedUp = (tallied > 0L);
        if (datetime.equals(this.datetime)) {
            total += tallied;
            delta += tallied;
            this.error += error;
        } else {
            this.datetime = datetime;
            total += tallied;
            delta = tallied;
            this.error = error;
        }
    }

    synchronized void reset(String datetime, long total, long delta, long error) {
        Assert.isTrue(total >= 0, "total must be positive");
        Assert.isTrue(delta >= 0, "delta must be positive");
        Assert.isTrue(error >= 0, "error must be positive");
        this.datetime = datetime;
        counter.reset();
        talliedUp = false;
        this.total = total;
        this.delta = delta;
        this.error = error;
    }

    private static class TallyCounter {

        private final LongAdder count = new LongAdder();

        private final LongAdder error = new LongAdder();

        public void count() {
            count.increment();
        }

        public void error() {
            error.increment();
        }

        public long getTallied() {
            return count.sum();
        }

        public long getError() {
            return error.sum();
        }

        public void reset() {
            count.reset();
            error.reset();
        }

    }

}

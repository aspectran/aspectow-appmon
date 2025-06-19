/*
 * Copyright (c) 2020-present The Aspectran Project
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
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.concurrent.atomic.LongAdder;

/**
 * <p>Created: 2025-02-12</p>
 */
public class EventCount {

    private final Tallying tallying = new Tallying();

    private final Tallied tallied = new Tallied();

    private volatile boolean updated;

    public void count() {
        tallying.count();
    }

    public void error() {
        tallying.error();
    }

    public Tallying getTallying() {
        return tallying;
    }

    public Tallied getTallied() {
        return tallied;
    }

    public boolean isUpdated() {
        return updated;
    }

    synchronized void rollup(String datetime) {
        Assert.notNull(datetime, "datetime must not be null");
        Assert.isTrue(datetime.length() == 12, "datetime length must be 12");
        updated = tallied.update(datetime, tallying);
        tallying.reset();
    }

    synchronized void reset(String datetime, long total, long delta, long error) {
        Assert.isTrue(total >= 0, "total must be positive");
        Assert.isTrue(delta >= 0, "delta must be positive");
        Assert.isTrue(error >= 0, "error must be positive");
        tallied.update(datetime, total, delta, error);
        tallying.reset();
        updated = false;
    }

    public static class Tallying {

        private final LongAdder total = new LongAdder();

        private final LongAdder error = new LongAdder();

        public void count() {
            total.increment();
        }

        public void error() {
            error.increment();
        }

        public long getTotal() {
            return total.sum();
        }

        public long getError() {
            return error.sum();
        }

        public void reset() {
            total.reset();
            error.reset();
        }

    }

    public static class Tallied {

        private String datetime;

        private long total;

        private long delta;

        private long error;

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

        private boolean update(@NonNull String datetime, @NonNull Tallying tallying) {
            long total = tallying.getTotal();
            long error = tallying.getError();
            if (datetime.equals(this.datetime)) {
                this.total += total;
                this.delta += total;
                this.error += error;
            } else {
                this.datetime = datetime;
                this.total += total;
                this.delta = total;
                this.error = error;
            }
            return (total > 0);
        }

        private void update(String datetime, long total, long delta, long error) {
            this.datetime = datetime;
            this.total = total;
            this.delta = delta;
            this.error = error;
        }

    }

}

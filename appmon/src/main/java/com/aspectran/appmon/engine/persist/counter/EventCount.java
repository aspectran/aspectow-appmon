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
package com.aspectran.appmon.engine.persist.counter;

import com.aspectran.utils.Assert;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.LongAdder;

/**
 * Holds and manages the count data for a specific event.
 * It separates counts into a short-term 'tallying' period and a long-term 'tallied' aggregate.
 * This class is thread-safe.
 *
 * <p>Created: 2025-02-12</p>
 */
public class EventCount {

    private final Tallying tallying = new Tallying();

    private final Tallied tallied = new Tallied();

    private volatile boolean updated;

    /**
     * Increments the event count.
     */
    public void count() {
        tallying.count();
    }

    /**
     * Increments the error count.
     */
    public void error() {
        tallying.error();
    }

    /**
     * Gets the current tallying data.
     * @return the tallying data
     */
    public Tallying getTallying() {
        return tallying;
    }

    /**
     * Gets the last tallied (rolled up) data.
     * @return the tallied data
     */
    public Tallied getTallied() {
        return tallied;
    }

    /**
     * Checks if the count has been updated since the last rollup.
     * @return {@code true} if updated, {@code false} otherwise
     */
    public boolean isUpdated() {
        return updated;
    }

    /**
     * Rolls up the current tallying counts into the tallied counts.
     * @param datetime the datetime for the rollup
     */
    synchronized void rollup(LocalDateTime datetime) {
        Assert.notNull(datetime, "datetime must not be null");
        updated = tallied.update(datetime, tallying);
        tallying.reset();
    }

    /**
     * Resets the counter with the given values.
     * @param datetime the datetime for the reset
     * @param total the total count
     * @param delta the delta count
     * @param error the error count
     */
    synchronized void reset(LocalDateTime datetime, long total, long delta, long error) {
        Assert.isTrue(total >= 0, "total must be positive");
        Assert.isTrue(delta >= 0, "delta must be positive");
        Assert.isTrue(error >= 0, "error must be positive");
        tallied.update(datetime, total, delta, error);
        tallying.reset();
        updated = false;
    }

    /**
     * Holds the counts for the current, short-term tallying period.
     */
    public static class Tallying {

        private final LongAdder total = new LongAdder();

        private final LongAdder error = new LongAdder();

        /**
         * Increments the total count.
         */
        public void count() {
            total.increment();
        }

        /**
         * Increments the error count.
         */
        public void error() {
            error.increment();
        }

        /**
         * Gets the total count for the current period.
         * @return the total count
         */
        public long getTotal() {
            return total.sum();
        }

        /**
         * Gets the error count for the current period.
         * @return the error count
         */
        public long getError() {
            return error.sum();
        }

        /**
         * Resets all counts for the current period to zero.
         */
        public void reset() {
            total.reset();
            error.reset();
        }

    }

    /**
     * Holds the aggregated counts from previous tallying periods.
     */
    public static class Tallied {

        private LocalDateTime datetime;

        private long total;

        private long delta;

        private long error;

        /**
         * Gets the datetime of the last rollup.
         * @return the datetime object
         */
        public LocalDateTime getDatetime() {
            return datetime;
        }

        /**
         * Gets the overall total count.
         * @return the total count
         */
        public long getTotal() {
            return total;
        }

        /**
         * Gets the count since the last rollup (the delta).
         * @return the delta count
         */
        public long getDelta() {
            return delta;
        }

        /**
         * Gets the overall error count.
         * @return the error count
         */
        public long getError() {
            return error;
        }

        private boolean update(@NonNull LocalDateTime datetime, @NonNull Tallying tallying) {
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

        private void update(LocalDateTime datetime, long total, long delta, long error) {
            this.datetime = datetime;
            this.total = total;
            this.delta = delta;
            this.error = error;
        }

    }

}

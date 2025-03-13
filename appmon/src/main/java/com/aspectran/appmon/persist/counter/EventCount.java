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

    private final LongAdder current = new LongAdder();

    private volatile long old;

    private volatile long total;

    private volatile long delta;

    public void hit() {
        current.increment();
    }

    public long getTotal() {
        return total;
    }

    public long getDelta() {
        return delta;
    }

    public long getCurrent() {
        return current.sum();
    }

    public synchronized long getCurrentTotal() {
        return (total + current.sum());
    }

    synchronized void rollup() {
        long sum = current.sum();
        current.reset();
        total += sum;
        delta = total - old;
        old = total;
    }

    synchronized void reset(long total, long delta) {
        current.reset();
        old = total - delta;
        this.total = total;
        this.delta = delta;
    }

}

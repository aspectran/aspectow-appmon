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
public class CounterData {

    private final LongAdder counter = new LongAdder();

    private volatile long previous = 0L;

    public void count() {
        counter.increment();
    }

    public long getPrevious() {
        return previous;
    }

    public long getCurrent() {
        return counter.sum();
    }

    public synchronized long acquire(long current) {
        long acquired = current - previous;
        previous = current;
        return acquired;
    }

}

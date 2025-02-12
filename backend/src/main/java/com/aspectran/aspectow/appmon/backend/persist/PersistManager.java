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
package com.aspectran.aspectow.appmon.backend.persist;

import com.aspectran.aspectow.appmon.backend.persist.counter.CounterPersist;
import com.aspectran.aspectow.appmon.manager.AppMonManager;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created: 2025-02-12</p>
 */
public class PersistManager {

    private final List<CounterPersist> counterPersistList = new ArrayList<>();

    private final AppMonManager appMonManager;

    public PersistManager(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    public AppMonManager getAppMonManager() {
        return appMonManager;
    }

    public void addCounterPersist(CounterPersist counterPersist) {
        counterPersistList.add(counterPersist);
    }

    public List<CounterPersist> getCounterPersistList() {
        return counterPersistList;
    }

}

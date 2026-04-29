/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.node.config;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Configuration for the distributed scheduler, specifically for managing
 * the behavior of job locking in a clustered environment.
 */
public class SchedulerConfig extends DefaultParameters {

    private static final ParameterKey lockTimeout;
    private static final ParameterKey releasedOnUnlock;

    private static final ParameterKey[] parameterKeys;

    static {
        lockTimeout = new ParameterKey("lockTimeout", ValueType.LONG);
        releasedOnUnlock = new ParameterKey("releasedOnUnlock", ValueType.BOOLEAN);

        parameterKeys = new ParameterKey[] {
                lockTimeout,
                releasedOnUnlock
        };
    }

    public SchedulerConfig() {
        super(parameterKeys);
    }

    public long getLockTimeout() {
        return getLong(lockTimeout, 60);
    }

    public boolean hasLockTimeout() {
        return hasValue(lockTimeout);
    }

    public void setLockTimeout(long lockTimeout) {
        putValue(SchedulerConfig.lockTimeout, lockTimeout);
    }

    public boolean isReleasedOnUnlock() {
        return getBoolean(releasedOnUnlock, false);
    }

    public boolean hasReleasedOnUnlock() {
        return hasValue(releasedOnUnlock);
    }

    public void setReleasedOnUnlock(boolean releasedOnUnlock) {
        putValue(SchedulerConfig.releasedOnUnlock, releasedOnUnlock);
    }

}

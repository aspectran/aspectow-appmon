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
package com.aspectran.aspectow.appmon.endpoint.polling;

import com.aspectran.aspectow.appmon.endpoint.AppMonSession;
import com.aspectran.utils.thread.AutoLock;
import com.aspectran.utils.timer.CyclicTimeout;

import java.util.concurrent.TimeUnit;

public class PollingAppMonSession implements AppMonSession {

    private static final int MIN_POLLING_INTERVAL = 500;

    private static final int MIN_SESSION_TIMEOUT = 500;

    private final AutoLock autoLock = new AutoLock();

    private final PollingAppMonService service;

    private final SessionExpiryTimer expiryTimer;

    private volatile int sessionTimeout;

    private volatile int pollingInterval;

    private int lastLineIndex = -1;

    private boolean expired;

    private String[] joinedGroups;

    public PollingAppMonSession(PollingAppMonService service, int sessionTimeout, int pollingInterval) {
        this.service = service;
        this.sessionTimeout = sessionTimeout;
        this.pollingInterval = pollingInterval;
        this.expiryTimer = new SessionExpiryTimer();
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = Math.max(sessionTimeout, MIN_SESSION_TIMEOUT);
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = Math.max(pollingInterval, MIN_POLLING_INTERVAL);
    }

    @Override
    public String[] getJoinedGroups() {
        return joinedGroups;
    }

    @Override
    public void saveJoinedGroups(String[] joinGroupNames) {
        this.joinedGroups = joinGroupNames;
    }

    @Override
    public void removeJoinedGroups() {
        this.joinedGroups = null;
    }

    @Override
    public boolean isTwoWay() {
        return false;
    }

    public int getLastLineIndex() {
        return lastLineIndex;
    }

    protected void setLastLineIndex(int lastLineIndex) {
        this.lastLineIndex = lastLineIndex;
    }

    protected void access(boolean create) {
        try (AutoLock ignored = autoLock.lock()) {
            if (isValid()) {
                if (!create) {
                    expiryTimer.cancel();
                }
                expiryTimer.schedule(sessionTimeout);
            }
        }
    }

    protected void destroy() {
        try (AutoLock ignored = autoLock.lock()) {
            expiryTimer.destroy();
        }
    }

    @Override
    public boolean isValid() {
        return !isExpired();
    }

    protected boolean isExpired() {
        try (AutoLock ignored = autoLock.lock()) {
            return expired;
        }
    }

    protected AutoLock lock() {
        return autoLock.lock();
    }

    private void doExpiry() {
        try (AutoLock ignored = lock()) {
            if (!expired) {
                expired = true;
                service.scavenge();
            }
        }
    }

    public class SessionExpiryTimer {

        private final CyclicTimeout timer;

        SessionExpiryTimer() {
            timer = new CyclicTimeout(service.getScheduler()) {
                @Override
                public void onTimeoutExpired() {
                    doExpiry();
                }
            };
        }

        public void schedule(long delay) {
            if (delay >= 0) {
                timer.schedule(delay, TimeUnit.MILLISECONDS);
            }
        }

        public void cancel() {
            timer.cancel();
        }

        public void destroy() {
            timer.destroy();
        }

    }

}

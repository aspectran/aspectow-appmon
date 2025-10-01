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
package com.aspectran.appmon.service.polling;

import com.aspectran.appmon.service.ServiceSession;
import com.aspectran.utils.concurrent.AutoLock;
import com.aspectran.utils.timer.CyclicTimeout;

import java.util.concurrent.TimeUnit;

/**
 * Represents a client session for the {@link PollingExportService}.
 * It manages session-specific state like timeouts, polling intervals, and joined instances.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class PollingServiceSession implements ServiceSession {

    private static final int MIN_POLLING_INTERVAL = 500;

    private static final int MIN_SESSION_TIMEOUT = 500;

    private final AutoLock autoLock = new AutoLock();

    private final PollingServiceSessionManager sessionManager;

    private final SessionExpiryTimer expiryTimer;

    private volatile int sessionTimeout;

    private volatile int pollingInterval;

    private int lastLineIndex = -1;

    private boolean expired;

    private String[] joinedInstances;

    private String timeZone;

    /**
     * Instantiates a new PollingServiceSession.
     * @param sessionManager the session manager that created this session
     */
    public PollingServiceSession(PollingServiceSessionManager sessionManager) {
        this.sessionManager = sessionManager;
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
    public String[] getJoinedInstances() {
        return joinedInstances;
    }

    @Override
    public void setJoinedInstances(String[] instanceNames) {
        this.joinedInstances = instanceNames;
    }

    @Override
    public void removeJoinedInstances() {
        this.joinedInstances = null;
    }

    @Override
    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Gets the index of the last message line that was sent to this session.
     * @return the last line index
     */
    public int getLastLineIndex() {
        return lastLineIndex;
    }

    protected void setLastLineIndex(int lastLineIndex) {
        this.lastLineIndex = lastLineIndex;
    }

    /**
     * Updates the session's last access time and schedules the next expiry check.
     * @param create {@code true} if the session is being created
     */
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

    /**
     * Destroys this session and its expiry timer.
     */
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
                sessionManager.scavenge();
            }
        }
    }

    /**
     * A timer to handle session expiration.
     */
    public class SessionExpiryTimer {

        private final CyclicTimeout timer;

        SessionExpiryTimer() {
            timer = new CyclicTimeout(sessionManager.getScheduler()) {
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

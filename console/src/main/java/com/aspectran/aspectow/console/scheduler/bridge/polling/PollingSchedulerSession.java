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
package com.aspectran.aspectow.console.scheduler.bridge.polling;

import com.aspectran.aspectow.console.scheduler.bridge.SchedulerSession;
import com.aspectran.utils.concurrent.AutoLock;
import com.aspectran.utils.timer.CyclicTimeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link SchedulerSession} implementation for HTTP polling.
 * It tracks the last message index retrieved by the client.
 */
public class PollingSchedulerSession implements SchedulerSession {

    private static final int MIN_SESSION_TIMEOUT = 500;

    private final AutoLock autoLock = new AutoLock();

    private final String id;

    private final PollingSchedulerBridge bridge;

    private final PollingSessionManager sessionManager;

    private final SessionExpiryTimer expiryTimer;

    private String nodeId;

    private volatile int sessionTimeout;

    private final AtomicInteger lastLineIndex = new AtomicInteger(-1);

    private boolean expired;

    public PollingSchedulerSession(String id, PollingSchedulerBridge bridge, PollingSessionManager sessionManager) {
        this.id = id;
        this.bridge = bridge;
        this.sessionManager = sessionManager;
        this.expiryTimer = new SessionExpiryTimer();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public boolean isValid() {
        return !isExpired();
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = Math.max(sessionTimeout, MIN_SESSION_TIMEOUT);
    }

    public void access(boolean first) {
        try (AutoLock ignored = autoLock.lock()) {
            if (isValid()) {
                if (!first) {
                    expiryTimer.cancel();
                }
                if (first && bridge != null) {
                    this.lastLineIndex.set(bridge.getBufferedMessages().getCurrentLineIndex());
                }
                expiryTimer.schedule(sessionTimeout);
            }
        }
    }

    public boolean isExpired() {
        try (AutoLock ignored = autoLock.lock()) {
            return expired;
        }
    }

    public int getLastLineIndex() {
        return lastLineIndex.get();
    }

    public void setLastLineIndex(int lastLineIndex) {
        this.lastLineIndex.set(lastLineIndex);
    }

    public void destroy() {
        try (AutoLock ignored = autoLock.lock()) {
            expired = true;
            expiryTimer.destroy();
        }
    }

    private void doExpiry() {
        try (AutoLock ignored = autoLock.lock()) {
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

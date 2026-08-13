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
package com.aspectran.aspectow.appmon.engine.relay.polling;

import com.aspectran.aspectow.appmon.engine.relay.RelaySession;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.concurrent.AutoLock;
import com.aspectran.utils.timer.CyclicTimeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Represents a client session for the {@link PollingMessageRelayer}.
 * It manages session-specific state like timeouts, polling intervals, and subscribed apps.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class PollingRelaySession implements RelaySession {

    private static final int MIN_POLLING_INTERVAL = 500;

    private static final int MIN_SESSION_TIMEOUT = 500;

    private final AutoLock autoLock = new AutoLock();

    private final String id;

    private final PollingSessionManager sessionManager;

    private final SessionExpiryTimer expiryTimer;

    private final List<String> messageQueue = new ArrayList<>();

    private volatile int sessionTimeout;

    private volatile int pollingInterval;

    private int lastLineIndex = -1;

    private boolean expired;

    private String[] subscribedApps;

    private String subscribedNodeId;

    private String timeZone;

    private String focusedAppId;

    /**
     * Instantiates a new PollingRelaySession.
     * @param id the unique identifier of this session
     * @param sessionManager the session manager that created this session
     */
    public PollingRelaySession(String id, PollingSessionManager sessionManager) {
        this.id = id;
        this.sessionManager = sessionManager;
        this.expiryTimer = new SessionExpiryTimer();
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Returns the session timeout duration in milliseconds.
     * @return the session timeout
     */
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Sets the session timeout duration in milliseconds.
     * @param sessionTimeout the session timeout to set
     */
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = Math.max(sessionTimeout, MIN_SESSION_TIMEOUT);
    }

    /**
     * Returns the polling interval in milliseconds.
     * @return the polling interval
     */
    public int getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Sets the polling interval in milliseconds.
     * @param pollingInterval the polling interval to set
     */
    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = Math.max(pollingInterval, MIN_POLLING_INTERVAL);
    }

    @Override
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Sets the time zone ID for the session.
     * @param timeZone the time zone ID
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public String[] getSubscribedApps() {
        return subscribedApps;
    }

    @Override
    public String getSubscribedNodeId() {
        return subscribedNodeId;
    }

    @Override
    public void setSubscribedNodeId(String nodeId) {
        if (StringUtils.hasText(nodeId)) {
            this.subscribedNodeId = nodeId;
        } else {
            this.subscribedNodeId = null;
        }
    }

    @Override
    public void setSubscribedApps(String[] appIds) {
        this.subscribedApps = appIds;
    }

    @Override
    public void removeSubscribedApps() {
        this.subscribedApps = null;
    }

    @Override
    public String getFocusedAppId() {
        return focusedAppId;
    }

    @Override
    public void setFocusedAppId(String appId) {
        if (StringUtils.hasText(appId)) {
            this.focusedAppId = appId;
        } else {
            this.focusedAppId = null;
        }
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
     * Pushes a message to the session's individual queue.
     * @param message the message to push
     */
    public void push(String message) {
        try (AutoLock ignored = autoLock.lock()) {
            if (isValid()) {
                messageQueue.add(message);
            }
        }
    }

    /**
     * Pops all messages from the session's individual queue.
     * @return a list of messages, or {@code null} if the queue is empty
     */
    public List<String> popMessages() {
        try (AutoLock ignored = autoLock.lock()) {
            if (messageQueue.isEmpty()) {
                return null;
            }
            List<String> messages = new ArrayList<>(messageQueue);
            messageQueue.clear();
            return messages;
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
            messageQueue.clear();
        }
    }

    /**
     * Acquires the session lock for thread-safe operations.
     * @return the auto lock instance
     */
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

        /**
         * Constructs a new SessionExpiryTimer.
         */
        SessionExpiryTimer() {
            timer = new CyclicTimeout(sessionManager.getScheduler()) {
                @Override
                public void onTimeoutExpired() {
                    doExpiry();
                }
            };
        }

        /**
         * Schedules the expiration check after the specified delay.
         * @param delay the delay in milliseconds
         */
        public void schedule(long delay) {
            if (delay >= 0) {
                timer.schedule(delay, TimeUnit.MILLISECONDS);
            }
        }

        /**
         * Cancels the scheduled expiration check.
         */
        public void cancel() {
            timer.cancel();
        }

        /**
         * Destroys the expiration timer.
         */
        public void destroy() {
            timer.destroy();
        }

    }

}

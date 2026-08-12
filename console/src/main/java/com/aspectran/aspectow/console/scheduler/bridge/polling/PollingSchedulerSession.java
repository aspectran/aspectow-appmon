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
package com.aspectran.aspectow.console.scheduler.bridge.polling;

import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerSession;
import com.aspectran.utils.concurrent.AutoLock;
import com.aspectran.utils.timer.CyclicTimeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A {@link SchedulerSession} implementation for HTTP polling.
 * It tracks the last message index retrieved by the client.
 */
class PollingSchedulerSession implements SchedulerSession {

    private static final int DEFAULT_POLLING_INTERVAL = 3000;

    private static final int MIN_POLLING_INTERVAL = 1000;

    private static final int MAX_POLLING_INTERVAL = 10000;

    private static final int SESSION_TIMEOUT_THRESHOLD = 30000;

    private final AutoLock autoLock = new AutoLock();

    private final List<String> messageQueue = new ArrayList<>();

    private final String id;

    private final PollingSessionManager sessionManager;

    private final SessionExpiryTimer expiryTimer;

    private String nodeId;

    private volatile int pollingInterval = MAX_POLLING_INTERVAL;

    private volatile int sessionTimeout = MAX_POLLING_INTERVAL + SESSION_TIMEOUT_THRESHOLD;

    private int lastLineIndex = -1;

    private boolean expired;

    /**
     * Instantiates a new PollingSchedulerSession.
     * @param id the unique identifier of this session
     * @param sessionManager the session manager that created this session
     */
    public PollingSchedulerSession(String id, PollingSessionManager sessionManager) {
        this.id = id;
        this.sessionManager = sessionManager;
        this.expiryTimer = new SessionExpiryTimer();
    }

    /**
     * Gets the unique identifier of this polling session.
     * @return the session ID
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Gets the node identifier associated with this session.
     * @return the node ID, or {@code null} if not set
     */
    @Override
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Sets the node identifier associated with this session.
     * @param nodeId the node ID to associate with this session
     */
    @Override
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Gets the current polling interval in milliseconds.
     * @return the polling interval in milliseconds
     */
    public int getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Sets the polling interval and recalculates the session timeout.
     * @param pollingInterval the polling interval in milliseconds
     */
    public void setPollingInterval(int pollingInterval) {
        if (pollingInterval <= 0) {
            this.pollingInterval = DEFAULT_POLLING_INTERVAL;
        } else if (pollingInterval < MIN_POLLING_INTERVAL) {
            this.pollingInterval = MIN_POLLING_INTERVAL;
        } else {
            this.pollingInterval = Math.min(pollingInterval, MAX_POLLING_INTERVAL);
        }
        this.sessionTimeout = pollingInterval + SESSION_TIMEOUT_THRESHOLD;
    }

    /**
     * Gets the session timeout threshold in milliseconds.
     * @return the session timeout in milliseconds
     */
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Gets the index of the last message line that was sent to this session.
     * @return the last line index
     */
    public int getLastLineIndex() {
        return lastLineIndex;
    }

    /**
     * Sets the index of the last message line that was sent to this session.
     * @param lastLineIndex the last line index
     */
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

    /**
     * Checks if this session is valid (i.e., not expired).
     * @return {@code true} if the session is valid; {@code false} otherwise
     */
    @Override
    public boolean isValid() {
        return !isExpired();
    }

    /**
     * Checks if this session has expired.
     * @return {@code true} if the session is expired; {@code false} otherwise
     */
    protected boolean isExpired() {
        try (AutoLock ignored = autoLock.lock()) {
            return expired;
        }
    }

    /**
     * Updates the session's last access time and schedules the next expiry check.
     * @param create {@code true} if the session is being created; {@code false}
     *      if it is an access to an existing session
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
     * Locks this session using its internal AutoLock.
     * @return the {@link AutoLock} instance
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
         * Constructs a new session expiry timer.
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
         * Destroys this expiry timer.
         */
        public void destroy() {
            timer.destroy();
        }

    }

}

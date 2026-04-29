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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link SchedulerSession} implementation for HTTP polling.
 * It tracks the last message index retrieved by the client.
 */
public class PollingSchedulerSession implements SchedulerSession {

    private final PollingSchedulerBridge bridge;

    private String nodeId;

    private volatile int sessionTimeout;

    private volatile long lastAccessTime;

    private final AtomicInteger lastLineIndex = new AtomicInteger(-1);

    private volatile boolean expired;

    public PollingSchedulerSession(PollingSchedulerBridge bridge) {
        this.bridge = bridge;
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
        return !expired;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void access(boolean first) {
        this.lastAccessTime = System.currentTimeMillis();
        if (first && bridge != null) {
            this.lastLineIndex.set(bridge.getBufferedMessages().getCurrentLineIndex());
        }
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public boolean isExpired() {
        if (expired) {
            return true;
        }
        if (sessionTimeout > 0 && lastAccessTime > 0) {
            if (System.currentTimeMillis() - lastAccessTime > (long)sessionTimeout * 1000L) {
                expired = true;
                return true;
            }
        }
        return false;
    }

    public int getLastLineIndex() {
        return lastLineIndex.get();
    }

    public void setLastLineIndex(int lastLineIndex) {
        this.lastLineIndex.set(lastLineIndex);
    }

    public void expire() {
        this.expired = true;
    }

    public void destroy() {
        expire();
    }

}

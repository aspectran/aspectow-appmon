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

import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.session.SessionIdGenerator;
import com.aspectran.utils.CopyOnWriteMap;
import com.aspectran.utils.scheduling.ScheduledExecutorScheduler;
import com.aspectran.utils.scheduling.Scheduler;

import java.util.Map;

/**
 * Manages {@link PollingSchedulerSession} instances for the polling scheduler bridge.
 * It handles session creation, retrieval, and expiration.
 */
public class PollingSessionManager extends AbstractComponent {

    private final SessionIdGenerator sessionIdGenerator = new SessionIdGenerator();

    private final Scheduler scheduler = new ScheduledExecutorScheduler("PSM-Scheduler", false);

    private final Map<String, PollingSchedulerSession> sessions = new CopyOnWriteMap<>();

    private final PollingSchedulerBridge bridge;

    public PollingSessionManager(PollingSchedulerBridge bridge) {
        this.bridge = bridge;
    }

    public PollingSchedulerSession createSession(String nodeId) {
        String sessionId = sessionIdGenerator.createSessionId();
        PollingSchedulerSession newSession = new PollingSchedulerSession(sessionId, bridge, this);
        newSession.setNodeId(nodeId);
        newSession.setSessionTimeout(60); // 1 minute default
        newSession.access(true);
        sessions.put(sessionId, newSession);
        bridge.getBroker().join(newSession);
        return newSession;
    }

    public PollingSchedulerSession getSession(String sessionId) {
        PollingSchedulerSession session = sessions.get(sessionId);
        if (session != null) {
            session.access(false);
        }
        return session;
    }

    public Map<String, PollingSchedulerSession> getSessions() {
        return sessions;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Scavenges for and removes expired sessions.
     */
    public void scavenge() {
        if (!sessions.isEmpty()) {
            sessions.entrySet().removeIf(entry -> {
                PollingSchedulerSession session = entry.getValue();
                if (session.isExpired()) {
                    bridge.getBroker().release(session);
                    session.destroy();
                    return true;
                }
                return false;
            });
            if (sessions.isEmpty()) {
                bridge.getBufferedMessages().clear();
            } else {
                bridge.shrinkBuffer();
            }
        }
    }

    @Override
    protected void doInitialize() throws Exception {
        scheduler.start();
    }

    @Override
    protected void doDestroy() throws Exception {
        scheduler.stop();
        sessions.clear();
    }

}

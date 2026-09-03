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
package com.aspectran.aspectow.console.cluster.bridge.polling;

import com.aspectran.aspectow.node.management.nodes.bridge.NodeSession;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.session.SessionIdGenerator;
import com.aspectran.utils.CopyOnWriteMap;
import com.aspectran.utils.scheduling.ScheduledExecutorScheduler;
import com.aspectran.utils.scheduling.Scheduler;
import com.aspectran.web.support.http.Cookie;
import com.aspectran.web.support.util.CookieGenerator;
import com.aspectran.web.support.util.WebUtils;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Manages {@link PollingNodeSession} instances for the polling node management bridge.
 */
class PollingSessionManager extends AbstractComponent {

    private static final String SESSION_ID_COOKIE_NAME = PollingSessionManager.class.getName() + ".SESSION_ID";

    private final CookieGenerator sessionIdCookieGenerator = new CookieGenerator(SESSION_ID_COOKIE_NAME);

    private final SessionIdGenerator sessionIdGenerator = new SessionIdGenerator();

    private final Scheduler scheduler = new ScheduledExecutorScheduler("PSM-Scheduler(nodes)", true);

    private final Map<String, PollingNodeSession> sessions = new CopyOnWriteMap<>();

    private final PollingNodeBridge bridge;

    private final BroadcastMessageBuffer broadcastMessageBuffer;

    /**
     * Instantiates a new {@code PollingSessionManager} with the specified node bridge.
     * @param bridge the polling node management bridge associated with this manager
     */
    public PollingSessionManager(PollingNodeBridge bridge) {
        this.bridge = bridge;
        this.broadcastMessageBuffer = new BroadcastMessageBuffer();
    }

    /**
     * Retrieves the polling node session with the given session ID.
     * @param sessionId the session ID
     * @return the {@link PollingNodeSession}, or {@code null} if not found
     */
    public PollingNodeSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Creates a new polling session or retrieves an existing one.
     * @param translet the current translet
     * @return a new or existing {@link PollingNodeSession}
     */
    public PollingNodeSession createSession(@NonNull Translet translet) {
        int pollingInterval = 0;
        try {
            pollingInterval = Integer.parseInt(translet.getParameter("pollingInterval"));
        } catch (NumberFormatException e) {
            // ignore
        }

        String sessionId = getSessionId(translet, true);
        PollingNodeSession existingSession = sessions.get(sessionId);
        if (existingSession != null) {
            existingSession.access(false);
            existingSession.setPollingInterval(pollingInterval);
            return existingSession;
        } else {
            PollingNodeSession newSession = new PollingNodeSession(sessionId, this);
            newSession.setPollingInterval(pollingInterval);
            existingSession = sessions.put(sessionId, newSession);
            if (existingSession != null) {
                return existingSession;
            } else {
                newSession.access(true);
                return newSession;
            }
        }
    }

    /**
     * Gets the polling session associated with the current request.
     * @param translet the current translet
     * @return the {@link PollingNodeSession}, or {@code null} if not found
     */
    public PollingNodeSession getSession(@NonNull Translet translet) {
        String sessionId = getSessionId(translet, false);
        if (sessionId == null) {
            return null;
        }
        PollingNodeSession serviceSession = sessions.get(sessionId);
        if (serviceSession != null) {
            serviceSession.access(false);
            return serviceSession;
        } else {
            return null;
        }
    }

    private String getSessionId(@NonNull Translet translet, boolean create) {
        String cookieName = sessionIdCookieGenerator.getCookieName();
        Cookie cookie = WebUtils.getCookie(translet, cookieName);
        String sessionId = null;
        if (cookie != null) {
            sessionId = cookie.getValue();
        }
        if (sessionId == null && create) {
            sessionId = sessionIdGenerator.createSessionId();
            sessionIdCookieGenerator.addCookie(translet.getResponseAdapter(), sessionId);
        }
        return sessionId;
    }

    /**
     * Pushes a message to the central buffer to be pulled by clients.
     * @param message the message to push
     */
    public void push(String message) {
        if (!sessions.isEmpty()) {
            broadcastMessageBuffer.push(message);
        }
    }

    /**
     * Pushes a message to a specific node session.
     * @param nodeSession the target node session
     * @param message the message to push
     */
    public void push(NodeSession nodeSession, String message) {
        if (nodeSession instanceof PollingNodeSession pollingNodeSession) {
            pollingNodeSession.push(message);
        }
    }

    /**
     * Pulls new messages from the buffer for a specific session.
     * @param session the session pulling the messages
     * @return an array of new messages, or {@code null} if there are no new messages
     */
    public String[] pull(PollingNodeSession session) {
        String[] bMessages = broadcastMessageBuffer.pop(session);
        List<String> pMessages = session.popMessages();
        if (bMessages == null && pMessages == null) {
            return null;
        }
        if (bMessages != null && bMessages.length > 0) {
            shrinkBuffer();
        }
        if (bMessages != null && pMessages != null) {
            String[] messages = new String[bMessages.length + pMessages.size()];
            System.arraycopy(bMessages, 0, messages, 0, bMessages.length);
            for (int i = 0; i < pMessages.size(); i++) {
                messages[bMessages.length + i] = pMessages.get(i);
            }
            return messages;
        } else if (bMessages != null) {
            return bMessages;
        } else {
            return pMessages.toArray(new String[0]);
        }
    }

    private void shrinkBuffer() {
        int minLineIndex = getMinLineIndex();
        if (minLineIndex > -1) {
            broadcastMessageBuffer.shrink(minLineIndex);
        }
    }

    private int getMinLineIndex() {
        int minLineIndex = -1;
        for (PollingNodeSession session : sessions.values()) {
            if (minLineIndex == -1) {
                minLineIndex = session.getLastLineIndex();
            } else if (session.getLastLineIndex() < minLineIndex) {
                minLineIndex = session.getLastLineIndex();
            }
        }
        return minLineIndex;
    }

    /**
     * Scavenges for and removes expired sessions.
     */
    protected void scavenge() {
        if (!sessions.isEmpty()) {
            sessions.entrySet().removeIf(entry -> {
                PollingNodeSession session = entry.getValue();
                if (session.isExpired()) {
                    bridge.getRemoteNodeManager().unregisterSession(session.getId());
                    bridge.getBroker().unsubscribe(session);
                    session.destroy();
                    return true;
                }
                return false;
            });
            if (sessions.isEmpty()) {
                broadcastMessageBuffer.clear();
            } else {
                shrinkBuffer();
            }
        }
    }

    /**
     * Returns the scheduler used for managing background tasks.
     * @return the scheduler
     */
    protected Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    protected void doInitialize() throws Exception {
        scheduler.start();
    }

    @Override
    protected void doDestroy() throws Exception {
        scheduler.stop();
        broadcastMessageBuffer.clear();
    }

}

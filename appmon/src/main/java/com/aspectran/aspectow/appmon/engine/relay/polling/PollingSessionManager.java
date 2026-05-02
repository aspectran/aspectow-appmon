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

import com.aspectran.aspectow.appmon.engine.config.PollingConfig;
import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.session.SessionIdGenerator;
import com.aspectran.utils.CopyOnWriteMap;
import com.aspectran.utils.scheduling.ScheduledExecutorScheduler;
import com.aspectran.utils.scheduling.Scheduler;
import com.aspectran.web.support.util.CookieGenerator;
import com.aspectran.web.support.util.WebUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * Manages {@link PollingRelaySession} apps for the polling export service.
 * It handles session creation, retrieval, and expiration, as well as managing a central message buffer.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class PollingSessionManager extends AbstractComponent {

    private static final String SESSION_ID_COOKIE_NAME = PollingSessionManager.class.getName() + ".SESSION_ID";

    private final CookieGenerator sessionIdCookieGenerator = new CookieGenerator(SESSION_ID_COOKIE_NAME);

    private final SessionIdGenerator sessionIdGenerator = new SessionIdGenerator();

    private final Scheduler scheduler = new ScheduledExecutorScheduler("PSM-Scheduler", false);

    private final Map<String, PollingRelaySession> sessions = new CopyOnWriteMap<>();

    private final AppMonManager appMonManager;

    private final BufferedMessages bufferedMessages;

    /**
     * Instantiates a new PollingSessionManager.
     * @param appMonManager the main application manager
     */
    public PollingSessionManager(@NonNull AppMonManager appMonManager) {
        this.appMonManager = appMonManager;

        PollingConfig pollingConfig = appMonManager.getPollingConfig();
        this.bufferedMessages = new BufferedMessages(pollingConfig.getInitialBufferSize());
    }

    /**
     * Creates a new polling session or retrieves an existing one.
     * @param translet the current translet
     * @param pollingConfig the polling configuration
     * @param appIds the IDs of the apps to join
     * @return a new or existing {@link PollingRelaySession}
     */
    public PollingRelaySession createSession(
            @NonNull Translet translet, @NonNull PollingConfig pollingConfig, String[] appIds) {
        int pollingInterval = pollingConfig.getPollingInterval();
        int sessionTimeout = pollingConfig.getSessionTimeout();
        if (pollingInterval > 0 && sessionTimeout <= 0) {
            sessionTimeout = pollingInterval * 2;
        }

        String sessionId = getSessionId(translet, true);
        PollingRelaySession existingSession = sessions.get(sessionId);
        if (existingSession != null) {
            existingSession.access(false);
            existingSession.setSessionTimeout(sessionTimeout);
            existingSession.setPollingInterval(pollingInterval);
            return existingSession;
        } else {
            PollingRelaySession newSession = new PollingRelaySession(sessionId, this);
            newSession.setSessionTimeout(sessionTimeout);
            newSession.setPollingInterval(pollingInterval);
            if (appIds != null) {
                newSession.setJoinedApps(appIds);
            }
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
     * @return the {@link PollingRelaySession}, or {@code null} if not found
     */
    public PollingRelaySession getSession(@NonNull Translet translet) {
        String sessionId = getSessionId(translet, false);
        if (sessionId == null) {
            return null;
        }
        PollingRelaySession serviceSession = sessions.get(sessionId);
        if (serviceSession != null) {
            serviceSession.access(false);
            return serviceSession;
        } else {
            return null;
        }
    }

    private String getSessionId(@NonNull Translet translet, boolean create) {
        HttpServletRequest request = translet.getRequestAdaptee();
        HttpServletResponse response = translet.getResponseAdaptee();
        String cookieName = sessionIdCookieGenerator.getCookieName();
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        String sessionId = null;
        if (cookie != null) {
            sessionId = cookie.getValue();
        }
        if (sessionId == null && create) {
            sessionId = sessionIdGenerator.createSessionId();
            sessionIdCookieGenerator.addCookie(response, sessionId);
        }
        return sessionId;
    }

    /**
     * Pushes a message to the central buffer to be pulled by clients.
     * @param message the message to push
     */
    public void push(String message) {
        if (!sessions.isEmpty()) {
            bufferedMessages.push(message);
        }
    }

    /**
     * Pulls new messages from the buffer for a specific session.
     * @param session the session pulling the messages
     * @return an array of new messages, or {@code null} if there are no new messages
     */
    public String[] pull(PollingRelaySession session) {
        String[] messages = bufferedMessages.pop(session);
        if (messages != null && messages.length > 0) {
            shrinkBuffer();
        }
        return messages;
    }

    private void shrinkBuffer() {
        int minLineIndex = getMinLineIndex();
        if (minLineIndex > -1) {
            bufferedMessages.shrink(minLineIndex);
        }
    }

    private int getMinLineIndex() {
        int minLineIndex = -1;
        for (PollingRelaySession serviceSession : sessions.values()) {
            if (minLineIndex == -1) {
                minLineIndex = serviceSession.getLastLineIndex();
            } else if (serviceSession.getLastLineIndex() < minLineIndex) {
                minLineIndex = serviceSession.getLastLineIndex();
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
                PollingRelaySession session = entry.getValue();
                if (session.isExpired()) {
                    appMonManager.getMessageRelayManager().release(session);
                    session.destroy();
                    return true;
                }
                return false;
            });
            if (sessions.isEmpty()) {
                bufferedMessages.clear();
            } else {
                shrinkBuffer();
            }
        }
    }

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
        bufferedMessages.clear();
    }

}

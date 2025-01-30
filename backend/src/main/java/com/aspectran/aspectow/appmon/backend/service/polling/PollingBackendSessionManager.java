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
package com.aspectran.aspectow.appmon.backend.service.polling;

import com.aspectran.aspectow.appmon.backend.config.EndpointPollingConfig;
import com.aspectran.aspectow.appmon.manager.AppMonManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.session.SessionIdGenerator;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.annotation.jsr305.Nullable;
import com.aspectran.utils.thread.ScheduledExecutorScheduler;
import com.aspectran.utils.thread.Scheduler;
import com.aspectran.web.support.util.CookieGenerator;
import com.aspectran.web.support.util.WebUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PollingBackendSessionManager extends AbstractComponent {

    private static final String SESSION_ID_COOKIE_NAME = PollingBackendSessionManager.class.getName() + ".SESSION_ID";

    private final CookieGenerator sessionIdCookieGenerator = new CookieGenerator(SESSION_ID_COOKIE_NAME);

    private final SessionIdGenerator sessionIdGenerator = new SessionIdGenerator();

    private final Scheduler scheduler = new ScheduledExecutorScheduler("PollingEndpointSessionScheduler", false);

    private final Map<String, PollingBackendSession> sessions = new ConcurrentHashMap<>();

    private final AppMonManager appMonManager;

    private final BufferedMessages bufferedMessages;

    public PollingBackendSessionManager(AppMonManager appMonManager, int initialBufferSize) {
        this.appMonManager = appMonManager;
        this.bufferedMessages = new BufferedMessages(initialBufferSize);
    }

    public PollingBackendSession createSession(
            @NonNull Translet translet, @Nullable EndpointPollingConfig pollingConfig, String[] joinGroupNames) {
        String sessionId = getSessionId(translet, true);
        PollingBackendSession existingSession = sessions.get(sessionId);
        if (existingSession != null) {
            existingSession.access(false);
            return existingSession;
        } else {
            int pollingInterval = 0;
            int sessionTimeout = 0;
            if (pollingConfig != null) {
                pollingInterval = pollingConfig.getPollingInterval();
                sessionTimeout = pollingConfig.getSessionTimeout();
            }
            if (pollingInterval > 0 && sessionTimeout <= 0) {
                sessionTimeout = pollingInterval * 2;
            }
            PollingBackendSession session = new PollingBackendSession(this, sessionTimeout, pollingInterval);
            if (joinGroupNames != null) {
                session.saveJoinedGroups(joinGroupNames);
            }
            sessions.put(sessionId, session);
            session.access(true);
            return session;
        }
    }

    public PollingBackendSession getSession(@NonNull Translet translet) {
        String sessionId = getSessionId(translet, false);
        if (sessionId == null) {
            return null;
        }
        PollingBackendSession session = sessions.get(sessionId);
        if (session != null) {
            session.access(false);
            return session;
        } else {
            return null;
        }
    }

    private String getSessionId(@NonNull Translet translet, boolean create) {
        HttpServletRequest request = translet.getRequestAdapter().getAdaptee();
        HttpServletResponse response = translet.getResponseAdapter().getAdaptee();
        String cookieName = sessionIdCookieGenerator.getCookieName();
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        String sessionId = null;
        if (cookie != null) {
            sessionId = cookie.getValue();
        }
        if (sessionId == null && create) {
            sessionId = sessionIdGenerator.createSessionId(hashCode());
            sessionIdCookieGenerator.addCookie(response, sessionId);
        }
        return sessionId;
    }

    public void push(String message) {
        if (!sessions.isEmpty()) {
            bufferedMessages.push(message);
        }
    }

    public String[] pull(PollingBackendSession session) {
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
        for (PollingBackendSession session : sessions.values()) {
            if (minLineIndex == -1) {
                minLineIndex = session.getLastLineIndex();
            } else if (session.getLastLineIndex() < minLineIndex) {
                minLineIndex = session.getLastLineIndex();
            }
        }
        return minLineIndex;
    }

    protected boolean isUsingGroup(String groupName) {
        if (StringUtils.hasLength(groupName)) {
            for (PollingBackendSession session : sessions.values()) {
                if (session.isValid()) {
                    String[] joinedGroups = session.getJoinedGroups();
                    if (joinedGroups != null) {
                        for (String name : joinedGroups) {
                            if (groupName.equals(name)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    protected void scavenge() {
        List<String> expiredSessions = new ArrayList<>();
        for (Map.Entry<String, PollingBackendSession> entry : sessions.entrySet()) {
            String id = entry.getKey();
            PollingBackendSession session = entry.getValue();
            if (session.isExpired()) {
                appMonManager.release(session);
                session.destroy();
                expiredSessions.add(id);
            }
        }
        for (String id : expiredSessions) {
            sessions.remove(id);
        }
        if (sessions.isEmpty()) {
            bufferedMessages.clear();
        } else {
            shrinkBuffer();
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

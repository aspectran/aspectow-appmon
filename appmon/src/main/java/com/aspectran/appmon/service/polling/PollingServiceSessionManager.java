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

import com.aspectran.appmon.config.PollingConfig;
import com.aspectran.appmon.manager.AppMonManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.session.SessionIdGenerator;
import com.aspectran.utils.CopyOnWriteMap;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.scheduling.ScheduledExecutorScheduler;
import com.aspectran.utils.scheduling.Scheduler;
import com.aspectran.web.support.util.CookieGenerator;
import com.aspectran.web.support.util.WebUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PollingServiceSessionManager extends AbstractComponent {

    private static final String SESSION_ID_COOKIE_NAME = PollingServiceSessionManager.class.getName() + ".SESSION_ID";

    private final CookieGenerator sessionIdCookieGenerator = new CookieGenerator(SESSION_ID_COOKIE_NAME);

    private final SessionIdGenerator sessionIdGenerator = new SessionIdGenerator();

    private final Scheduler scheduler = new ScheduledExecutorScheduler("PSSM-Scheduler", false);

    private final Map<String, PollingServiceSession> sessions = new CopyOnWriteMap<>();

    private final AppMonManager appMonManager;

    private final BufferedMessages bufferedMessages;

    public PollingServiceSessionManager(@NonNull AppMonManager appMonManager) {
        this.appMonManager = appMonManager;

        PollingConfig pollingConfig = appMonManager.getPollingConfig();
        this.bufferedMessages = new BufferedMessages(pollingConfig.getInitialBufferSize());
    }

    public PollingServiceSession createSession(
            @NonNull Translet translet, @NonNull PollingConfig pollingConfig, String[] instanceNames) {
        int pollingInterval = pollingConfig.getPollingInterval();
        int sessionTimeout = pollingConfig.getSessionTimeout();
        if (pollingInterval > 0 && sessionTimeout <= 0) {
            sessionTimeout = pollingInterval * 2;
        }

        String sessionId = getSessionId(translet, true);
        PollingServiceSession existingSession = sessions.get(sessionId);
        if (existingSession != null) {
            existingSession.access(false);
            existingSession.setSessionTimeout(sessionTimeout);
            existingSession.setPollingInterval(pollingInterval);
            return existingSession;
        } else {
            PollingServiceSession newSession = new PollingServiceSession(this);
            newSession.setSessionTimeout(sessionTimeout);
            newSession.setPollingInterval(pollingInterval);
            if (instanceNames != null) {
                newSession.setJoinedInstances(instanceNames);
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

    public PollingServiceSession getSession(@NonNull Translet translet) {
        String sessionId = getSessionId(translet, false);
        if (sessionId == null) {
            return null;
        }
        PollingServiceSession serviceSession = sessions.get(sessionId);
        if (serviceSession != null) {
            serviceSession.access(false);
            return serviceSession;
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

    public String[] pull(PollingServiceSession session) {
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
        for (PollingServiceSession serviceSession : sessions.values()) {
            if (minLineIndex == -1) {
                minLineIndex = serviceSession.getLastLineIndex();
            } else if (serviceSession.getLastLineIndex() < minLineIndex) {
                minLineIndex = serviceSession.getLastLineIndex();
            }
        }
        return minLineIndex;
    }

    protected boolean isUsingInstance(String instanceName) {
        if (StringUtils.hasLength(instanceName)) {
            synchronized (sessions) {
                for (PollingServiceSession serviceSession : sessions.values()) {
                    if (serviceSession.isValid()) {
                        String[] instanceNames = serviceSession.getJoinedInstances();
                        if (instanceNames != null) {
                            for (String name : instanceNames) {
                                if (instanceName.equals(name)) {
                                    return true;
                                }
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
        for (Map.Entry<String, PollingServiceSession> entry : sessions.entrySet()) {
            String id = entry.getKey();
            PollingServiceSession session = entry.getValue();
            if (session.isExpired()) {
                appMonManager.getExportServiceManager().release(session);
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

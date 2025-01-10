/*
 * Copyright (c) 2018-2025 The Aspectran Project
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
package com.aspectran.aspectow.appmon.exporter.state.session;

import com.aspectran.aspectow.appmon.config.StateInfo;
import com.aspectran.aspectow.appmon.exporter.state.StateExporter;
import com.aspectran.aspectow.appmon.exporter.state.StateExporterManager;
import com.aspectran.aspectow.appmon.exporter.state.StateReader;
import com.aspectran.core.component.UnavailableException;
import com.aspectran.core.component.bean.NoSuchBeanException;
import com.aspectran.core.component.session.ManagedSession;
import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionHandler;
import com.aspectran.core.component.session.SessionListenerRegistration;
import com.aspectran.core.component.session.SessionStatistics;
import com.aspectran.undertow.server.TowServer;
import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.json.JsonBuilder;
import com.aspectran.utils.json.JsonString;
import com.aspectran.utils.logging.Logger;
import com.aspectran.utils.logging.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SessionStateReader implements StateReader {

    private static final Logger logger = LoggerFactory.getLogger(SessionStateReader.class);

    public static final String USER_SESSION_KEY = "user";

    private final StateExporterManager stateExporterManager;

    private final StateInfo stateInfo;

    private final String deploymentName;

    private final SessionHandler sessionHandler;

    private SessionStateListener sessionListener;

    private volatile SessionStatePayload oldPayload;

    public SessionStateReader(@NonNull StateExporterManager stateExporterManager,
                              @NonNull StateInfo stateInfo) {
        this.stateExporterManager = stateExporterManager;
        this.stateInfo = stateInfo;

        String[] arr = StringUtils.split(stateInfo.getTarget(), '/', 2);
        String serverId = arr[0];
        String deploymentName = arr[1];

        try {
            TowServer towServer = stateExporterManager.getBean(serverId);
            this.sessionHandler = towServer.getSessionHandler(deploymentName);
            this.deploymentName = deploymentName;
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve session handler with " + stateInfo.getTarget(), e);
        }
    }

    public StateExporter getStateExporter() {
        return stateExporterManager.getExporter(stateInfo.getName());
    }

    @Override
    public void start() {
        if (sessionHandler != null) {
            sessionListener = new SessionStateListener(this);
            getSessionListenerRegistration().register(sessionListener, deploymentName);
        }
    }

    @Override
    public void stop() {
        if (sessionHandler != null) {
            oldPayload = null;
            if (sessionListener != null) {
                try {
                    getSessionListenerRegistration().remove(sessionListener, deploymentName);
                } catch (UnavailableException e) {
                    // ignored
                }
            }
        }
    }

    @NonNull
    private SessionListenerRegistration getSessionListenerRegistration() {
        try {
            return stateExporterManager.getBean(SessionListenerRegistration.class);
        } catch (NoSuchBeanException e) {
            throw new IllegalStateException("Bean for SessionListenerRegistration must be defined", e);
        }
    }

    @Override
    public String read() {
        if (sessionListener == null) {
            return null;
        }
        try {
            SessionStatePayload payload = loadWithActiveSessions();
            oldPayload = payload;
            return payload.toJson();
        } catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public String readIfChanged() {
        if (sessionListener == null) {
            return null;
        }
        try {
            SessionStatePayload payload = load();
            if (!payload.equals(oldPayload)) {
                oldPayload = payload;
                return payload.toJson();
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    String readWithCreatedSession(Session session) {
        SessionStatePayload payload = load();
        oldPayload = payload;
        payload.setCreatedSessions(new JsonString[] { serialize(session) });
        return payload.toJson();
    }

    String readWithDestroyedSession(String sessionId) {
        SessionStatePayload payload = load();
        oldPayload = payload;
        payload.setDestroyedSessions(new String[] { sessionId });
        return payload.toJson();
    }

    String readWithEvictedSession(String sessionId) {
        SessionStatePayload payload = load();
        oldPayload = payload;
        payload.setEvictedSessions(new String[] { sessionId });
        return payload.toJson();
    }

    String readWithResidedSession(Session session) {
        SessionStatePayload payload = load();
        oldPayload = payload;
        payload.setResidedSessions(new JsonString[] { serialize(session) });
        return payload.toJson();
    }

    SessionStatePayload loadWithActiveSessions() {
        SessionStatePayload payload = load();
        payload.setCreatedSessions(getAllActiveSessions());
        return payload;
    }

    @NonNull
    private SessionStatePayload load() {
        SessionStatistics statistics = sessionHandler.getStatistics();
        SessionStatePayload payload = new SessionStatePayload();
        payload.setNumberOfCreated(statistics.getNumberOfCreated());
        payload.setNumberOfExpired(statistics.getNumberOfExpired());
        payload.setNumberOfActives(statistics.getNumberOfActives());
        payload.setHighestNumberOfActives(statistics.getHighestNumberOfActives());
        payload.setNumberOfUnmanaged(Math.abs(statistics.getNumberOfUnmanaged()));
        payload.setNumberOfRejected(statistics.getNumberOfRejected());
        payload.setElapsedTime(formatDuration(statistics.getStartTime()));
        return payload;
    }

    @NonNull
    private JsonString[] getAllActiveSessions() {
        Set<String> sessionIds = sessionHandler.getActiveSessions();
        List<JsonString> list = new ArrayList<>(sessionIds.size());
        for (String sessionId : sessionIds) {
            ManagedSession session = sessionHandler.getSession(sessionId);
            if (session != null) {
                list.add(serialize(session));
            }
        }
        return list.toArray(new JsonString[0]);
    }

    private static JsonString serialize(Session session) {
        Assert.notNull(session, "Session must not be null");
        return new JsonBuilder()
                .nullWritable(false)
                .prettyPrint(false)
                .object()
                    .put("sessionId", session.getId())
                    .put("username", session.getAttribute("user.name"))
                    .put("countryCode", session.getAttribute("user.countryCode"))
                    .put("ipAddress", session.getAttribute("user.ipAddress"))
                    .put("createAt", formatTime(session.getCreationTime()))
                .endObject()
                .toJsonString();
    }

    @NonNull
    private static String formatTime(long time) {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        return date.toString();
    }

    @NonNull
    private static String formatDuration(long startTime) {
        Instant start = Instant.ofEpochMilli(startTime);
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        long seconds = duration.getSeconds();
        return String.format(
                "%02d:%02d:%02d",
                seconds / 3600,
                (seconds % 3600) / 60,
                seconds % 60);
    }

}

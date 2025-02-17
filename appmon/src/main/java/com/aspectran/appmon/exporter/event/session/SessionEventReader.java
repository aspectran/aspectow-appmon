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
package com.aspectran.appmon.exporter.event.session;

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.appmon.exporter.event.AbstractEventReader;
import com.aspectran.appmon.exporter.event.EventExporter;
import com.aspectran.appmon.exporter.event.EventExporterManager;
import com.aspectran.core.component.UnavailableException;
import com.aspectran.core.component.bean.NoSuchBeanException;
import com.aspectran.core.component.session.ManagedSession;
import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListenerRegistration;
import com.aspectran.core.component.session.SessionManager;
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

public class SessionEventReader extends AbstractEventReader {

    private static final Logger logger = LoggerFactory.getLogger(SessionEventReader.class);

    private static final String USER_NAME = "user.name";
    private static final String USER_COUNTRY_CODE = "user.countryCode";
    private static final String USER_IP_ADDRESS = "user.ipAddress";

    private final String serverId;

    private final String deploymentName;

    private EventExporter eventExporter;

    private SessionManager sessionManager;

    private SessionEventReadingListener sessionListener;

    private volatile SessionEventData oldData;

    public SessionEventReader(@NonNull EventExporterManager eventExporterManager,
                              @NonNull EventInfo eventInfo) {
        super(eventExporterManager, eventInfo);

        String[] arr = StringUtils.divide(eventInfo.getTarget(), "/");
        this.serverId = arr[0];
        this.deploymentName = arr[1];
    }

    EventExporter getEventExporter() {
        if (eventExporter == null) {
            eventExporter = getEventExporterManager().getExporter(getEventInfo().getName());
        }
        return eventExporter;
    }

    @Override
    public void start() {
        try {
            TowServer towServer = getEventExporterManager().getBean(serverId);
            sessionManager = towServer.getSessionManager(deploymentName);
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve session manager with " + getEventInfo().getTarget(), e);
        }
        if (sessionManager != null) {
            sessionListener = new SessionEventReadingListener(this);
            getSessionListenerRegistration().register(sessionListener, deploymentName);
        }
    }

    @Override
    public void stop() {
        if (sessionManager != null) {
            oldData = null;
            if (sessionListener != null) {
                try {
                    getSessionListenerRegistration().remove(sessionListener, deploymentName);
                    sessionListener = null;
                } catch (UnavailableException e) {
                    // ignored
                }
            }
        }
    }

    @NonNull
    private SessionListenerRegistration getSessionListenerRegistration() {
        try {
            return getEventExporterManager().getBean(SessionListenerRegistration.class);
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
            SessionEventData data = loadWithActiveSessions();
            oldData = data;
            return data.toJson();
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
            SessionEventData data = (oldData == null ? loadWithActiveSessions() : load());
            if (!data.equals(oldData)) {
                oldData = data;
                return data.toJson();
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    void sessionCreated(@NonNull Session session) {
        String json = readWithCreatedSession(session);
        getEventExporter().broadcast(json);
    }

    void sessionDestroyed(@NonNull Session session) {
        String json = readWithDestroyedSession(session.getId());
        getEventExporter().broadcast(json);
    }

    void sessionEvicted(@NonNull Session session) {
        String json = readWithEvictedSession(session.getId());
        getEventExporter().broadcast(json);
    }

    void sessionResided(@NonNull Session session) {
        String json = readWithResidedSession(session);
        getEventExporter().broadcast(json);
    }

    void attributeAdded(Session session, String name) {
        if (USER_NAME.equals(name)) {
            sessionCreated(session);
        }
    }

    void attributeUpdated(Session session, String name) {
        if (USER_NAME.equals(name)) {
            sessionCreated(session);
        }
    }

    private String readWithCreatedSession(Session session) {
        SessionEventData data = load();
        oldData = data;
        data.setCreatedSessions(new JsonString[] { serialize(session) });
        return data.toJson();
    }

    private String readWithDestroyedSession(String sessionId) {
        SessionEventData data = load();
        oldData = data;
        data.setDestroyedSessions(new String[] { sessionId });
        return data.toJson();
    }

    private String readWithEvictedSession(String sessionId) {
        SessionEventData data = load();
        oldData = data;
        data.setEvictedSessions(new String[] { sessionId });
        return data.toJson();
    }

    private String readWithResidedSession(Session session) {
        SessionEventData data = load();
        oldData = data;
        data.setResidedSessions(new JsonString[] { serialize(session) });
        return data.toJson();
    }

    @NonNull
    private SessionEventData loadWithActiveSessions() {
        SessionEventData data = load();
        data.setCreatedSessions(getAllActiveSessions());
        return data;
    }

    @NonNull
    private SessionEventData load() {
        SessionStatistics statistics = sessionManager.getStatistics();
        SessionEventData data = new SessionEventData();
        data.setNumberOfCreated(statistics.getNumberOfCreated());
        data.setNumberOfExpired(statistics.getNumberOfExpired());
        data.setNumberOfActives(statistics.getNumberOfActives());
        data.setHighestNumberOfActives(statistics.getHighestNumberOfActives());
        data.setNumberOfUnmanaged(Math.abs(statistics.getNumberOfUnmanaged()));
        data.setNumberOfRejected(statistics.getNumberOfRejected());
        data.setElapsedTime(formatDuration(statistics.getStartTime()));
        return data;
    }

    @NonNull
    private JsonString[] getAllActiveSessions() {
        Set<String> sessionIds = sessionManager.getActiveSessions();
        List<JsonString> list = new ArrayList<>(sessionIds.size());
        for (String sessionId : sessionIds) {
            ManagedSession session = sessionManager.getSession(sessionId);
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
                    .put("username", session.getAttribute(USER_NAME))
                    .put("countryCode", session.getAttribute(USER_COUNTRY_CODE))
                    .put("ipAddress", session.getAttribute(USER_IP_ADDRESS))
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

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
package com.aspectran.appmon.exporter.event.session;

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.appmon.exporter.event.AbstractEventReader;
import com.aspectran.appmon.persist.counter.EventCount;
import com.aspectran.core.component.UnavailableException;
import com.aspectran.core.component.session.ManagedSession;
import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListenerRegistration;
import com.aspectran.core.component.session.SessionManager;
import com.aspectran.core.component.session.SessionStatistics;
import com.aspectran.undertow.server.TowServer;
import com.aspectran.undertow.support.SessionListenerRegistrationBean;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.json.JsonBuilder;
import com.aspectran.utils.json.JsonString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionEventReader extends AbstractEventReader {

    private static final Logger logger = LoggerFactory.getLogger(SessionEventReader.class);

    public static final String USER_NAME = "user.name";
    public static final String USER_IP_ADDRESS = "user.ipAddress";
    public static final String USER_COUNTRY_CODE = "user.countryCode";
    public static final String USER_ACTIVITY_COUNT = "user.activityCount";

    private String serverId;

    private String deploymentName;

    private SessionManager sessionManager;

    private SessionEventReadingListener sessionListener;

    private volatile SessionEventData oldData;

    public SessionEventReader(
            @NonNull ExporterManager exporterManager,
            @NonNull EventInfo eventInfo,
            @NonNull EventCount eventCount) {
        super(exporterManager, eventInfo, eventCount);
    }

    @Override
    public void init() throws Exception {
        String[] arr = StringUtils.divide(getEventInfo().getTarget(), "/");
        serverId = arr[0];
        deploymentName = arr[1];
    }

    @Override
    public void start() {
        try {
            TowServer towServer = getExporterManager().getBean(serverId);
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
                } catch (UnavailableException e) {
                    // ignored
                }
                sessionListener = null;
            }
        }
    }

    @NonNull
    private SessionListenerRegistration getSessionListenerRegistration() {
        SessionListenerRegistration sessionListenerRegistration;
        if (getExporterManager().containsBean(SessionListenerRegistration.class)) {
            sessionListenerRegistration = getExporterManager().getBean(SessionListenerRegistration.class);
        } else {
            if (getExporterManager().containsBean(TowServer.class)) {
                sessionListenerRegistration = new SessionListenerRegistrationBean();
            } else {
                throw new IllegalStateException("Bean for SessionListenerRegistration must be defined");
            }
        }
        return sessionListenerRegistration;
    }

    @Override
    public String read() {
        if (sessionListener == null) {
            return null;
        }
        try {
            if (oldData != null) {
                return oldData.toJson();
            } else {
                SessionEventData data = loadWithActiveSessions();
                oldData = data;
                return data.toJson();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
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
        data.setStartTime(formatTime(statistics.getStartTime()));
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

    private static JsonString serialize(@NonNull Session session) {
        AtomicInteger count = session.getAttribute(USER_ACTIVITY_COUNT);
        return new JsonBuilder()
                .nullWritable(false)
                .prettyPrint(false)
                .object()
                    .put("sessionId", session.getId())
                    .put("username", session.getAttribute(USER_NAME))
                    .put("ipAddress", session.getAttribute(USER_IP_ADDRESS))
                    .put("countryCode", session.getAttribute(USER_COUNTRY_CODE))
                    .put("activityCount", (count != null ? count.get() : 0))
                    .put("createAt", formatTime(session.getCreationTime()))
                    .put("inactiveInterval", session.getMaxInactiveInterval())
                    .put("tempResident", session.isTempResident())
                .endObject()
                .toJsonString();
    }

    @NonNull
    private static String formatTime(long time) {
        return Instant.ofEpochMilli(time).toString();
    }

}

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
package com.aspectran.aspectow.appmon.engine.exporter.event.session;

import com.aspectran.aspectow.appmon.common.listener.UserTrackingListener;
import com.aspectran.aspectow.appmon.common.support.IPCountryResolver;
import com.aspectran.aspectow.appmon.common.support.SessionUserResolver;
import com.aspectran.aspectow.appmon.engine.config.EventInfo;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.engine.exporter.event.AbstractEventReader;
import com.aspectran.aspectow.appmon.engine.persist.counter.EventCount;
import com.aspectran.core.component.session.ManagedSession;
import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.core.component.session.SessionManager;
import com.aspectran.core.component.session.SessionManagerProvider;
import com.aspectran.core.component.session.SessionStatistics;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.BeanUtils;
import com.aspectran.utils.ClassUtils;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.json.JsonBuilder;
import com.aspectran.utils.json.JsonString;
import com.aspectran.utils.lifecycle.LifeCycle;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads session statistics and events from a {@link SessionManager}.
 * It registers a {@link SessionListener} to receive real-time session lifecycle events.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class SessionEventReader extends AbstractEventReader {

    private static final Logger logger = LoggerFactory.getLogger(SessionEventReader.class);

    public static final String PARAMETER_USERNAME_ATTRIBUTE = "usernameAttribute";
    public static final String PARAMETER_USER_RESOLVER = "userResolver";

    public static final String USER_NAME = "user.name";
    public static final String USER_IP_ADDRESS = "user.ipAddress";
    public static final String USER_COUNTRY_CODE = "user.countryCode";
    public static final String USER_ACTIVITY_COUNT = "user.activityCount";

    private static final Map<String, UserTrackingListener> registeredTrackingListeners = new ConcurrentHashMap<>();

    private String serverId;

    private String contextName;

    private SessionUserResolver userResolver;

    private String usernameAttribute;

    private String rootAttributeName;

    private String nestedPropertyPath;

    private LifeCycle serverLifeCycle;

    private LifeCycle.Listener serverLifeCycleListener;

    private SessionManager sessionManager;

    private SessionEventReadingListener sessionListener;

    private UserTrackingListener userTrackingListener;

    private volatile boolean changed;

    /**
     * Instantiates a new SessionEventReader.
     * @param exporterManager the exporter manager
     * @param eventInfo the event configuration
     * @param eventCount the event counter
     */
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
        contextName = (arr[1] != null ? arr[1] : "");

        SessionManagerProvider server = (serverId != null ? getExporterManager().getBean(serverId) : null);
        if (server instanceof LifeCycle lifeCycle) {
            this.serverLifeCycle = lifeCycle;
            if (lifeCycle.isStarted()) {
                setupSessionManager(server);
            } else {
                this.serverLifeCycleListener = new LifeCycle.Listener() {
                    @Override
                    public void lifeCycleStarted(LifeCycle event) {
                        synchronized (SessionEventReader.this) {
                            setupSessionManager(server);
                            if (sessionListener != null && sessionManager != null) {
                                try {
                                    sessionManager.addSessionListener(sessionListener);
                                } catch (Exception e) {
                                    logger.warn("Failed to register SessionEventReadingListener on server start", e);
                                }
                            }
                        }
                    }

                    @Override
                    public void lifeCycleStopped(LifeCycle event) {
                        synchronized (SessionEventReader.this) {
                            sessionManager = null;
                        }
                    }
                };
                lifeCycle.addLifeCycleListener(serverLifeCycleListener);
            }
        } else if (server != null) {
            setupSessionManager(server);
        }

        if (getEventInfo().hasParameters()) {
            Parameters params = getEventInfo().getParameters();
            String userResolverParam = params.getString(PARAMETER_USER_RESOLVER);
            if (StringUtils.hasText(userResolverParam)) {
                if (getExporterManager().containsBean(userResolverParam)) {
                    userResolver = getExporterManager().getBean(userResolverParam);
                } else {
                    Class<?> resolverType = ClassUtils.classForName(userResolverParam);
                    userResolver = (SessionUserResolver)ClassUtils.createInstance(resolverType);
                }
            } else if (getExporterManager().containsBean(SessionUserResolver.class)) {
                userResolver = getExporterManager().getBean(SessionUserResolver.class);
            }

            usernameAttribute = params.getString(PARAMETER_USERNAME_ATTRIBUTE);
            if (StringUtils.hasText(usernameAttribute)) {
                int dotIdx = usernameAttribute.indexOf('.');
                if (dotIdx > 0) {
                    rootAttributeName = usernameAttribute.substring(0, dotIdx);
                    nestedPropertyPath = usernameAttribute.substring(dotIdx + 1);
                } else {
                    rootAttributeName = usernameAttribute;
                    nestedPropertyPath = null;
                }
            } else {
                rootAttributeName = USER_NAME;
                nestedPropertyPath = null;
            }
        } else {
            if (getExporterManager().containsBean(SessionUserResolver.class)) {
                userResolver = getExporterManager().getBean(SessionUserResolver.class);
            }
            rootAttributeName = USER_NAME;
            nestedPropertyPath = null;
        }
    }

    private void setupSessionManager(@NonNull SessionManagerProvider server) {
        try {
            SessionManager sm = (StringUtils.hasLength(contextName) ?
                    server.getSessionManager(contextName) : server.getSessionManager());
            if (sm != null) {
                this.sessionManager = sm;
                registerUserTrackingListener(sm);
            } else {
                logger.warn("Unable to obtain session manager from {}", getEventInfo().getTarget());
            }
        } catch (Exception e) {
            logger.warn("Cannot resolve session manager with {}", getEventInfo().getTarget(), e);
        }
    }

    private void registerUserTrackingListener(@NonNull SessionManager sm) {
        String targetKey = serverId + "/" + contextName;
        ActivityContext currentContext = getExporterManager().getAppMonManager().getActivityContext();
        synchronized (registeredTrackingListeners) {
            UserTrackingListener existingListener = registeredTrackingListeners.get(targetKey);
            if (existingListener == null || existingListener.getActivityContext() != currentContext) {
                if (existingListener != null) {
                    try {
                        sm.removeSessionListener(existingListener);
                    } catch (Exception e) {
                        // ignored
                    }
                }
                IPCountryResolver ipCountryResolver = null;
                if (getExporterManager().containsBean(IPCountryResolver.class)) {
                    ipCountryResolver = getExporterManager().getBean(IPCountryResolver.class);
                }
                UserTrackingListener newListener = new UserTrackingListener(currentContext, ipCountryResolver);
                try {
                    sm.addSessionListener(newListener);
                    registeredTrackingListeners.put(targetKey, newListener);
                    this.userTrackingListener = newListener;
                } catch (Exception e) {
                    logger.warn("Failed to register UserTrackingListener for {}", targetKey, e);
                }
            } else {
                this.userTrackingListener = existingListener;
            }
        }
    }

    @Override
    public synchronized void start() {
        if (sessionManager == null) {
            sessionManager = resolveSessionManager();
            if (sessionManager != null && userTrackingListener == null) {
                registerUserTrackingListener(sessionManager);
            }
        }
        if (sessionManager != null) {
            if (sessionListener == null) {
                sessionListener = new SessionEventReadingListener(this);
                sessionManager.addSessionListener(sessionListener);
                changed = true;
            }
        } else {
            if (serverLifeCycle != null && !serverLifeCycle.isStarted()) {
                sessionListener = new SessionEventReadingListener(this);
                changed = true;
            } else {
                throw new RuntimeException("Cannot resolve session manager with " + getEventInfo().getTarget());
            }
        }
    }

    @Override
    public synchronized void stop() {
        changed = false;
        if (sessionManager != null && sessionListener != null) {
            try {
                sessionManager.removeSessionListener(sessionListener);
            } catch (Exception e) {
                // ignored
            }
        }
        sessionListener = null;
    }

    @Override
    public synchronized void destroy() {
        if (serverLifeCycle != null && serverLifeCycleListener != null) {
            try {
                serverLifeCycle.removeLifeCycleListener(serverLifeCycleListener);
            } catch (Exception e) {
                // ignored
            }
            serverLifeCycleListener = null;
            serverLifeCycle = null;
        }
        if (userTrackingListener != null) {
            String targetKey = serverId + "/" + contextName;
            synchronized (registeredTrackingListeners) {
                if (registeredTrackingListeners.remove(targetKey, userTrackingListener)) {
                    SessionManager sm = (sessionManager != null ? sessionManager : resolveSessionManager());
                    if (sm != null) {
                        try {
                            sm.removeSessionListener(userTrackingListener);
                        } catch (Exception e) {
                            // ignored
                        }
                    }
                }
            }
            userTrackingListener = null;
        }
        sessionManager = null;
    }

    @Nullable
    private SessionManager resolveSessionManager() {
        if (serverId == null) {
            return null;
        }
        try {
            SessionManagerProvider server = getExporterManager().getBean(serverId);
            return (StringUtils.hasLength(contextName) ?
                    server.getSessionManager(contextName) : server.getSessionManager());
        } catch (Exception e) {
            logger.warn("Cannot resolve session manager with {}", getEventInfo().getTarget(), e);
            return null;
        }
    }

    @Override
    public String read() {
        if (sessionListener == null) {
            return null;
        }
        try {
            SessionEventData data = loadWithActiveSessions();
            changed = false;
            return data.toJson();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean hasChanges() {
        return (sessionListener != null && changed);
    }

    void sessionCreated(@NonNull Session session) {
        changed = true;
        String json = readWithCreatedSession(session);
        getEventExporter().broadcast(json);
    }

    void sessionDestroyed(@NonNull Session session) {
        changed = true;
        String json = readWithDestroyedSession(session.getId());
        getEventExporter().broadcast(json);
    }

    void sessionEvicted(@NonNull Session session) {
        changed = true;
        String json = readWithEvictedSession(session.getId());
        getEventExporter().broadcast(json);
    }

    void sessionResided(@NonNull Session session) {
        changed = true;
        String json = readWithResidedSession(session);
        getEventExporter().broadcast(json);
    }

    void attributeAdded(Session session, String name) {
        if ((rootAttributeName != null && rootAttributeName.equals(name)) || USER_NAME.equals(name)) {
            sessionCreated(session);
        }
    }

    void attributeUpdated(Session session, String name) {
        if ((rootAttributeName != null && rootAttributeName.equals(name)) || USER_NAME.equals(name)) {
            sessionCreated(session);
        }
    }

    private String readWithCreatedSession(Session session) {
        SessionEventData data = load();
        data.setCreatedSessions(new JsonString[] { serialize(session) });
        return data.toJson();
    }

    private String readWithDestroyedSession(String sessionId) {
        SessionEventData data = load();
        data.setDestroyedSessions(new String[] { sessionId });
        return data.toJson();
    }

    private String readWithEvictedSession(String sessionId) {
        SessionEventData data = load();
        data.setEvictedSessions(new String[] { sessionId });
        return data.toJson();
    }

    private String readWithResidedSession(Session session) {
        SessionEventData data = load();
        data.setResidedSessions(new JsonString[] { serialize(session) });
        return data.toJson();
    }

    @NonNull
    private SessionEventData loadWithActiveSessions() {
        SessionEventData data = load();
        data.setFullSync(true);
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

    private JsonString @NonNull [] getAllActiveSessions() {
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

    private JsonString serialize(@NonNull Session session) {
        AtomicInteger count = session.getAttribute(USER_ACTIVITY_COUNT);
        return new JsonBuilder()
                .nullWritable(false)
                .prettyPrint(false)
                .object()
                    .put("sessionId", session.getId())
                    .put("username", resolveUsername(session))
                    .put("ipAddress", session.getAttribute(USER_IP_ADDRESS))
                    .put("countryCode", session.getAttribute(USER_COUNTRY_CODE))
                    .put("activityCount", (count != null ? count.get() : 0))
                    .put("createAt", formatTime(session.getCreationTime()))
                    .put("inactiveInterval", session.getMaxInactiveInterval())
                    .put("tempResident", session.isTempResident())
                .endObject()
                .toJsonString();
    }

    @Nullable
    private String resolveUsername(@NonNull Session session) {
        if (userResolver != null) {
            try {
                String username = userResolver.resolveUsername(contextName, session);
                if (StringUtils.hasLength(username)) {
                    return username;
                }
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to resolve username using userResolver for context {}", contextName, e);
                }
            }
        }
        if (StringUtils.hasText(usernameAttribute)) {
            try {
                Object rootObj = session.getAttribute(rootAttributeName);
                if (rootObj != null) {
                    if (nestedPropertyPath != null) {
                        Object propVal = BeanUtils.getProperty(rootObj, nestedPropertyPath);
                        if (propVal != null) {
                            return propVal.toString();
                        }
                    } else {
                        return rootObj.toString();
                    }
                }
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to resolve username attribute '{}' for context {}", usernameAttribute, contextName, e);
                }
            }
        }
        Object username = session.getAttribute(USER_NAME);
        return (username != null ? username.toString() : null);
    }

    @NonNull
    private static String formatTime(long time) {
        return Instant.ofEpochMilli(time).toString();
    }

}

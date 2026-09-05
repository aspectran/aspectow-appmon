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
import com.aspectran.core.component.UnavailableException;
import com.aspectran.core.component.bean.aware.ActivityContextAware;
import com.aspectran.core.component.session.ManagedSession;
import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.core.component.session.SessionListenerRegistration;
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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

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

        String targetKey = serverId + "/" + contextName;
        IPCountryResolver ipCountryResolver = null;
        if (getExporterManager().containsBean(IPCountryResolver.class)) {
            ipCountryResolver = getExporterManager().getBean(IPCountryResolver.class);
        }
        ActivityContext context = getExporterManager().getAppMonManager().getActivityContext();
        UserTrackingListener newListener = new UserTrackingListener(context, ipCountryResolver);
        UserTrackingListener oldListener = registeredTrackingListeners.put(targetKey, newListener);

        SessionListenerRegistration registration = getSessionListenerRegistration();
        if (oldListener != null) {
            try {
                registration.remove(oldListener, contextName);
            } catch (Exception e) {
                // ignored
            }
        }
        registration.register(newListener, contextName);
        this.userTrackingListener = newListener;

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

    @Override
    public void start() {
        try {
            SessionManagerProvider server = getExporterManager().getBean(serverId);
            sessionManager = (StringUtils.hasLength(contextName) ?
                    server.getSessionManager(contextName) : server.getSessionManager());
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve session manager with " + getEventInfo().getTarget(), e);
        }
        if (sessionManager != null) {
            sessionListener = new SessionEventReadingListener(this);
            getSessionListenerRegistration().register(sessionListener, contextName);
            changed = true;
        }
    }

    @Override
    public void stop() {
        if (sessionManager != null) {
            changed = false;
            if (sessionListener != null) {
                try {
                    getSessionListenerRegistration().remove(sessionListener, contextName);
                } catch (UnavailableException e) {
                    // ignored
                }
                sessionListener = null;
            }
            if (userTrackingListener != null) {
                String targetKey = serverId + "/" + contextName;
                registeredTrackingListeners.remove(targetKey, userTrackingListener);
                try {
                    getSessionListenerRegistration().remove(userTrackingListener, contextName);
                } catch (UnavailableException e) {
                    // ignored
                }
                userTrackingListener = null;
            }
        }
    }

    @NonNull
    private SessionListenerRegistration getSessionListenerRegistration() {
        if (getExporterManager().containsBean(SessionListenerRegistration.class)) {
            return getExporterManager().getBean(SessionListenerRegistration.class);
        }
        return createSessionListenerRegistrationFallback();
    }

    @NonNull
    private SessionListenerRegistration createSessionListenerRegistrationFallback() {
        ActivityContext context = getExporterManager().getAppMonManager().getActivityContext();
        String[] candidateClasses = {
            "com.aspectran.undertow.support.SessionListenerRegistrationBean",
            "com.aspectran.netty.support.SessionListenerRegistrationBean"
        };
        for (String className : candidateClasses) {
            try {
                Class<?> clazz = ClassUtils.classForName(className);
                Constructor<?> ctor = clazz.getConstructor(String.class, String.class);
                SessionListenerRegistration registration =
                        (SessionListenerRegistration) ctor.newInstance(serverId, contextName);
                if (registration instanceof ActivityContextAware aware) {
                    aware.setActivityContext(context);
                }
                return registration;
            } catch (ClassNotFoundException ignored) {
                // Ignore and try the next candidate
            } catch (Exception e) {
                logger.warn("Failed to instantiate {}", className, e);
            }
        }
        throw new IllegalStateException("Bean for SessionListenerRegistration must be defined");
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

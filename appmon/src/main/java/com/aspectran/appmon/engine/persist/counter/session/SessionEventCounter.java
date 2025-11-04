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
package com.aspectran.appmon.engine.persist.counter.session;

import com.aspectran.appmon.engine.config.EventInfo;
import com.aspectran.appmon.engine.persist.counter.EventCounter;
import com.aspectran.appmon.engine.persist.counter.AbstractEventCounter;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.core.component.session.SessionListenerRegistration;
import com.aspectran.core.component.session.SessionManager;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.core.service.ServiceHoldingListener;
import com.aspectran.undertow.server.TowServer;
import com.aspectran.undertow.support.SessionListenerRegistrationBean;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;

/**
 * An {@link EventCounter} for counting session creation events.
 * It registers a {@link SessionListener} to receive notifications when sessions are created.
 *
 * <p>Created: 2025-02-12</p>
 */
public class SessionEventCounter extends AbstractEventCounter {

    private final String serverId;

    private final String deploymentName;

    /**
     * Instantiates a new SessionEventCounter.
     * @param eventInfo the event configuration
     */
    public SessionEventCounter(@NonNull EventInfo eventInfo) {
        super(eventInfo);

        String[] arr = StringUtils.divide(eventInfo.getTarget(), "/");
        this.serverId = arr[0];
        this.deploymentName = arr[1];
    }

    @Override
    public void initialize() throws Exception {
        final SessionListener sessionListener = new SessionEventCountingListener(this);
        ActivityContext context = CoreServiceHolder.findActivityContext(deploymentName);
        if (context != null) {
            registerSessionListener(context, sessionListener);
        } else {
            CoreServiceHolder.addServiceHoldingListener(new ServiceHoldingListener() {
                @Override
                public void afterServiceHolding(CoreService service) {
                    if (service.getActivityContext() != null) {
                        String contextName = service.getActivityContext().getName();
                        if (contextName != null && contextName.equals(deploymentName)) {
                            registerSessionListener(service.getActivityContext(), sessionListener);
                        }
                    }
                }
            });
        }
    }

    private void registerSessionListener(@NonNull ActivityContext context, SessionListener sessionListener) {
        SessionManager sessionManager;
        try {
            TowServer towServer = context.getBeanRegistry().getBean(serverId);
            sessionManager = towServer.getSessionManager(deploymentName);
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve session handler with " + getEventInfo().getTarget(), e);
        }
        if (sessionManager != null) {
            getSessionListenerRegistration(context).register(sessionListener, deploymentName);
        }
    }

    @NonNull
    private SessionListenerRegistration getSessionListenerRegistration(@NonNull ActivityContext context) {
        SessionListenerRegistration sessionListenerRegistration;
        if (context.getBeanRegistry().containsBean(SessionListenerRegistration.class)) {
            sessionListenerRegistration = context.getBeanRegistry().getBean(SessionListenerRegistration.class);
        } else {
            if (context.getBeanRegistry().containsBean(TowServer.class)) {
                sessionListenerRegistration = new SessionListenerRegistrationBean();
            } else {
                throw new IllegalStateException("Bean for SessionListenerRegistration must be defined");
            }
        }
        return sessionListenerRegistration;
    }

    /**
     * Called by {@link SessionEventCountingListener} when a session is created.
     */
    void sessionCreated() {
        getEventCount().count();
    }

}

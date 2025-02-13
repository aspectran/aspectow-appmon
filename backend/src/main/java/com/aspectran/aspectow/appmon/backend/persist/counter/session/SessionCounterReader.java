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
package com.aspectran.aspectow.appmon.backend.persist.counter.session;

import com.aspectran.aspectow.appmon.backend.config.EventInfo;
import com.aspectran.aspectow.appmon.backend.persist.counter.AbstractCounterReader;
import com.aspectran.core.component.bean.NoSuchBeanException;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.core.component.session.SessionListenerRegistration;
import com.aspectran.core.component.session.SessionManager;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.core.service.ServiceHoldingListener;
import com.aspectran.undertow.server.TowServer;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;

/**
 * <p>Created: 2025-02-12</p>
 */
public class SessionCounterReader extends AbstractCounterReader {

    private final String serverId;

    private final String deploymentName;

    public SessionCounterReader(@NonNull EventInfo eventInfo) {
        super(eventInfo);

        String[] arr = StringUtils.divide(eventInfo.getTarget(), "/");
        this.serverId = arr[0];
        this.deploymentName = arr[1];
    }

    @Override
    public void initialize() throws Exception {
        final SessionListener sessionListener = new SessionCounterListener(this);
        ActivityContext context = CoreServiceHolder.findActivityContext(deploymentName);
        if (context != null) {
            registerSessionListener(context, sessionListener);
        } else {
            CoreServiceHolder.addServiceHolingListener(new ServiceHoldingListener() {
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
        try {
            return context.getBeanRegistry().getBean(SessionListenerRegistration.class);
        } catch (NoSuchBeanException e) {
            throw new IllegalStateException("Bean for SessionListenerRegistration must be defined", e);
        }
    }

    void sessionCreated() {
        getCounterData().count();
    }

}

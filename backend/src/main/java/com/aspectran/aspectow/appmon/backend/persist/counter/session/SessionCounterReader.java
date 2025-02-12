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
import com.aspectran.aspectow.appmon.backend.persist.PersistManager;
import com.aspectran.aspectow.appmon.backend.persist.counter.AbstractCounterReader;
import com.aspectran.aspectow.appmon.backend.persist.counter.CounterData;
import com.aspectran.core.component.UnavailableException;
import com.aspectran.core.component.bean.NoSuchBeanException;
import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListenerRegistration;
import com.aspectran.core.component.session.SessionManager;
import com.aspectran.undertow.server.TowServer;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.logging.Logger;
import com.aspectran.utils.logging.LoggerFactory;

/**
 * <p>Created: 2025-02-12</p>
 */
public class SessionCounterReader extends AbstractCounterReader {

    private static final Logger logger = LoggerFactory.getLogger(SessionCounterReader.class);

    private final PersistManager persistManager;

    private final String serverId;

    private final String deploymentName;

    private SessionCounterListener sessionListener;

    public SessionCounterReader(PersistManager persistManager, @NonNull EventInfo eventInfo) {
        super(eventInfo);
        this.persistManager = persistManager;

        String[] arr = StringUtils.divide(eventInfo.getTarget(), "/");
        this.serverId = arr[0];
        this.deploymentName = arr[1];
    }

    @Override
    public void start() {
        SessionManager sessionManager;
        try {
            TowServer towServer = persistManager.getAppMonManager().getBean(serverId);
            sessionManager = towServer.getSessionManager(deploymentName);
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve session handler with " + getEventInfo().getTarget(), e);
        }
        if (sessionManager != null) {
            sessionListener = new SessionCounterListener(this);
            getSessionListenerRegistration().register(sessionListener, deploymentName);
        }
    }

    @Override
    public void stop() {
        if (sessionListener != null) {
            try {
                getSessionListenerRegistration().remove(sessionListener, deploymentName);
                sessionListener = null;
            } catch (UnavailableException e) {
                // ignored
            }
        }
    }

    @NonNull
    private SessionListenerRegistration getSessionListenerRegistration() {
        try {
            return persistManager.getAppMonManager().getBean(SessionListenerRegistration.class);
        } catch (NoSuchBeanException e) {
            throw new IllegalStateException("Bean for SessionListenerRegistration must be defined", e);
        }
    }

    void sessionCreated(@NonNull Session session) {
    }

}

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
package com.aspectran.appmon.agent.listener;

import com.aspectran.core.activity.Activity;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.core.component.session.SessionListenerRegistration;
import com.aspectran.undertow.server.TowServer;
import com.aspectran.undertow.support.SessionListenerRegistrationBean;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.web.support.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;

import static com.aspectran.appmon.exporter.event.session.SessionEventReader.USER_IP_ADDRESS;

/**
 * <p>Created: 2024-12-13</p>
 */
@Component
public class UserTrackingListener extends InstantActivitySupport implements SessionListener, InitializableBean {

    @Override
    public void sessionCreated(@NonNull Session session) {
        Activity activity = getCurrentActivity();
        String ipAddress = getRemoteAddr(activity.getTranslet());
        if (!StringUtils.isEmpty(ipAddress)) {
            session.setAttribute(USER_IP_ADDRESS, ipAddress);
        }
    }

    @Override
    public void initialize() throws Exception {
        SessionListenerRegistration sessionListenerRegistration;
        if (getBeanRegistry().containsBean(SessionListenerRegistration.class)) {
            sessionListenerRegistration = getBeanRegistry().getBean(SessionListenerRegistration.class);
        } else {
            try {
                Class.forName("com.aspectran.undertow.server.TowServer");
            } catch (ClassNotFoundException e) {
                // Undertow not available
                return;
            }
            if (getBeanRegistry().containsBean(TowServer.class)) {
                sessionListenerRegistration = new SessionListenerRegistrationBean();
            } else {
                throw new IllegalStateException("Bean for SessionListenerRegistration must be defined");
            }
        }
        sessionListenerRegistration.register(this, getActivityContext().getName());
    }

    public static String getRemoteAddr(@NonNull Translet translet) {
        String remoteAddr = translet.getRequestAdapter().getHeader(HttpHeaders.X_FORWARDED_FOR);
        if (StringUtils.hasLength(remoteAddr)) {
            if (remoteAddr.contains(",")) {
                remoteAddr = StringUtils.tokenize(remoteAddr, ",", true)[0];
            }
        } else {
            remoteAddr = ((HttpServletRequest)translet.getRequestAdaptee()).getRemoteAddr();
        }
        return remoteAddr;
    }

}

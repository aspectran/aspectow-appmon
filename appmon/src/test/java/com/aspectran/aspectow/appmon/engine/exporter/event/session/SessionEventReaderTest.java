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

import com.aspectran.aspectow.appmon.engine.config.EventInfo;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterType;
import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.aspectow.appmon.engine.persist.counter.EventCount;
import com.aspectran.aspectow.node.config.ClusterConfig;
import com.aspectran.aspectow.node.config.GroupInfoHolder;
import com.aspectran.aspectow.node.config.NodeInfoHolder;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.session.DefaultSessionManager;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.core.component.session.SessionListenerRegistration;
import com.aspectran.core.component.session.SessionManager;
import com.aspectran.core.component.session.SessionManagerProvider;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.netty.support.SessionListenerRegistrationBean;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionEventReaderTest {

    @Test
    void testNettyRootContextResolution() throws Exception {
        SessionManager rootSessionManager = new DefaultSessionManager("root");
        SessionManager consoleSessionManager = new DefaultSessionManager("console");

        SessionManagerProvider nettyServer = new SessionManagerProvider() {
            @Override
            public SessionManager getSessionManager() {
                return rootSessionManager;
            }

            @Override
            public SessionManager getSessionManager(String name) {
                if ("root".equals(name) || "/".equals(name) || name == null || name.isEmpty()) {
                    return rootSessionManager;
                }
                if ("console".equals(name) || "/console".equals(name)) {
                    return consoleSessionManager;
                }
                return null;
            }

            @Override
            public SessionManager getSessionManagerByPath(String path) {
                if ("/".equals(path) || path == null || path.isEmpty()) {
                    return rootSessionManager;
                }
                if ("/console".equals(path) || "console".equals(path)) {
                    return consoleSessionManager;
                }
                return null;
            }
        };

        AtomicReference<String> registeredContext = new AtomicReference<>();
        AtomicReference<SessionListener> registeredListener = new AtomicReference<>();

        SessionListenerRegistration nettyRegistration = new SessionListenerRegistrationBean() {
            @Override
            public void register(SessionListener listener, String contextName) {
                registeredContext.set(contextName);
                registeredListener.set(listener);
            }

            @Override
            public void remove(SessionListener listener, String contextName) {
                registeredContext.set(null);
                registeredListener.set(null);
            }
        };

        ExporterManager exporterManager = createExporterManager(nettyRegistration, nettyServer, "netty.server");

        EventInfo eventInfo = new EventInfo();
        eventInfo.putValue("id", "session");
        eventInfo.putValue("target", "netty.server/root");

        EventCount eventCount = new EventCount();
        SessionEventReader reader = new SessionEventReader(exporterManager, eventInfo, eventCount);

        reader.init();
        assertEquals("root", registeredContext.get(), "Netty registration should preserve 'root'");

        reader.start();
        assertEquals("root", registeredContext.get());
        assertNotNull(registeredListener.get());
        assertTrue(registeredListener.get() instanceof SessionEventReadingListener);

        reader.stop();
        assertEquals(null, registeredListener.get());
    }

    @Test
    void testUndertowRootContextResolution() throws Exception {
        SessionManager rootSessionManager = new DefaultSessionManager("root");

        SessionManagerProvider undertowServer = new SessionManagerProvider() {
            @Override
            public SessionManager getSessionManager() {
                return rootSessionManager;
            }

            @Override
            public SessionManager getSessionManager(String name) {
                if ("root".equals(name)) {
                    return rootSessionManager;
                }
                return null;
            }

            @Override
            public SessionManager getSessionManagerByPath(String path) {
                if ("/".equals(path)) {
                    return rootSessionManager;
                }
                return null;
            }
        };

        AtomicReference<String> registeredContext = new AtomicReference<>();
        AtomicReference<SessionListener> registeredListener = new AtomicReference<>();

        // Subclass of Undertow SessionListenerRegistrationBean
        SessionListenerRegistration undertowRegistration = new com.aspectran.undertow.support.SessionListenerRegistrationBean() {
            @Override
            public void register(SessionListener listener, String deploymentName) {
                registeredContext.set(deploymentName);
                registeredListener.set(listener);
            }

            @Override
            public void remove(SessionListener listener, String deploymentName) {
                registeredContext.set(null);
                registeredListener.set(null);
            }
        };

        ExporterManager exporterManager = createExporterManager(undertowRegistration, undertowServer, "tow.server");

        EventInfo eventInfo = new EventInfo();
        eventInfo.putValue("id", "session");
        eventInfo.putValue("target", "tow.server/root");

        EventCount eventCount = new EventCount();
        SessionEventReader reader = new SessionEventReader(exporterManager, eventInfo, eventCount);

        reader.init();
        assertEquals("root", registeredContext.get(), "Undertow registration should preserve 'root'");

        reader.start();
        assertEquals("root", registeredContext.get());
        assertNotNull(registeredListener.get());

        reader.stop();
        assertNull(registeredListener.get());
    }

    @Test
    void testNettySubContextResolution() throws Exception {
        SessionManager consoleSessionManager = new DefaultSessionManager("console");

        SessionManagerProvider nettyServer = new SessionManagerProvider() {
            @Override
            public SessionManager getSessionManager() {
                return null;
            }

            @Override
            public SessionManager getSessionManager(String name) {
                if ("console".equals(name)) {
                    return consoleSessionManager;
                }
                return null;
            }

            @Override
            public SessionManager getSessionManagerByPath(String path) {
                return null;
            }
        };

        AtomicReference<String> registeredContext = new AtomicReference<>();

        SessionListenerRegistration nettyRegistration = new SessionListenerRegistrationBean() {
            @Override
            public void register(SessionListener listener, String contextName) {
                registeredContext.set(contextName);
            }

            @Override
            public void remove(SessionListener listener, String contextName) {
                registeredContext.set(null);
            }
        };

        ExporterManager exporterManager = createExporterManager(nettyRegistration, nettyServer, "netty.server");

        EventInfo eventInfo = new EventInfo();
        eventInfo.putValue("id", "session");
        eventInfo.putValue("target", "netty.server/console");

        EventCount eventCount = new EventCount();
        SessionEventReader reader = new SessionEventReader(exporterManager, eventInfo, eventCount);

        reader.init();
        assertEquals("console", registeredContext.get(), "Subcontext 'console' should be preserved as 'console'");

        reader.start();
        assertEquals("console", registeredContext.get());

        reader.stop();
    }

    private ExporterManager createExporterManager(
            SessionListenerRegistration registration,
            SessionManagerProvider server,
            String serverId) {
        ActivityContext mockActivityContext = (ActivityContext) Proxy.newProxyInstance(
                ActivityContext.class.getClassLoader(),
                new Class<?>[] { ActivityContext.class },
                (proxy, method, args) -> null
        );
        NodeManager nodeManager = new NodeManager(
                "node1", "group1", new ClusterConfig(), new NodeInfoHolder(), new GroupInfoHolder());
        AppMonManager appMonManager = new AppMonManager(nodeManager, null, null, 0) {
            @Override
            @NonNull
            public ActivityContext getActivityContext() {
                return mockActivityContext;
            }
        };
        return new ExporterManager(ExporterType.EVENT, appMonManager, "testApp") {
            @Override
            public boolean containsBean(@NonNull Class<?> type) {
                return type == SessionListenerRegistration.class;
            }

            @Override
            public boolean containsBean(@NonNull String id) {
                return serverId.equals(id);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <V> V getBean(@NonNull Class<V> type) {
                if (type == SessionListenerRegistration.class) {
                    return (V) registration;
                }
                return null;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <V> V getBean(@NonNull String id) {
                if (serverId.equals(id)) {
                    return (V) server;
                }
                return null;
            }
        };
    }

}

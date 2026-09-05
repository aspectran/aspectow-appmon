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
import com.aspectran.core.component.session.SessionManager;
import com.aspectran.core.component.session.SessionManagerProvider;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.lifecycle.AbstractLifeCycle;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionEventReaderTest {

    private static class TestSessionManager extends DefaultSessionManager {
        private final List<SessionListener> listeners = new CopyOnWriteArrayList<>();
        private final AtomicReference<SessionListener> lastAddedListener = new AtomicReference<>();
        private final AtomicReference<SessionListener> lastRemovedListener = new AtomicReference<>();

        public TestSessionManager(String name) {
            super(name);
        }

        @Override
        public void addSessionListener(SessionListener listener) {
            listeners.add(listener);
            lastAddedListener.set(listener);
            super.addSessionListener(listener);
        }

        @Override
        public void removeSessionListener(SessionListener listener) {
            listeners.remove(listener);
            lastRemovedListener.set(listener);
            super.removeSessionListener(listener);
        }

        public boolean containsListener(Class<? extends SessionListener> listenerType) {
            for (SessionListener l : listeners) {
                if (listenerType.isInstance(l)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Test
    void testNettyRootContextResolution() throws Exception {
        TestSessionManager rootSessionManager = new TestSessionManager("root");
        TestSessionManager consoleSessionManager = new TestSessionManager("console");

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

        ExporterManager exporterManager = createExporterManager(nettyServer, "netty.server");

        EventInfo eventInfo = new EventInfo();
        eventInfo.putValue("id", "session");
        eventInfo.putValue("target", "netty.server/root");

        EventCount eventCount = new EventCount();
        SessionEventReader reader = new SessionEventReader(exporterManager, eventInfo, eventCount);

        reader.init();
        assertTrue(rootSessionManager.containsListener(UserTrackingListener.class),
                "UserTrackingListener should be registered on root session manager");

        reader.start();
        assertTrue(rootSessionManager.containsListener(SessionEventReadingListener.class),
                "SessionEventReadingListener should be registered on root session manager");
        assertTrue(rootSessionManager.lastAddedListener.get() instanceof SessionEventReadingListener);

        reader.stop();
        assertFalse(rootSessionManager.containsListener(SessionEventReadingListener.class),
                "SessionEventReadingListener should be removed on stop");
        assertTrue(rootSessionManager.containsListener(UserTrackingListener.class),
                "UserTrackingListener should NOT be removed on stop");

        reader.destroy();
        assertFalse(rootSessionManager.containsListener(UserTrackingListener.class),
                "UserTrackingListener should be removed on destroy");
    }

    @Test
    void testUndertowRootContextResolution() throws Exception {
        TestSessionManager rootSessionManager = new TestSessionManager("root");

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

        ExporterManager exporterManager = createExporterManager(undertowServer, "tow.server");

        EventInfo eventInfo = new EventInfo();
        eventInfo.putValue("id", "session");
        eventInfo.putValue("target", "tow.server/root");

        EventCount eventCount = new EventCount();
        SessionEventReader reader = new SessionEventReader(exporterManager, eventInfo, eventCount);

        reader.init();
        assertTrue(rootSessionManager.containsListener(UserTrackingListener.class),
                "UserTrackingListener should be registered on Undertow root session manager");

        reader.start();
        assertTrue(rootSessionManager.containsListener(SessionEventReadingListener.class));

        reader.stop();
        assertFalse(rootSessionManager.containsListener(SessionEventReadingListener.class));
        assertTrue(rootSessionManager.containsListener(UserTrackingListener.class));

        reader.destroy();
        assertFalse(rootSessionManager.containsListener(UserTrackingListener.class));
    }

    @Test
    void testNettySubContextResolution() throws Exception {
        TestSessionManager consoleSessionManager = new TestSessionManager("console");

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

        ExporterManager exporterManager = createExporterManager(nettyServer, "netty.server");

        EventInfo eventInfo = new EventInfo();
        eventInfo.putValue("id", "session");
        eventInfo.putValue("target", "netty.server/console");

        EventCount eventCount = new EventCount();
        SessionEventReader reader = new SessionEventReader(exporterManager, eventInfo, eventCount);

        reader.init();
        assertTrue(consoleSessionManager.containsListener(UserTrackingListener.class),
                "Subcontext 'console' session manager should have UserTrackingListener registered");

        reader.start();
        assertTrue(consoleSessionManager.containsListener(SessionEventReadingListener.class));

        reader.stop();
        assertFalse(consoleSessionManager.containsListener(SessionEventReadingListener.class));

        reader.destroy();
        assertFalse(consoleSessionManager.containsListener(UserTrackingListener.class));
    }

    @Test
    void testDuplicateTargetRegistrationSkipped() throws Exception {
        AtomicInteger addCount = new AtomicInteger();
        TestSessionManager rootSessionManager = new TestSessionManager("root-dup-test") {
            @Override
            public void addSessionListener(SessionListener listener) {
                if (listener instanceof UserTrackingListener) {
                    addCount.incrementAndGet();
                }
                super.addSessionListener(listener);
            }
        };

        SessionManagerProvider nettyServer = new SessionManagerProvider() {
            @Override
            public SessionManager getSessionManager() {
                return rootSessionManager;
            }

            @Override
            public SessionManager getSessionManager(String name) {
                return "root-dup-test".equals(name) ? rootSessionManager : null;
            }

            @Override
            public SessionManager getSessionManagerByPath(String path) {
                return "/".equals(path) ? rootSessionManager : null;
            }
        };

        ExporterManager exporterManager = createExporterManager(nettyServer, "netty.server");

        EventInfo eventInfo1 = new EventInfo();
        eventInfo1.putValue("id", "session-1");
        eventInfo1.putValue("target", "netty.server/root-dup-test");

        SessionEventReader reader1 = new SessionEventReader(exporterManager, eventInfo1, new EventCount());
        reader1.init();
        assertEquals(1, addCount.get());

        // Same target in the same context
        EventInfo eventInfo2 = new EventInfo();
        eventInfo2.putValue("id", "session-2");
        eventInfo2.putValue("target", "netty.server/root-dup-test");

        SessionEventReader reader2 = new SessionEventReader(exporterManager, eventInfo2, new EventCount());
        reader2.init();
        // Should NOT register again (skipped)
        assertEquals(1, addCount.get());

        // Cleanup
        reader1.destroy();
        reader2.destroy();
    }

    @Test
    void testDestroyRemovesTrackingListener() throws Exception {
        TestSessionManager rootSessionManager = new TestSessionManager("root-destroy-test");

        SessionManagerProvider nettyServer = new SessionManagerProvider() {
            @Override
            public SessionManager getSessionManager() {
                return rootSessionManager;
            }

            @Override
            public SessionManager getSessionManager(String name) {
                return "root-destroy-test".equals(name) ? rootSessionManager : null;
            }

            @Override
            public SessionManager getSessionManagerByPath(String path) {
                return "/".equals(path) ? rootSessionManager : null;
            }
        };

        ExporterManager exporterManager = createExporterManager(nettyServer, "netty.server");

        EventInfo eventInfo = new EventInfo();
        eventInfo.putValue("id", "session");
        eventInfo.putValue("target", "netty.server/root-destroy-test");

        SessionEventReader reader = new SessionEventReader(exporterManager, eventInfo, new EventCount());
        reader.init();
        reader.start();
        reader.stop();

        // stop() should NOT remove UserTrackingListener
        assertTrue(rootSessionManager.containsListener(UserTrackingListener.class));

        // destroy() should remove UserTrackingListener
        reader.destroy();
        assertFalse(rootSessionManager.containsListener(UserTrackingListener.class));
    }

    private static class TestLifeCycleServer extends AbstractLifeCycle implements SessionManagerProvider {
        private final SessionManager sessionManager;

        public TestLifeCycleServer(SessionManager sessionManager) {
            this.sessionManager = sessionManager;
        }

        @Override
        protected void doStart() throws Exception {
        }

        @Override
        protected void doStop() throws Exception {
        }

        @Override
        public SessionManager getSessionManager() {
            return isStarted() ? sessionManager : null;
        }

        @Override
        public SessionManager getSessionManager(String name) {
            return isStarted() ? sessionManager : null;
        }

        @Override
        public SessionManager getSessionManagerByPath(String path) {
            return isStarted() ? sessionManager : null;
        }
    }

    @Test
    void testDelayedServerStart_autoStartFalse() throws Exception {
        TestSessionManager rootSessionManager = new TestSessionManager("delayed-root");
        TestLifeCycleServer delayedServer = new TestLifeCycleServer(rootSessionManager);

        ExporterManager exporterManager = createExporterManager(delayedServer, "tow.server");

        EventInfo eventInfo = new EventInfo();
        eventInfo.putValue("id", "session");
        eventInfo.putValue("target", "tow.server/delayed-root");

        SessionEventReader reader = new SessionEventReader(exporterManager, eventInfo, new EventCount());
        // init when server is not started (autoStart=false)
        reader.init();
        assertFalse(rootSessionManager.containsListener(UserTrackingListener.class),
                "UserTrackingListener should NOT be registered yet because server is not started");

        // Dashboard viewer connects before server starts
        reader.start();
        assertFalse(rootSessionManager.containsListener(SessionEventReadingListener.class),
                "SessionEventReadingListener should NOT be registered yet because server is not started");

        // Server starts later
        delayedServer.start();
        assertTrue(rootSessionManager.containsListener(UserTrackingListener.class),
                "UserTrackingListener should be registered automatically upon server start");
        assertTrue(rootSessionManager.containsListener(SessionEventReadingListener.class),
                "SessionEventReadingListener should be registered automatically upon server start");

        reader.stop();
        assertFalse(rootSessionManager.containsListener(SessionEventReadingListener.class),
                "SessionEventReadingListener should be removed on stop");
        assertTrue(rootSessionManager.containsListener(UserTrackingListener.class),
                "UserTrackingListener should remain on stop");

        reader.destroy();
        assertFalse(rootSessionManager.containsListener(UserTrackingListener.class),
                "UserTrackingListener should be removed on destroy");
    }

    @Test
    void testDelayedServerStart_viewerConnectsAfterServerStart() throws Exception {
        TestSessionManager rootSessionManager = new TestSessionManager("delayed-root-2");
        TestLifeCycleServer delayedServer = new TestLifeCycleServer(rootSessionManager);

        ExporterManager exporterManager = createExporterManager(delayedServer, "tow.server");

        EventInfo eventInfo = new EventInfo();
        eventInfo.putValue("id", "session");
        eventInfo.putValue("target", "tow.server/delayed-root-2");

        SessionEventReader reader = new SessionEventReader(exporterManager, eventInfo, new EventCount());
        reader.init();
        assertFalse(rootSessionManager.containsListener(UserTrackingListener.class));

        // Server starts before dashboard viewer connects
        delayedServer.start();
        assertTrue(rootSessionManager.containsListener(UserTrackingListener.class),
                "UserTrackingListener should be registered on server start");
        assertFalse(rootSessionManager.containsListener(SessionEventReadingListener.class),
                "SessionEventReadingListener should not be registered before reader.start()");

        // Viewer connects now
        reader.start();
        assertTrue(rootSessionManager.containsListener(SessionEventReadingListener.class),
                "SessionEventReadingListener should be registered after reader.start()");

        reader.stop();
        assertFalse(rootSessionManager.containsListener(SessionEventReadingListener.class));
        assertTrue(rootSessionManager.containsListener(UserTrackingListener.class));

        reader.destroy();
        assertFalse(rootSessionManager.containsListener(UserTrackingListener.class));
    }

    private ExporterManager createExporterManager(
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
                return false;
            }

            @Override
            public boolean containsBean(@NonNull String id) {
                return serverId.equals(id);
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

/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.appmon.engine.service.websocket;

import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.aspectow.appmon.engine.service.ExportService;
import com.aspectran.aspectow.appmon.engine.service.ServiceSession;
import com.aspectran.aspectow.node.config.NodeConfig;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.websocket.jsr356.AspectranConfigurator;
import com.aspectran.web.websocket.jsr356.SimplifiedEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AppMonLogRelayHandler bridges the Redis log messages to the browser's WebSocket.
 * It follows the standard AppMon session management pattern.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component
@ServerEndpoint(
        value = "/relay/appmon/{nodeId}/{token}",
        configurator = AspectranConfigurator.class
)
public class AppMonLogRelayHandler extends SimplifiedEndpoint implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(AppMonLogRelayHandler.class);

    private final AppMonManager appMonManager;

    private final Map<String, Set<String>> sessionJoinedInstances = new ConcurrentHashMap<>();

    @Autowired
    public AppMonLogRelayHandler(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    @Initialize
    public void registerExportService() {
        appMonManager.getExportServiceManager().addExportService(this);
    }

    @Destroy
    public void unregisterExportService() {
        appMonManager.getExportServiceManager().removeExportService(this);
    }

    @Override
    protected void registerMessageHandlers(@NonNull Session session) {
        if (addSession(session)) {
            String nodeId = session.getPathParameters().get("nodeId");
            if (StringUtils.hasText(nodeId)) {
                Set<String> instances = new HashSet<>();
                instances.add(nodeId);
                sessionJoinedInstances.put(session.getId(), instances);
                logger.info("Log relay session joined for instance: {}", nodeId);
            }
        }
    }

    @Override
    protected void onSessionRemoved(@NonNull Session session) {
        sessionJoinedInstances.remove(session.getId());
        logger.info("Log relay session closed: {}", session.getId());
    }

    @Override
    public void broadcast(String message) {
        // AppMon protocol message: {instanceName}:{type}:{name}:{content}
        int colonIdx = message.indexOf(':');
        if (colonIdx > 0) {
            String instanceName = message.substring(0, colonIdx);
            super.containsSession(session -> {
                Set<String> joined = sessionJoinedInstances.get(session.getId());
                if (joined != null && joined.contains(instanceName)) {
                    sendText(session, message);
                }
                return false;
            });
        }
    }

    @Override
    public void broadcast(ServiceSession serviceSession, String message) {
        if (serviceSession instanceof WebsocketServiceSession session) {
            sendText(session.getSession(), message);
        }
    }

    @Override
    public boolean isUsingInstance(String instanceName) {
        for (Set<String> instances : sessionJoinedInstances.values()) {
            if (instances.contains(instanceName)) {
                return true;
            }
        }
        return false;
    }

}

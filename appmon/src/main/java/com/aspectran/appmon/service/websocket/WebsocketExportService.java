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
package com.aspectran.appmon.service.websocket;

import com.aspectran.appmon.manager.AppMonManager;
import com.aspectran.appmon.service.ExportService;
import com.aspectran.appmon.service.ServiceSession;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.websocket.jsr356.AspectranConfigurator;
import com.aspectran.web.websocket.jsr356.SimplifiedEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
@ServerEndpoint(
        value = "/backend/{token}/websocket",
        configurator = AspectranConfigurator.class
)
public class WebsocketExportService extends SimplifiedEndpoint implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketExportService.class);

    private static final String MESSAGE_PING = "ping:";

    private static final String MESSAGE_PONG = "pong:";

    private static final String MESSAGE_JOIN = "join:";

    private static final String MESSAGE_LEAVE = "leave";

    private static final String MESSAGE_JOINED = "joined:";

    private static final String MESSAGE_ESTABLISHED = "established:";

    private final AppMonManager appMonManager;

    @Autowired
    public WebsocketExportService(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    @Initialize
    public void registerExportService() {
        appMonManager.getExportServiceManager().addExportService(this);
    }

    @Destroy
    public void destroy() throws Exception {
        appMonManager.getExportServiceManager().removeExportService(this);
    }

    @Override
    protected boolean checkAuthorized(@NonNull Session session) {
        String token = session.getPathParameters().get("token");
        try {
            AppMonManager.validateToken(token);
        } catch (InvalidPBTokenException e) {
            logger.error("Invalid token: {}", token);
            return false;
        }
        return true;
    }

    @Override
    protected void registerMessageHandlers(@NonNull Session session) {
        if (session.getMessageHandlers().isEmpty()) {
            session.addMessageHandler(String.class, message
                    -> handleMessage(session, message));
        }
    }

    private void handleMessage(Session session, String message) {
        if (MESSAGE_PING.equals(message)) {
            pong(session);
        } else if (message != null && message.startsWith(MESSAGE_JOIN)) {
            join(session, message.substring(MESSAGE_JOIN.length()));
        } else if (MESSAGE_ESTABLISHED.equals(message)) {
            joinComplete(session);
        } else if (MESSAGE_LEAVE.equals(message)) {
            removeSession(session);
        }
    }

    @Override
    protected void onSessionRemoved(Session session) {
        ServiceSession serviceSession = new WebsocketServiceSession(session);
        appMonManager.getExportServiceManager().release(serviceSession);
    }

    private void pong(Session session) {
        String newToken = AppMonManager.issueToken(1800); // 30 min.
        sendText(session, MESSAGE_PONG + newToken);
    }

    private void join(Session session, String joinInstances) {
        ServiceSession serviceSession = new WebsocketServiceSession(session);
        String[] instanceNames = StringUtils.splitWithComma(joinInstances);
        instanceNames = appMonManager.getVerifiedInstanceNames(instanceNames);
        if (!StringUtils.hasText(joinInstances) || instanceNames.length > 0) {
            serviceSession.setJoinedInstances(instanceNames);
        }
        if (addSession(session)) {
            broadcast(serviceSession, MESSAGE_JOINED);
        }
    }

    private void joinComplete(@NonNull Session session) {
        ServiceSession serviceSession = new WebsocketServiceSession(session);
        appMonManager.getExportServiceManager().join(serviceSession);
        List<String> messages = appMonManager.getExportServiceManager().getLastMessages(serviceSession);
        for (String message : messages) {
            sendText(session, message);
        }
    }

    @Override
    public void broadcast(String message) {
        super.broadcast(message);
    }

    @Override
    public void broadcast(@NonNull ServiceSession serviceSession, String message) {
        if (serviceSession instanceof WebsocketServiceSession session) {
            sendText(session.getSession(), message);
        }
    }

    @Override
    public boolean isUsingInstance(String instanceName) {
        if (StringUtils.hasLength(instanceName)) {
            return existsSession(session -> {
                ServiceSession serviceSession = new WebsocketServiceSession(session);
                String[] instanceNames = serviceSession.getJoinedInstances();
                if (instanceNames != null) {
                    for (String name : instanceNames) {
                        if (instanceName.equals(name)) {
                            return true;
                        }
                    }
                }
                return false;
            });
        }
        return false;
    }

}

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
package com.aspectran.aspectow.appmon.backend.service.websocket;

import com.aspectran.aspectow.appmon.backend.service.BackendSession;
import com.aspectran.aspectow.appmon.backend.service.ExportService;
import com.aspectran.aspectow.appmon.manager.AppMonManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.AvoidAdvice;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.utils.ExceptionUtils;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.logging.Logger;
import com.aspectran.utils.logging.LoggerFactory;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.websocket.jsr356.AspectranConfigurator;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Component
@ServerEndpoint(
        value = "/server/endpoint/{token}",
        configurator = AspectranConfigurator.class
)
@AvoidAdvice
public class WebsocketExportService implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketExportService.class);

    private static final String MESSAGE_PING = "ping:";

    private static final String MESSAGE_PONG = "pong:";

    private static final String MESSAGE_JOIN = "join:";

    private static final String MESSAGE_LEAVE = "leave";

    private static final String MESSAGE_JOINED = "joined:";

    private static final String MESSAGE_ESTABLISHED = "established:";

    private static final Set<WebsocketBackendSession> sessions = new HashSet<>();

    private final AppMonManager appMonManager;

    @Autowired
    public WebsocketExportService(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    @Initialize
    public void registerExportService() {
        appMonManager.addExportService(this);
    }

    @OnOpen
    public void onOpen(@PathParam("token") String token, Session session) throws IOException {
        try {
            AppMonManager.validateToken(token);
        } catch (InvalidPBTokenException e) {
            logger.error("Invalid token: " + token);
            String reason = "Invalid token";
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
            throw new IOException(reason, e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("WebSocket connection established with token: " + token);
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        if (MESSAGE_PING.equals(message)) {
            broadcast(session, MESSAGE_PONG + AppMonManager.issueToken(80));
        } else if (message != null && message.startsWith(MESSAGE_JOIN)) {
            addSession(session, message.substring(MESSAGE_JOIN.length()));
        } else if (MESSAGE_ESTABLISHED.equals(message)) {
            establishComplete(session);
        } else if (MESSAGE_LEAVE.equals(message)) {
            removeSession(session);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        if (logger.isDebugEnabled()) {
            logger.debug("Websocket session " + session.getId() + " has been closed. Reason: " + reason);
        }
        removeSession(session);
    }

    @OnError
    public void onError(@NonNull Session session, Throwable error) {
        if (!ExceptionUtils.hasCause(error, ClosedChannelException.class, TimeoutException.class)) {
            logger.warn("Error in websocket session: " + session.getId(), error);
        }
        try {
            removeSession(session);
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, null));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void addSession(Session session, String joinInstances) {
        WebsocketBackendSession appMonSession = new WebsocketBackendSession(session);
        synchronized (sessions) {
            if (sessions.add(appMonSession)) {
                String[] instanceNames = appMonManager.getVerifiedInstanceNames(StringUtils.splitCommaDelimitedString(joinInstances));
                if (!StringUtils.hasText(joinInstances) || instanceNames.length > 0) {
                    appMonSession.setJoinedInstances(instanceNames);
                }
                sendJoined(appMonSession);
            }
        }
    }

    private void sendJoined(@NonNull BackendSession backendSession) {
        String[] instanceNames = backendSession.getJoinedInstances();
        if (instanceNames != null) {
            broadcast(backendSession, MESSAGE_JOINED);
        }
    }

    private void establishComplete(@NonNull Session session) {
        BackendSession backendSession = new WebsocketBackendSession(session);
        appMonManager.join(backendSession);
        List<String> messages = appMonManager.getLastMessages(backendSession);
        for (String message : messages) {
            broadcast(backendSession, message);
        }
    }

    private void removeSession(Session session) {
        WebsocketBackendSession appMonSession = new WebsocketBackendSession(session);
        synchronized (sessions) {
            if (sessions.remove(appMonSession)) {
                appMonManager.release(appMonSession);
            }
        }
    }

    @Override
    public void broadcast(String message) {
        synchronized (sessions) {
            for (WebsocketBackendSession websocketEndpointSession : sessions) {
                broadcast(websocketEndpointSession.getSession(), message);
            }
        }
    }

    @Override
    public void broadcast(@NonNull BackendSession session, String message) {
        if (session instanceof WebsocketBackendSession websocketEndpointSession) {
            broadcast(websocketEndpointSession.getSession(), message);
        }
    }

    private void broadcast(@NonNull Session session, String message) {
        if (session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        }
    }

    @Override
    public boolean isUsingInstance(String instanceName) {
        if (StringUtils.hasLength(instanceName)) {
            synchronized (sessions) {
                for (WebsocketBackendSession appMonSession : sessions) {
                    String[] instanceNames = appMonSession.getJoinedInstances();
                    if (instanceNames != null) {
                        for (String name : instanceNames) {
                            if (instanceName.equals(name)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

}

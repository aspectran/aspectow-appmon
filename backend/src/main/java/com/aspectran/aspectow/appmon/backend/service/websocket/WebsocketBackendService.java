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

import com.aspectran.aspectow.appmon.backend.config.GroupInfo;
import com.aspectran.aspectow.appmon.backend.service.BackendService;
import com.aspectran.aspectow.appmon.backend.service.BackendSession;
import com.aspectran.aspectow.appmon.manager.AppMonManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.AvoidAdvice;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.json.JsonBuilder;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ServerEndpoint(
        value = "/server/endpoint/{token}",
        configurator = AspectranConfigurator.class
)
@AvoidAdvice
public class WebsocketBackendService implements BackendService {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketBackendService.class);

    private static final String HEARTBEAT_PING_MSG = "--ping--";

    private static final String HEARTBEAT_PONG_MSG = "--pong--";

    private static final String MESSAGE_JOIN = "join:";

    private static final String MESSAGE_LEAVE = "leave";

    private static final String MESSAGE_JOINED = "joined:";

    private static final String MESSAGE_ESTABLISHED = "established:";

    private static final Set<WebsocketBackendSession> sessions = new HashSet<>();

    private final AppMonManager appMonManager;

    @Autowired
    public WebsocketBackendService(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    @Initialize
    public void registerBackendService() {
        appMonManager.addBackendService(this);
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
        if (HEARTBEAT_PING_MSG.equals(message)) {
            session.getAsyncRemote().sendText(HEARTBEAT_PONG_MSG);
            return;
        }
        if (message != null && message.startsWith(MESSAGE_JOIN)) {
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
        logger.error("Error in websocket session: " + session.getId(), error);
        try {
            removeSession(session);
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, null));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void addSession(Session session, String joinGroups) {
        WebsocketBackendSession appMonSession = new WebsocketBackendSession(session);
        synchronized (sessions) {
            if (sessions.add(appMonSession)) {
                String[] joinGroupNames = appMonManager.getVerifiedGroupNames(StringUtils.splitCommaDelimitedString(joinGroups));
                if (!StringUtils.hasText(joinGroups) || joinGroupNames.length > 0) {
                    appMonSession.saveJoinedGroups(joinGroupNames);
                }
                sendJoined(appMonSession);
            }
        }
    }

    private void sendJoined(@NonNull BackendSession backendSession) {
        String[] joinGroupNames = backendSession.getJoinedGroups();
        if (joinGroupNames != null) {
            List<GroupInfo> groups = appMonManager.getGroupInfoList(backendSession.getJoinedGroups());
            String json = new JsonBuilder()
                .nullWritable(false)
                .object()
                    .put("groups", groups)
                .endObject()
                .toString();
            broadcast(backendSession, MESSAGE_JOINED + json);
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
    public boolean isUsingGroup(String groupName) {
        if (StringUtils.hasLength(groupName)) {
            synchronized (sessions) {
                for (WebsocketBackendSession appMonSession : sessions) {
                    String[] joinedGroups = appMonSession.getJoinedGroups();
                    if (joinedGroups != null) {
                        for (String name : joinedGroups) {
                            if (groupName.equals(name)) {
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

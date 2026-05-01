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
package com.aspectran.aspectow.appmon.engine.relay.websocket;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.aspectow.appmon.engine.relay.CommandOptions;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayer;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager;
import com.aspectran.aspectow.appmon.engine.relay.RelaySession;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.websocket.jsr356.AspectranConfigurator;
import com.aspectran.web.websocket.jsr356.SimplifiedEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_LOAD_PREVIOUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_REFRESH;

/**
 * An {@link MessageRelayer} implementation based on the WebSocket protocol (JSR-356).
 * It provides real-time, bidirectional communication with clients.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
@Component
@ServerEndpoint(
        value = "/nodes/{nodeId}/appmon/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class WebsocketMessageRelayer extends SimplifiedEndpoint implements MessageRelayer {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketMessageRelayer.class);

    private static final String COMMAND_PING = "ping";
    private static final String COMMAND_JOIN = "join";
    private static final String COMMAND_ESTABLISHED = "established";

    private static final String MESSAGE_PONG = "pong:";
    private static final String MESSAGE_JOINED = "joined:";

    private final AppMonManager appMonManager;

    @Autowired
    public WebsocketMessageRelayer(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    /**
     * Initializes the service by registering it with the {@link MessageRelayManager}.
     */
    @Initialize
    public void registerRelayer() {
        appMonManager.getMessageRelayManager().addRelayer(this);
    }

    /**
     * Destroys the service, unregistering from the manager.
     */
    @Destroy
    public void destroy() throws Exception {
        appMonManager.getMessageRelayManager().removeRelayer(this);
    }

    @Override
    protected boolean checkAuthorized(@NonNull Session session) {
        String token = session.getPathParameters().get("token");
        try {
            AppMonTokenIssuer.validateToken(token);
        } catch (InvalidPBTokenException e) {
            logger.error("Invalid token: {}", token);
            return false;
        }
        return true;
    }

    @Override
    protected void registerMessageHandlers(@NonNull Session session) {
        if (session.getMessageHandlers().isEmpty()) {
            session.addMessageHandler(String.class, message -> {
                setLoggingGroup();
                handleMessage(session, message);
            });
        }
    }

    private void handleMessage(Session session, String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        CommandOptions commandOptions = new CommandOptions(message);
        switch (commandOptions.getCommand()) {
            case COMMAND_PING:
                pong(session);
                break;
            case COMMAND_JOIN:
                join(session, commandOptions);
                break;
            case COMMAND_ESTABLISHED:
                joinComplete(session);
                break;
            case COMMAND_REFRESH:
            case COMMAND_LOAD_PREVIOUS:
                refreshData(session, commandOptions);
        }
    }

    @Override
    protected void onSessionRemoved(Session session) {
        RelaySession relaySession = new WebsocketRelaySession(session);
        appMonManager.getMessageRelayManager().release(relaySession);
    }

    private void pong(Session session) {
        String newToken = AppMonTokenIssuer.issueToken(1800); // 30 min.
        sendText(session, MESSAGE_PONG + newToken);
    }

    private void join(Session session, @NonNull CommandOptions commandOptions) {
        WebsocketRelaySession relaySession = new WebsocketRelaySession(session);
        String timeZone = commandOptions.getTimeZone();
        if (StringUtils.hasText(timeZone)) {
            relaySession.setTimeZone(timeZone);
        }
        String appsToJoin = commandOptions.getAppsToJoin();
        String[] appIds = StringUtils.splitWithComma(appsToJoin);
        appIds = appMonManager.getVerifiedAppIds(appIds);
        if (!StringUtils.hasText(appsToJoin) || appIds.length > 0) {
            relaySession.setJoinedApps(appIds);
        }
        if (addSession(session)) {
            relay(relaySession, MESSAGE_JOINED);
        }
    }

    private void joinComplete(@NonNull Session session) {
        RelaySession relaySession = new WebsocketRelaySession(session);
        appMonManager.getMessageRelayManager().join(relaySession);
        List<String> messages = appMonManager.getMessageRelayManager().getLastMessages(relaySession);
        for (String message : messages) {
            sendText(session, message);
        }
    }

    private void refreshData(@NonNull Session session, @NonNull CommandOptions commandOptions) {
        RelaySession relaySession = new WebsocketRelaySession(session);
        if (!commandOptions.hasTimeZone()) {
            commandOptions.setTimeZone(relaySession.getTimeZone());
        }
        List<String> messages = appMonManager.getMessageRelayManager().getNewMessages(relaySession, commandOptions);
        for (String message : messages) {
            sendText(session, message);
        }
    }

    @Override
    public void relay(String message) {
        broadcast(message);
    }

    @Override
    public void relay(@NonNull RelaySession serviceSession, String message) {
        if (serviceSession instanceof WebsocketRelaySession session) {
            sendText(session.getSession(), message);
        }
    }

    @Override
    public boolean isUsingApp(String appId) {
        if (StringUtils.hasLength(appId)) {
            return containsSession(session -> {
                RelaySession relaySession = new WebsocketRelaySession(session);
                String[] appIds = relaySession.getJoinedApps();
                if (appIds != null) {
                    for (String id : appIds) {
                        if (appId.equals(id)) {
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

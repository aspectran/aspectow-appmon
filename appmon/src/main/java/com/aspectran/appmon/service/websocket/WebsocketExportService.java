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
import com.aspectran.appmon.service.CommandOptions;
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

    private static final String COMMAND_PING = "ping";
    private static final String COMMAND_JOIN = "join";
    private static final String COMMAND_ESTABLISHED = "established";
    private static final String COMMAND_REFRESH = "refresh";

    private static final String MESSAGE_PONG = "pong:";
    private static final String MESSAGE_JOINED = "joined:";

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
                refreshData(session, commandOptions);
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

    private void join(Session session, @NonNull CommandOptions commandOptions) {
        WebsocketServiceSession serviceSession = new WebsocketServiceSession(session);
        String timeZone = commandOptions.getTimeZone();
        if (StringUtils.hasText(timeZone)) {
            serviceSession.setTimeZone(timeZone);
        }
        String instancesToJoin = commandOptions.getInstancesToJoin();
        String[] instanceNames = StringUtils.splitWithComma(instancesToJoin);
        instanceNames = appMonManager.getVerifiedInstanceNames(instanceNames);
        if (!StringUtils.hasText(instancesToJoin) || instanceNames.length > 0) {
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

    private void refreshData(@NonNull Session session, @NonNull CommandOptions commandOptions) {
        ServiceSession serviceSession = new WebsocketServiceSession(session);
        if (!commandOptions.hasTimeZone()) {
            commandOptions.setTimeZone(serviceSession.getTimeZone());
        }
        List<String> messages = appMonManager.getExportServiceManager().getNewMessages(serviceSession, commandOptions);
        for (String message : messages) {
            sendText(session, message);
        }
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
            return containsSession(session -> {
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

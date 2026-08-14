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
package com.aspectran.aspectow.console.commands.bridge.polling;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.console.auth.UserInfo;
import com.aspectran.aspectow.node.management.commands.CommandRequestParameters;
import com.aspectran.aspectow.node.management.commands.RemoteCommandManager;
import com.aspectran.aspectow.node.management.commands.bridge.CommandBridge;
import com.aspectran.aspectow.node.management.commands.bridge.CommandBroker;
import com.aspectran.aspectow.node.management.commands.bridge.CommandSession;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static com.aspectran.aspectow.node.management.commands.bridge.CommandBroker.CATEGORY_COMMANDS;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * PollingCommandBridge manages client sessions for HTTP long-polling
 * and uses a central message buffer to distribute command results.
 */
@Component(NODES_BASE_PATH + "/${nodeId}/" + CATEGORY_COMMANDS)
public class PollingCommandBridge extends AbstractComponent implements CommandBridge {

    private final PollingSessionManager pollingSessionManager;

    private final RemoteCommandManager remoteCommandManager;

    /**
     * Constructs a new {@code PollingCommandBridge} with the specified remote
     * command manager.
     * @param remoteCommandManager the manager for executing remote commands
     */
    @Autowired
    public PollingCommandBridge(RemoteCommandManager remoteCommandManager) {
        this.remoteCommandManager = remoteCommandManager;
        this.pollingSessionManager = new PollingSessionManager(this);
    }

    /**
     * Initializes the polling command bridge by initializing the polling session manager.
     * @throws Exception if initialization fails
     */
    @Override
    protected void doInitialize() throws Exception {
        pollingSessionManager.initialize();
    }

    /**
     * Destroys the polling command bridge by destroying the polling session manager.
     * @throws Exception if destruction fails
     */
    @Override
    protected void doDestroy() throws Exception {
        pollingSessionManager.destroy();
    }

    /**
     * Returns the remote command manager.
     * @return the remote command manager
     */
    public RemoteCommandManager getRemoteCommandManager() {
        return remoteCommandManager;
    }

    /**
     * Returns the command broker.
     * @return the command broker
     */
    public CommandBroker getBroker() {
        return remoteCommandManager.getBroker();
    }

    /**
     * Allows a client to subscribe and start a polling session.
     * @param translet the current translet
     * @return the RestResponse containing session and node details
     */
    @Request("/polling/subscribe")
    public RestResponse subscribe(@NonNull Translet translet) {
        String token = translet.getParameter("token");
        try {
            AppMonTokenIssuer.validateToken(token);
        } catch (Exception e) {
            return new FailureResponse().forbidden();
        }

        String targetNodeId = translet.getParameter("targetNodeId");
        if (!StringUtils.hasText(targetNodeId)) {
            return new FailureResponse("Target node ID is required");
        }

        PollingCommandSession session = pollingSessionManager.getSession(translet);
        if (session == null) {
            session = pollingSessionManager.createSession(translet);
            remoteCommandManager.registerSession(session.getId(), this);
        }

        return new SuccessResponse(Map.of(
                "pollingInterval", session.getPollingInterval(),
                "nodeId", remoteCommandManager.getNodeId()
                )).ok();
    }

    /**
     * Allows a client to pull new messages from the server.
     * @param translet the current translet
     * @return the RestResponse containing the array of new messages
     */
    @Request("/polling/pull")
    public RestResponse pull(@NonNull Translet translet) {
        PollingCommandSession session = pollingSessionManager.getSession(translet);
        if (session == null) {
            return new FailureResponse().setError("session_not_found", "Session not found");
        }

        String[] messages = pollingSessionManager.pull(session);
        return new SuccessResponse(messages).ok();
    }

    /**
     * Creates a command to be sent to a specific node or all nodes.
     * @param translet the current translet
     * @param request the command request parameters
     * @return a success message or failure response
     */
    @Request("/polling/push")
    public RestResponse push(@NonNull Translet translet, @NonNull CommandRequestParameters request) {
        PollingCommandSession session = pollingSessionManager.getSession(translet);
        if (session == null) {
            return new FailureResponse().setError("session_not_found", "Session not found");
        }

        if (request.getCommand() == null) {
            return new FailureResponse().setError("command_required", "Command is required");
        }

        UserInfo userInfo = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
        boolean isDemo = (userInfo != null && userInfo.hasRole("DEMO"));
        if (isDemo) {
            CommandParameters commandParameters = request.getCommand();
            String commandName = commandParameters.getString("command");
            String transletName = commandParameters.getString("translet");

            if (!"sysinfo".equals(commandName) && !"translet".equals(commandName)) {
                return new FailureResponse().setError("forbidden", "Only 'sysinfo' and 'translet' commands are allowed in the demo environment.");
            }

            if ("translet".equals(commandName) && !"sample/commands/hello".equals(transletName)) {
                return new FailureResponse().setError("forbidden", "Only 'sample/commands/hello' translet can be executed in the demo environment.");
            }
        }

        try {
            request.setHeader("execute");
            request.setSessionId(session.getId());
            remoteCommandManager.process(request);
            return new SuccessResponse("Command initiated successfully").ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    /**
     * Finds the command session with the given session ID.
     * @param sessionId the session ID to find
     * @return the command session, or {@code null} if not found
     */
    @Override
    public CommandSession findCommandSession(String sessionId) {
        return pollingSessionManager.getSession(sessionId);
    }

    /**
     * Bridges a broadcast message to the central polling buffer.
     * @param message the message to broadcast
     */
    @Override
    public void bridge(String message) {
        pollingSessionManager.push(message);
    }

    /**
     * Bridges a message to a specific polling command session.
     * @param commandSession the target command session
     * @param message the message to send
     */
    @Override
    public void bridge(@NonNull CommandSession commandSession, String message) {
        pollingSessionManager.push(commandSession, message);
    }

}

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
package com.aspectran.aspectow.console.scheduler.bridge.polling;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.node.management.scheduler.RemoteSchedulerManager;
import com.aspectran.aspectow.node.management.scheduler.SchedulerRequestParameters;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBridge;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBroker;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerSession;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBroker.CATEGORY_SCHEDULER;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * PollingSchedulerBridge manages client sessions for HTTP long-polling
 * and uses a central message buffer to distribute scheduler management results.
 */
@Component(NODES_BASE_PATH + "/${nodeId}/" + CATEGORY_SCHEDULER)
public class PollingSchedulerBridge extends AbstractComponent implements SchedulerBridge {

    private final PollingSessionManager sessionManager;

    private final RemoteSchedulerManager remoteSchedulerManager;

    /**
     * Constructs a new {@code PollingSchedulerBridge} with the specified scheduler manager.
     * @param remoteSchedulerManager the scheduler manager to handle scheduler operations
     */
    @Autowired
    public PollingSchedulerBridge(RemoteSchedulerManager remoteSchedulerManager) {
        this.remoteSchedulerManager = remoteSchedulerManager;
        this.sessionManager = new PollingSessionManager(this);
    }

    /**
     * Initializes the bridge components, starting the session manager.
     * @throws Exception if initialization fails
     */
    @Override
    protected void doInitialize() throws Exception {
        sessionManager.initialize();
    }

    /**
     * Destroys the bridge components, stopping the session manager.
     * @throws Exception if destruction fails
     */
    @Override
    protected void doDestroy() throws Exception {
        sessionManager.destroy();
    }

    /**
     * Gets the associated scheduler manager.
     * @return the scheduler manager
     */
    public RemoteSchedulerManager getRemoteSchedulerManager() {
        return remoteSchedulerManager;
    }

    /**
     * Gets the scheduler broker from the scheduler manager.
     * @return the scheduler broker
     */
    public SchedulerBroker getBroker() {
        return remoteSchedulerManager.getBroker();
    }

    /**
     * Allows a client to subscribe and start a polling session.
     * @param translet the current translet
     * @return a {@link RestResponse} containing the subscription details or error message
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

        PollingSchedulerSession schedulerSession = sessionManager.getSession(translet);
        if (schedulerSession == null) {
            schedulerSession = sessionManager.createSession(translet);
            remoteSchedulerManager.registerSession(schedulerSession.getId(), this);
        }
        schedulerSession.setNodeId(targetNodeId);

        remoteSchedulerManager.getBroker().subscribe(schedulerSession);

        return new SuccessResponse(Map.of(
                "pollingInterval", schedulerSession.getPollingInterval(),
                "nodeId", remoteSchedulerManager.getNodeId()
                )).ok();
    }

    /**
     * Allows a client to pull new messages from the server.
     * @param translet the current translet
     * @return a {@link RestResponse} containing the list of new messages or error message
     */
    @Request("/polling/pull")
    public RestResponse pull(@NonNull Translet translet) {
        PollingSchedulerSession session = sessionManager.getSession(translet);
        if (session == null) {
            return new FailureResponse().setError("session_not_found", "Session not found");
        }

        String[] messages = sessionManager.pull(session);
        return new SuccessResponse(messages).ok();
    }

    /**
     * Creates a scheduler command to be sent to a specific node or all nodes.
     * @param translet the current translet
     * @param request the scheduler request parameters containing the command
     * @return a {@link RestResponse} representing success or failure of the push operation
     */
    @Request("/polling/push")
    public RestResponse push(@NonNull Translet translet, @NonNull SchedulerRequestParameters request) {
        PollingSchedulerSession session = sessionManager.getSession(translet);
        if (session == null) {
            return new FailureResponse().setError("session_not_found", "Session not found");
        }

        if (request.getCommand() == null) {
            return new FailureResponse().setError("command_required", "Command is required");
        }

        if (!StringUtils.hasText(request.getTargetNodeId())) {
            return new FailureResponse().setError("target_node_required", "Target node is required");
        }

        try {
            request.setNodeId(remoteSchedulerManager.getNodeId());
            request.setSessionId(session.getId());
            remoteSchedulerManager.process(request);
            return new SuccessResponse("Scheduler command initiated successfully").ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    /**
     * Simple ping-pong endpoint for node status verification.
     * @return a {@link RestResponse} containing "pong"
     */
    @Request("/ping")
    public RestResponse ping() {
        return new SuccessResponse("pong").ok();
    }

    /**
     * Finds and returns the scheduler session associated with the given session ID.
     * @param sessionId the session identifier
     * @return the {@link SchedulerSession} if found, or {@code null} if not found
     */
    @Override
    public SchedulerSession findSchedulerSession(String sessionId) {
        return sessionManager.getSession(sessionId);
    }

    /**
     * Broadcasts a message to all connected polling sessions.
     * @param message the message to broadcast
     */
    @Override
    public void bridge(String message) {
        sessionManager.push(message);
    }

    /**
     * Sends a message to a specific polling scheduler session.
     * @param schedulerSession the target scheduler session
     * @param message the message to send
     */
    @Override
    public void bridge(@NonNull SchedulerSession schedulerSession, String message) {
        sessionManager.push(schedulerSession, message);
    }

}

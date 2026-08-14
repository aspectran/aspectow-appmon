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
package com.aspectran.aspectow.console.cluster.bridge.polling;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.node.management.nodes.RemoteNodeManager;
import com.aspectran.aspectow.node.management.nodes.bridge.NodeBridge;
import com.aspectran.aspectow.node.management.nodes.bridge.NodeBroker;
import com.aspectran.aspectow.node.management.nodes.bridge.NodeSession;
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

import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * PollingNodeBridge manages client sessions for HTTP long-polling
 * to distribute cluster node status and join/left events.
 */
@Component(NODES_BASE_PATH + "/${nodeId}")
public class PollingNodeBridge extends AbstractComponent implements NodeBridge {

    private final PollingSessionManager pollingSessionManager;

    private final RemoteNodeManager remoteNodeManager;

    /**
     * Constructs a new {@code PollingNodeBridge} with the specified remote node manager.
     * @param remoteNodeManager the remote node manager
     */
    @Autowired
    public PollingNodeBridge(RemoteNodeManager remoteNodeManager) {
        this.remoteNodeManager = remoteNodeManager;
        this.pollingSessionManager = new PollingSessionManager(this);
    }

    @Override
    protected void doInitialize() throws Exception {
        pollingSessionManager.initialize();
    }

    @Override
    protected void doDestroy() throws Exception {
        pollingSessionManager.destroy();
    }

    /**
     * Returns the remote node manager.
     * @return the remote node manager
     */
    public RemoteNodeManager getRemoteNodeManager() {
        return remoteNodeManager;
    }

    /**
     * Returns the node broker.
     * @return the node broker
     */
    public NodeBroker getBroker() {
        return remoteNodeManager.getBroker();
    }

    /**
     * Allows a client to subscribe and start a polling session.
     * @param translet the current translet
     * @return the RestResponse containing session details
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

        PollingNodeSession session = pollingSessionManager.getSession(translet);
        if (session == null) {
            session = pollingSessionManager.createSession(translet);
            remoteNodeManager.registerSession(session.getId(), this);
        }

        return new SuccessResponse(Map.of(
                "pollingInterval", session.getPollingInterval(),
                "nodeId", remoteNodeManager.getNodeId()
                )).ok();
    }

    /**
     * Allows a client to pull new messages from the server.
     * @param translet the current translet
     * @return the RestResponse containing the array of new messages
     */
    @Request("/polling/pull")
    public RestResponse pull(@NonNull Translet translet) {
        PollingNodeSession session = pollingSessionManager.getSession(translet);
        if (session == null) {
            return new FailureResponse().setError("session_not_found", "Session not found");
        }

        String[] messages = pollingSessionManager.pull(session);
        return new SuccessResponse(messages).ok();
    }

    /**
     * Simple ping-pong endpoint for node status verification.
     * @return a {@link RestResponse} containing "pong"
     */
    @Request("/ping")
    public RestResponse ping() {
        return new SuccessResponse("pong").ok();
    }

    @Override
    public NodeSession findNodeSession(String sessionId) {
        return pollingSessionManager.getSession(sessionId);
    }

    @Override
    public void bridge(String message) {
        pollingSessionManager.push(message);
    }

    @Override
    public void bridge(@NonNull NodeSession nodeSession, String message) {
        pollingSessionManager.push(nodeSession, message);
    }

}

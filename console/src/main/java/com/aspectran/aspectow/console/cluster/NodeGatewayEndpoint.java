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
package com.aspectran.aspectow.console.cluster;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeMessageProtocol;
import com.aspectran.aspectow.node.redis.RedisMessageListener;
import com.aspectran.aspectow.node.redis.RedisMessageSubscriber;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.json.JsonBuilder;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.websocket.jsr356.AspectranConfigurator;
import com.aspectran.web.websocket.jsr356.SimplifiedEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NodeGatewayEndpoint provides real-time, bidirectional communication for cluster node management.
 *
 * <p>Created: 2026-04-19</p>
 */
@Component
@ServerEndpoint(
        value = "/nodes/{nodeId}/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class NodeGatewayEndpoint extends SimplifiedEndpoint implements RedisMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeGatewayEndpoint.class);

    private static final String COMMAND_PING = "ping";
    private static final String COMMAND_JOIN = "join";
    private static final String COMMAND_ESTABLISHED = "established";

    private static final String MESSAGE_PONG = "pong:";
    private static final String MESSAGE_JOINED = "joined:";

    private final NodeManager nodeManager;

    @Autowired
    public NodeGatewayEndpoint(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Initialize
    public void registerListener() {
        RedisMessageSubscriber subscriber = nodeManager.getRedisMessageSubscriber();
        if (subscriber != null) {
            subscriber.addListener(this);
            logger.info("NodeGatewayEndpoint registered as RedisMessageListener");
        }
    }

    @Destroy
    public void unregisterListener() {
        RedisMessageSubscriber subscriber = nodeManager.getRedisMessageSubscriber();
        if (subscriber != null) {
            subscriber.removeListener(this);
        }
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
            session.addMessageHandler(String.class, message -> handleMessage(session, message));
        }
    }

    @Override
    protected void onSessionRemoved(@NonNull Session session) {
        String nodeId = session.getPathParameters().get("nodeId");
        logger.info("Node management session removed: {} (nodeId: {})", session.getId(), nodeId);
    }

    @Override
    public String getCategory() {
        return NodeMessageProtocol.CATEGORY_CLUSTER;
    }

    @Override
    public void onControlMessage(String nodeId, String message) {
        String json = new JsonBuilder()
                .object()
                .put("type", "control")
                .put("nodeId", nodeId)
                .put("message", message)
                .endObject()
                .toString();
        broadcast(json);
    }

    @Override
    public void onRelayMessage(String nodeId, String message) {
        String json = new JsonBuilder()
                .object()
                .put("type", "relay")
                .put("nodeId", nodeId)
                .put("message", message)
                .endObject()
                .toString();
        broadcast(json);
    }

    private void handleMessage(Session session, String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        if (message.startsWith("command:")) {
            String command = message.substring(8);
            int idx = command.indexOf(';');
            if (idx != -1) {
                command = command.substring(0, idx);
            }

            switch (command) {
                case COMMAND_PING:
                    pong(session);
                    break;
                case COMMAND_JOIN:
                    join(session);
                    break;
                case COMMAND_ESTABLISHED:
                    joinComplete(session);
                    break;
            }
        }
    }

    private void pong(Session session) {
        String newToken = AppMonTokenIssuer.issueToken(1800); // 30 min.
        sendText(session, MESSAGE_PONG + newToken);
    }

    private void join(Session session) {
        if (addSession(session)) {
            sendText(session, MESSAGE_JOINED);
        }
    }

    private void joinComplete(@NonNull Session session) {
        String nodeId = session.getPathParameters().get("nodeId");
        logger.info("Node management session established: {} (nodeId: {})", session.getId(), nodeId);
    }

}

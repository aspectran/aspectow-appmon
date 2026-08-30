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
package com.aspectran.aspectow.console.build.bridge.websocket;

import com.aspectran.aspectow.console.build.bridge.BuildDeploySession;
import jakarta.websocket.Session;

/**
 * WebSocket implementation of BuildDeploySession.
 *
 * <p>Created: 2026-08-18</p>
 */
public class WebsocketBuildDeploySession implements BuildDeploySession {

    private final Session session;

    private String nodeId;

    /**
     * Constructs a new WebsocketBuildDeploySession wrapping the given WebSocket session.
     * @param session the WebSocket session
     */
    public WebsocketBuildDeploySession(Session session) {
        this.session = session;
    }

    /**
     * Returns the underlying WebSocket session.
     * @return the WebSocket session
     */
    public Session getSession() {
        return session;
    }

    @Override
    public String getSessionId() {
        return session.getId();
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

}

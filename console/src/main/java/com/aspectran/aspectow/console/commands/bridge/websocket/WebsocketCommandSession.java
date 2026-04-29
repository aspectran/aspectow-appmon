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
package com.aspectran.aspectow.console.commands.bridge.websocket;

import com.aspectran.aspectow.console.commands.bridge.CommandSession;
import com.aspectran.web.websocket.jsr356.WrappedSession;
import jakarta.websocket.Session;

/**
 * A {@link CommandSession} implementation that wraps a JSR-356 {@link Session}.
 * It stores session-specific data in the WebSocket session's user properties.
 */
public class WebsocketCommandSession extends WrappedSession implements CommandSession {

    private static final String NODE_ID_PROPERTY = "console:nodeId";

    /**
     * Instantiates a new WebsocketCommandSession.
     * @param session the underlying WebSocket session
     */
    public WebsocketCommandSession(Session session) {
        super(session);
    }

    @Override
    public String getNodeId() {
        return (String)getSession().getUserProperties().get(NODE_ID_PROPERTY);
    }

    @Override
    public void setNodeId(String nodeId) {
        getSession().getUserProperties().put(NODE_ID_PROPERTY, nodeId);
    }

    @Override
    public boolean isValid() {
        return getSession().isOpen();
    }

}

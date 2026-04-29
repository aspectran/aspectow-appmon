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
package com.aspectran.aspectow.console.commands.bridge.polling;

import com.aspectran.aspectow.console.commands.bridge.CommandBridge;
import com.aspectran.aspectow.console.commands.bridge.CommandSession;
import com.aspectran.aspectow.console.commands.manager.RemoteCommandManager;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.session.SessionIdGenerator;
import com.aspectran.utils.CopyOnWriteMap;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PollingCommandBridge manages client sessions for HTTP long-polling
 * and uses a central message buffer to distribute command results.
 */
@Component
public class PollingCommandBridge extends AbstractComponent implements CommandBridge {

    private static final Logger logger = LoggerFactory.getLogger(PollingCommandBridge.class);

    private final SessionIdGenerator sessionIdGenerator = new SessionIdGenerator();

    private final Map<String, PollingCommandSession> sessions = new CopyOnWriteMap<>();

    private final RemoteCommandManager remoteCommandManager;

    private final BufferedMessages bufferedMessages;

    @Autowired
    public PollingCommandBridge(RemoteCommandManager remoteCommandManager) {
        this.remoteCommandManager = remoteCommandManager;
        this.bufferedMessages = new BufferedMessages(100);
    }

    @Override
    protected void doInitialize() throws Exception {
        if (remoteCommandManager.getBroker() != null) {
            remoteCommandManager.getBroker().addBridge(this);
            logger.info("PollingCommandBridge registered with CommandBroker");
        } else {
            logger.warn("Failed to register PollingCommandBridge: CommandBroker is null");
        }
    }

    @Override
    protected void doDestroy() throws Exception {
        if (remoteCommandManager.getBroker() != null) {
            remoteCommandManager.getBroker().removeBridge(this);
        }
        bufferedMessages.clear();
        sessions.clear();
    }

    public PollingCommandSession createSession(String nodeId) {
        String sessionId = sessionIdGenerator.createSessionId();
        PollingCommandSession newSession = new PollingCommandSession(this);
        newSession.setNodeId(nodeId);
        newSession.setSessionTimeout(60); // 1 minute default
        newSession.access(true);
        sessions.put(sessionId, newSession);
        return newSession;
    }

    public PollingCommandSession getSession(String sessionId) {
        PollingCommandSession session = sessions.get(sessionId);
        if (session != null) {
            session.access(false);
        }
        return session;
    }

    @Override
    public void bridge(String data) {
        if (!sessions.isEmpty()) {
            bufferedMessages.push(data);
        }
    }

    @Override
    public void bridge(@NonNull CommandSession session, String data) {
        // For individual bridging, we might need a separate mechanism
        // but typically commands are broadcasted or targeted via NodeId
        bridge(data);
    }

    public String[] pull(PollingCommandSession session) {
        String[] messages = bufferedMessages.pop(session);
        if (messages != null && messages.length > 0) {
            shrinkBuffer();
        }
        return messages;
    }

    private void shrinkBuffer() {
        int minLineIndex = getMinLineIndex();
        if (minLineIndex > -1) {
            bufferedMessages.shrink(minLineIndex);
        }
    }

    private int getMinLineIndex() {
        int minLineIndex = -1;
        for (PollingCommandSession session : sessions.values()) {
            if (minLineIndex == -1) {
                minLineIndex = session.getLastLineIndex();
            } else if (session.getLastLineIndex() < minLineIndex) {
                minLineIndex = session.getLastLineIndex();
            }
        }
        return minLineIndex;
    }

    /**
     * Scavenges for and removes expired sessions.
     */
    public void scavenge() {
        List<String> expiredSessions = new ArrayList<>();
        for (Map.Entry<String, PollingCommandSession> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredSessions.add(entry.getKey());
            }
        }
        for (String id : expiredSessions) {
            PollingCommandSession session = sessions.remove(id);
            if (session != null) {
                session.destroy();
            }
        }
        if (sessions.isEmpty()) {
            bufferedMessages.clear();
        } else {
            shrinkBuffer();
        }
    }

    public BufferedMessages getBufferedMessages() {
        return bufferedMessages;
    }

}

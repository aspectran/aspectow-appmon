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
import com.aspectran.aspectow.console.commands.bridge.CommandBroker;
import com.aspectran.aspectow.console.commands.bridge.CommandSession;
import com.aspectran.aspectow.console.commands.manager.RemoteCommandManager;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PollingCommandBridge manages client sessions for HTTP long-polling
 * and uses a central message buffer to distribute command results.
 */
@Component
public class PollingCommandBridge extends AbstractComponent implements CommandBridge {

    private static final Logger logger = LoggerFactory.getLogger(PollingCommandBridge.class);

    private final PollingSessionManager sessionManager;

    private final RemoteCommandManager remoteCommandManager;

    private final BufferedMessages bufferedMessages;

    @Autowired
    public PollingCommandBridge(RemoteCommandManager remoteCommandManager) {
        this.remoteCommandManager = remoteCommandManager;
        this.sessionManager = new PollingSessionManager(this);
        this.bufferedMessages = new BufferedMessages(100);
    }

    @Override
    protected void doInitialize() throws Exception {
        sessionManager.initialize();
        if (remoteCommandManager.getBroker() != null) {
            remoteCommandManager.getBroker().addBridge(this);
            logger.info("PollingCommandBridge registered with CommandBroker");
        } else {
            logger.warn("Failed to register PollingCommandBridge: CommandBroker is null");
        }
    }

    @Override
    protected void doDestroy() throws Exception {
        sessionManager.destroy();
        if (remoteCommandManager.getBroker() != null) {
            remoteCommandManager.getBroker().removeBridge(this);
        }
        bufferedMessages.clear();
    }

    public PollingCommandSession createSession(String nodeId) {
        return sessionManager.createSession(nodeId);
    }

    public PollingCommandSession getSession(String sessionId) {
        return sessionManager.getSession(sessionId);
    }

    @Override
    public void bridge(String data) {
        if (!sessionManager.getSessions().isEmpty()) {
            bufferedMessages.push(data);
        }
    }

    @Override
    public void bridge(@NonNull CommandSession session, String data) {
        bridge(data);
    }

    public CommandBroker getBroker() {
        return remoteCommandManager.getBroker();
    }

    public String[] pull(PollingCommandSession session) {
        String[] messages = bufferedMessages.pop(session);
        if (messages != null && messages.length > 0) {
            shrinkBuffer();
        }
        return messages;
    }

    public void shrinkBuffer() {
        int minLineIndex = getMinLineIndex();
        if (minLineIndex > -1) {
            bufferedMessages.shrink(minLineIndex);
        }
    }

    private int getMinLineIndex() {
        int minLineIndex = -1;
        for (PollingCommandSession session : sessionManager.getSessions().values()) {
            if (minLineIndex == -1) {
                minLineIndex = session.getLastLineIndex();
            } else if (session.getLastLineIndex() < minLineIndex) {
                minLineIndex = session.getLastLineIndex();
            }
        }
        return minLineIndex;
    }

    public BufferedMessages getBufferedMessages() {
        return bufferedMessages;
    }

}

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
package com.aspectran.aspectow.console.scheduler.bridge.polling;

import com.aspectran.aspectow.console.scheduler.bridge.SchedulerBridge;
import com.aspectran.aspectow.console.scheduler.bridge.SchedulerBroker;
import com.aspectran.aspectow.console.scheduler.bridge.SchedulerSession;
import com.aspectran.aspectow.console.scheduler.manager.SchedulerManager;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * PollingSchedulerBridge manages client sessions for HTTP long-polling
 * and uses a central message buffer to distribute scheduler management results.
 */
@Component
public class PollingSchedulerBridge extends AbstractComponent implements SchedulerBridge {

    private static final Logger logger = LoggerFactory.getLogger(PollingSchedulerBridge.class);

    private final PollingSessionManager sessionManager;

    private final SchedulerManager schedulerManager;

    private final BufferedMessages bufferedMessages;

    @Autowired
    public PollingSchedulerBridge(SchedulerManager schedulerManager) {
        this.schedulerManager = schedulerManager;
        this.sessionManager = new PollingSessionManager(this);
        this.bufferedMessages = new BufferedMessages(100);
    }

    @Override
    protected void doInitialize() throws Exception {
        sessionManager.initialize();
        if (schedulerManager.getBroker() != null) {
            schedulerManager.getBroker().addBridge(this);
            logger.info("PollingSchedulerBridge registered with SchedulerBroker");
        } else {
            logger.warn("Failed to register PollingSchedulerBridge: SchedulerBroker is null");
        }
    }

    @Override
    protected void doDestroy() throws Exception {
        sessionManager.destroy();
        if (schedulerManager.getBroker() != null) {
            schedulerManager.getBroker().removeBridge(this);
        }
        bufferedMessages.clear();
    }

    public PollingSchedulerSession createSession(String nodeId) {
        return sessionManager.createSession(nodeId);
    }

    public PollingSchedulerSession getSession(String sessionId) {
        return sessionManager.getSession(sessionId);
    }

    public Collection<PollingSchedulerSession> getSessions() {
        return sessionManager.getSessions().values();
    }

    @Override
    public void bridge(String data) {
        if (!sessionManager.getSessions().isEmpty()) {
            bufferedMessages.push(data);
        }
    }

    @Override
    public void bridge(@NonNull SchedulerSession session, String data) {
        bridge(data);
    }

    public SchedulerBroker getBroker() {
        return schedulerManager.getBroker();
    }

    public String[] pull(PollingSchedulerSession session) {
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
        for (PollingSchedulerSession session : sessionManager.getSessions().values()) {
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

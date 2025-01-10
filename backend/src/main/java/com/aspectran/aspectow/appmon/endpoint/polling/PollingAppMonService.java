/*
 * Copyright (c) 2018-2025 The Aspectran Project
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
package com.aspectran.aspectow.appmon.endpoint.polling;

import com.aspectran.aspectow.appmon.config.EndpointPollingConfig;
import com.aspectran.aspectow.appmon.manager.AppMonManager;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.Nullable;
import com.aspectran.utils.thread.ScheduledExecutorScheduler;
import com.aspectran.utils.thread.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PollingAppMonService extends AbstractComponent {

    private final Map<String, PollingAppMonSession> sessions = new ConcurrentHashMap<>();

    private final Scheduler scheduler = new ScheduledExecutorScheduler("PollingAppMonSessionScheduler", false);

    private final AppMonManager appMonManager;

    private final PollingAppMonBuffer buffer;

    public PollingAppMonService(AppMonManager appMonManager, int initialBufferSize) {
        this.appMonManager = appMonManager;
        this.buffer = new PollingAppMonBuffer(initialBufferSize);
    }

    public PollingAppMonSession createSession(String id, @Nullable EndpointPollingConfig pollingConfig, String[] joinGroups) {
        PollingAppMonSession existingSession = sessions.get(id);
        if (existingSession != null) {
            existingSession.access(false);
            return existingSession;
        } else {
            int pollingInterval = 0;
            int sessionTimeout = 0;
            if (pollingConfig != null) {
                pollingInterval = pollingConfig.getPollingInterval();
                sessionTimeout = pollingConfig.getSessionTimeout();
            }
            if (pollingInterval > 0 && sessionTimeout <= 0) {
                sessionTimeout = pollingInterval * 2;
            }
            PollingAppMonSession session = new PollingAppMonSession(this, sessionTimeout, pollingInterval);
            session.saveJoinedGroups(appMonManager.getVerifiedGroupNames(joinGroups));
            sessions.put(id, session);
            session.access(true);
            return session;
        }
    }

    public PollingAppMonSession getSession(String id) {
        PollingAppMonSession session = sessions.get(id);
        if (session != null) {
            session.access(false);
            return session;
        } else {
            return null;
        }
    }

    public void push(String line) {
        if (!sessions.isEmpty()) {
            buffer.push(line);
        }
    }

    public String[] pull(PollingAppMonSession session) {
        String[] lines = buffer.pop(session);
        if (lines != null && lines.length > 0) {
            shrinkBuffer();
        }
        return lines;
    }

    private void shrinkBuffer() {
        int minLineIndex = getMinLineIndex();
        if (minLineIndex > -1) {
            buffer.shrink(minLineIndex);
        }
    }

    private int getMinLineIndex() {
        int minLineIndex = -1;
        for (PollingAppMonSession session : sessions.values()) {
            if (minLineIndex == -1) {
                minLineIndex = session.getLastLineIndex();
            } else if (session.getLastLineIndex() < minLineIndex) {
                minLineIndex = session.getLastLineIndex();
            }
        }
        return minLineIndex;
    }

    protected boolean isUsingGroup(String group) {
        if (StringUtils.hasLength(group)) {
            for (PollingAppMonSession session : sessions.values()) {
                if (session.isValid()) {
                    String[] savedGroups = session.getJoinedGroups();
                    if (savedGroups != null) {
                        for (String saved : savedGroups) {
                            if (group.equals(saved)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    protected void scavenge() {
        List<String> expiredSessions = new ArrayList<>();
        for (Map.Entry<String, PollingAppMonSession> entry : sessions.entrySet()) {
            String id = entry.getKey();
            PollingAppMonSession session = entry.getValue();
            if (session.isExpired()) {
                appMonManager.release(session);
                session.destroy();
                expiredSessions.add(id);
            }
        }
        for (String id : expiredSessions) {
            sessions.remove(id);
        }
        if (sessions.isEmpty()) {
            buffer.clear();
        } else {
            shrinkBuffer();
        }
    }

    protected Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    protected void doInitialize() throws Exception {
        scheduler.start();
    }

    @Override
    protected void doDestroy() throws Exception {
        scheduler.stop();
        buffer.clear();
    }

}

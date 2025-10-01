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
package com.aspectran.appmon.exporter.event.session;

import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.utils.annotation.jsr305.NonNull;

/**
 * A listener for session lifecycle events.
 * It forwards session events to the {@link SessionEventReader} for processing.
 *
 * <p>Created: 2024-12-13</p>
 */
public class SessionEventReadingListener implements SessionListener {

    private final SessionEventReader eventReader;

    /**
     * Instantiates a new SessionEventReadingListener.
     * @param eventReader the reader to which events will be forwarded
     */
    public SessionEventReadingListener(SessionEventReader eventReader) {
        this.eventReader = eventReader;
    }

    @Override
    public void sessionCreated(@NonNull Session session) {
        eventReader.sessionCreated(session);
    }

    @Override
    public void sessionDestroyed(@NonNull Session session) {
        eventReader.sessionDestroyed(session);
    }

    @Override
    public void sessionEvicted(@NonNull Session session) {
        eventReader.sessionEvicted(session);
    }

    @Override
    public void sessionResided(@NonNull Session session) {
        eventReader.sessionResided(session);
    }

    @Override
    public void attributeAdded(Session session, String name, Object value) {
        eventReader.attributeAdded(session, name);
    }

    @Override
    public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
        eventReader.attributeUpdated(session, name);
    }

}

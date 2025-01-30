/*
 * Copyright (c) 2020-2025 The Aspectran Project
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
package com.aspectran.aspectow.appmon.backend.service.websocket;

import com.aspectran.aspectow.appmon.backend.service.BackendSession;
import com.aspectran.utils.Assert;
import com.aspectran.utils.annotation.jsr305.Nullable;
import jakarta.websocket.Session;

public class WebsocketBackendSession implements BackendSession {

    private static final String JOINED_GROUPS_PROPERTY = "appmon:JoinedGroups";

    private final Session session;

    public WebsocketBackendSession(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    @Override
    public String[] getJoinedGroups() {
        return (String[])session.getUserProperties().get(JOINED_GROUPS_PROPERTY);
    }

    @Override
    public void saveJoinedGroups(String[] joinGroupNames) {
        Assert.notNull(joinGroupNames, "joinGroupNames must not be null");
        session.getUserProperties().put(JOINED_GROUPS_PROPERTY, joinGroupNames);
    }

    @Override
    public void removeJoinedGroups() {
        session.getUserProperties().remove(JOINED_GROUPS_PROPERTY);
    }

    @Override
    public boolean isTwoWay() {
        return true;
    }

    @Override
    public boolean isValid() {
        return session.isOpen();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other || session == other) {
            return true;
        }
        if (other instanceof WebsocketBackendSession that) {
            return session.equals(that.getSession());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return session.hashCode();
    }

}

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
package com.aspectran.aspectow.appmon.engine.relay.websocket;

import com.aspectran.aspectow.appmon.engine.relay.RelaySession;
import com.aspectran.utils.Assert;
import com.aspectran.web.websocket.jsr356.WrappedSession;
import jakarta.websocket.Session;

/**
 * A {@link RelaySession} implementation that wraps a JSR-356 {@link Session}.
 * It stores session-specific data, like joined instances, in the WebSocket session's user properties.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class WebsocketRelaySession extends WrappedSession implements RelaySession {

    private static final String JOINED_APPS_PROPERTY = "appmon:JoinedApps";

    private static final String TIME_ZONE_PROPERTY = "appmon:timeZone";

    private static final String FOCUSED_APP_ID_PROPERTY = "appmon:focusedAppId";

    /**
     * Instantiates a new WebsocketServiceSession.
     * @param session the underlying WebSocket session
     */
    public WebsocketRelaySession(Session session) {
        super(session);
    }

    @Override
    public String getId() {
        return getSession().getId();
    }

    @Override
    public String[] getJoinedApps() {
        return (String[])getSession().getUserProperties().get(JOINED_APPS_PROPERTY);
    }

    @Override
    public void setJoinedApps(String[] appIds) {
        Assert.notNull(appIds, "appIds must not be null");
        getSession().getUserProperties().put(JOINED_APPS_PROPERTY, appIds);
    }

    @Override
    public void removeJoinedApps() {
        getSession().getUserProperties().remove(JOINED_APPS_PROPERTY);
    }

    @Override
    public String getTimeZone() {
        return (String)getSession().getUserProperties().get(TIME_ZONE_PROPERTY);
    }

    public void setTimeZone(String timeZone) {
        getSession().getUserProperties().put(TIME_ZONE_PROPERTY, timeZone);
    }

    @Override
    public String getFocusedAppId() {
        return (String)getSession().getUserProperties().get(FOCUSED_APP_ID_PROPERTY);
    }

    @Override
    public void setFocusedAppId(String appId) {
        getSession().getUserProperties().put(FOCUSED_APP_ID_PROPERTY, appId);
    }

    @Override
    public boolean isValid() {
        return getSession().isOpen();
    }

}

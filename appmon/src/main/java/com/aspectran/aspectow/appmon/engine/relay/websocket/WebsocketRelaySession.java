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
import com.aspectran.utils.StringUtils;
import com.aspectran.web.websocket.jsr356.WrappedSession;
import jakarta.websocket.Session;

/**
 * A {@link RelaySession} implementation that wraps a JSR-356 {@link Session}.
 * It stores session-specific data, like subscribed instances, in the WebSocket session's user properties.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class WebsocketRelaySession extends WrappedSession implements RelaySession {

    private static final String SUBSCRIBED_APPS_PROPERTY = "appmon:subscribedApps";

    private static final String TIME_ZONE_PROPERTY = "appmon:timeZone";

    private static final String FOCUSED_APP_ID_PROPERTY = "appmon:focusedAppId";

    /**
     * Instantiates a new WebsocketServiceSession.
     * @param session the underlying WebSocket session
     */
    public WebsocketRelaySession(Session session) {
        super(session);
    }

    /**
     * Returns the unique identifier of the WebSocket session.
     * @return the session ID
     */
    @Override
    public String getId() {
        return getSession().getId();
    }

    /**
     * Returns the time zone specified for this WebSocket session.
     * @return the time zone ID
     */
    @Override
    public String getTimeZone() {
        return (String)getSession().getUserProperties().get(TIME_ZONE_PROPERTY);
    }

    /**
     * Sets the time zone ID in the WebSocket session's user properties.
     * @param timeZone the time zone ID
     */
    public void setTimeZone(String timeZone) {
        getSession().getUserProperties().put(TIME_ZONE_PROPERTY, timeZone);
    }

    /**
     * Returns the array of application IDs subscribed to in this WebSocket session.
     * @return an array of subscribed application IDs
     */
    @Override
    public String[] getSubscribedApps() {
        return (String[])getSession().getUserProperties().get(SUBSCRIBED_APPS_PROPERTY);
    }

    /**
     * Sets the application IDs to subscribe to in the WebSocket session's user properties.
     * @param appIds an array of application IDs to subscribe to
     */
    @Override
    public void setSubscribedApps(String[] appIds) {
        Assert.notNull(appIds, "appIds must not be null");
        getSession().getUserProperties().put(SUBSCRIBED_APPS_PROPERTY, appIds);
    }

    /**
     * Removes the subscribed applications property from the WebSocket session.
     */
    @Override
    public void removeSubscribedApps() {
        getSession().getUserProperties().remove(SUBSCRIBED_APPS_PROPERTY);
    }

    /**
     * Returns the application ID currently focused in this WebSocket session.
     * @return the focused application ID, or {@code null} if none
     */
    @Override
    public String getFocusedAppId() {
        return (String)getSession().getUserProperties().get(FOCUSED_APP_ID_PROPERTY);
    }

    /**
     * Sets the focused application ID in the WebSocket session's user properties.
     * @param appId the focused application ID to set
     */
    @Override
    public void setFocusedAppId(String appId) {
        if (StringUtils.hasText(appId)) {
            getSession().getUserProperties().put(FOCUSED_APP_ID_PROPERTY, appId);
        } else {
            getSession().getUserProperties().remove(FOCUSED_APP_ID_PROPERTY);
        }
    }

    /**
     * Checks if the underlying WebSocket session is open.
     * @return {@code true} if the session is open, {@code false} otherwise
     */
    @Override
    public boolean isValid() {
        return getSession().isOpen();
    }

}

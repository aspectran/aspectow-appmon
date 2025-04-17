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
package com.aspectran.appmon.service.websocket;

import com.aspectran.appmon.service.ServiceSession;
import com.aspectran.utils.Assert;
import com.aspectran.web.websocket.jsr356.WrappedSession;
import jakarta.websocket.Session;

public class WebsocketServiceSession extends WrappedSession implements ServiceSession {

    private static final String JOINED_INSTANCES_PROPERTY = "appmon:JoinedInstances";

    private static final String TIME_ZONE_PROPERTY = "appmon:timeZone";

    public WebsocketServiceSession(Session session) {
        super(session);
    }

    @Override
    public String[] getJoinedInstances() {
        return (String[])getSession().getUserProperties().get(JOINED_INSTANCES_PROPERTY);
    }

    @Override
    public void setJoinedInstances(String[] instanceNames) {
        Assert.notNull(instanceNames, "instanceNames must not be null");
        getSession().getUserProperties().put(JOINED_INSTANCES_PROPERTY, instanceNames);
    }

    @Override
    public void removeJoinedInstances() {
        getSession().getUserProperties().remove(JOINED_INSTANCES_PROPERTY);
    }

    @Override
    public String getTimeZone() {
        return (String)getSession().getUserProperties().get(TIME_ZONE_PROPERTY);
    }

    public void setTimeZone(String timeZone) {
        getSession().getUserProperties().put(TIME_ZONE_PROPERTY, timeZone);
    }

    @Override
    public boolean isValid() {
        return getSession().isOpen();
    }

}

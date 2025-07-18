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
package com.aspectran.appmon.persist.counter.session;

import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListener;

/**
 * <p>Created: 2024-12-13</p>
 */
public class SessionEventCountingListener implements SessionListener {

    private final SessionEventCounter sessionEventCounter;

    public SessionEventCountingListener(SessionEventCounter sessionEventCounter) {
        this.sessionEventCounter = sessionEventCounter;
    }

    @Override
    public void sessionCreated(Session session) {
        sessionEventCounter.sessionCreated();
    }

}

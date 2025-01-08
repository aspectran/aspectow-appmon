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
package com.aspectran.appmon.config;

import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

import java.util.List;

/**
 * <p>Created: 2020/02/12</p>
 */
public class GroupInfo extends AbstractParameters {

    private static final ParameterKey name;
    private static final ParameterKey title;
    private static final ParameterKey event;
    private static final ParameterKey state;
    private static final ParameterKey log;

    private static final ParameterKey[] parameterKeys;

    static {
        name = new ParameterKey("name", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        state = new ParameterKey("states", new String[] {"state"}, StateInfo.class, true, true);
        event = new ParameterKey("events", new String[] {"event"}, EventInfo.class, true, true);
        log = new ParameterKey("logs", new String[] {"log"}, LogInfo.class, true, true);

        parameterKeys = new ParameterKey[] {
                name,
                title,
                event,
                state,
                log
        };
    }

    public GroupInfo() {
        super(parameterKeys);
    }

    public String getName() {
        return getString(name);
    }

    public void setName(String name) {
        putValue(GroupInfo.name, name);
    }

    public String getTitle() {
        return getString(title);
    }

    public void setTitle(String name) {
        putValue(GroupInfo.title, name);
    }

    public List<EventInfo> getEventInfoList() {
        return getParametersList(event);
    }

    public List<StateInfo> getStateInfoList() {
        return getParametersList(state);
    }

    public List<LogInfo> getLogInfoList() {
        return getParametersList(log);
    }

}

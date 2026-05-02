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
package com.aspectran.aspectow.console.scheduler.bridge;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Represents a structured request for scheduler management.
 */
public class SchedulerRequestParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey targetNodeId;
    public static final ParameterKey command;
    public static final ParameterKey serviceName;
    public static final ParameterKey scheduleId;
    public static final ParameterKey jobName;
    public static final ParameterKey loggingGroup;
    public static final ParameterKey loadedLines;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        targetNodeId = new ParameterKey("targetNodeId", ValueType.STRING);
        command = new ParameterKey("command", ValueType.STRING);
        serviceName = new ParameterKey("serviceName", ValueType.STRING);
        scheduleId = new ParameterKey("scheduleId", ValueType.STRING);
        jobName = new ParameterKey("jobName", ValueType.STRING);
        loggingGroup = new ParameterKey("loggingGroup", ValueType.STRING);
        loadedLines = new ParameterKey("loadedLines", ValueType.INT);

        parameterKeys = new ParameterKey[] {
                header,
                targetNodeId,
                command,
                serviceName,
                scheduleId,
                jobName,
                loggingGroup,
                loadedLines
        };
    }

    public SchedulerRequestParameters() {
        super(parameterKeys);
    }

    public String getHeader() {
        return getString(header);
    }

    public String getTargetNodeId() {
        return getString(targetNodeId);
    }

    public String getCommand() {
        return getString(command);
    }

    public String getServiceName() {
        return getString(serviceName);
    }

    public String getScheduleId() {
        return getString(scheduleId);
    }

    public String getJobName() {
        return getString(jobName);
    }

    public String getLoggingGroup() {
        return getString(loggingGroup);
    }

    public int getLoadedLines() {
        return getInt(loadedLines, 0);
    }

}

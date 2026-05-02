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
package com.aspectran.aspectow.console.scheduler.manager;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Metadata for scheduler log file monitoring.
 */
public class SchedulerLogInfo extends DefaultParameters {

    private static final ParameterKey contextName;
    private static final ParameterKey logFile;
    private static final ParameterKey sampleInterval;
    private static final ParameterKey lastLines;

    private static final ParameterKey[] parameterKeys;

    static {
        contextName = new ParameterKey("contextName", ValueType.STRING);
        logFile = new ParameterKey("logFile", ValueType.STRING);
        sampleInterval = new ParameterKey("sampleInterval", ValueType.LONG);
        lastLines = new ParameterKey("lastLines", ValueType.INT);

        parameterKeys = new ParameterKey[] {
                contextName,
                logFile,
                sampleInterval,
                lastLines
        };
    }

    public SchedulerLogInfo() {
        super(parameterKeys);
    }

    public String getContextName() {
        return getString(contextName);
    }

    public void setContextName(String contextName) {
        putValue(SchedulerLogInfo.contextName, contextName);
    }

    public String getLogFile() {
        return getString(logFile);
    }

    public void setLogFile(String logFile) {
        putValue(SchedulerLogInfo.logFile, logFile);
    }

    public long getSampleInterval() {
        return getLong(sampleInterval, 1000L);
    }

    public void setSampleInterval(long sampleInterval) {
        putValue(SchedulerLogInfo.sampleInterval, sampleInterval);
    }

    public int getLastLines() {
        return getInt(lastLines, 100);
    }

    public void setLastLines(int lastLines) {
        putValue(SchedulerLogInfo.lastLines, lastLines);
    }

}

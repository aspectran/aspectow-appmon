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
package com.aspectran.aspectow.appmon.backend.config;

import com.aspectran.utils.Assert;
import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * <p>Created: 2020/02/12</p>
 */
public class LogInfo extends AbstractParameters {

    private static final ParameterKey name;
    private static final ParameterKey title;
    private static final ParameterKey file;
    private static final ParameterKey charset;
    private static final ParameterKey sampleInterval;
    private static final ParameterKey lastLines;

    private static final ParameterKey[] parameterKeys;

    static {
        name = new ParameterKey("name", ValueType.STRING);
        file = new ParameterKey("file", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        charset = new ParameterKey("charset", ValueType.STRING);
        sampleInterval = new ParameterKey("sampleInterval", ValueType.INT);
        lastLines = new ParameterKey("lastLines", ValueType.INT);

        parameterKeys = new ParameterKey[] {
                name,
                title,
                file,
                charset,
                sampleInterval,
                lastLines
        };
    }

    private String instanceName;

    public LogInfo() {
        super(parameterKeys);
    }

    public String getInstanceName() {
        return instanceName;
    }

    void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getName() {
        return getString(name);
    }

    public void setName(String name) {
        putValue(LogInfo.name, name);
    }

    public String getTitle() {
        return getString(title);
    }

    public void setTitle(String title) {
        putValue(LogInfo.title, title);
    }

    public String getFile() {
        return getString(file);
    }

    public void setFile(String file) {
        putValue(LogInfo.file, file);
    }

    public String getCharset() {
        return getString(charset);
    }

    public void setCharset(String charset) {
        putValue(LogInfo.charset, charset);
    }

    public int getSampleInterval() {
        return getInt(sampleInterval, 0);
    }

    public void setSampleInterval(int sampleInterval) {
        putValue(LogInfo.sampleInterval, sampleInterval);
    }

    public int getLastLines() {
        return getInt(lastLines, 0);
    }

    public void setLastLines(int lastLines) {
        putValue(LogInfo.lastLines, lastLines);
    }

    public void validateRequiredParameters() {
        Assert.hasLength(getString(name), "Missing value of required parameter: " + getQualifiedName(name));
        Assert.hasLength(getString(file), "Missing value of required parameter: " + getQualifiedName(file));
    }

}

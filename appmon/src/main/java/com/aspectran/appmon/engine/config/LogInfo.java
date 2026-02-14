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
package com.aspectran.appmon.engine.config;

import com.aspectran.utils.Assert;
import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Contains configuration for tailing a specific log file.
 * This includes the file path, character set, and other tailing options.
 *
 * <p>Created: 2020/02/12</p>
 */
public class LogInfo extends DefaultParameters {

    private static final ParameterKey name;
    private static final ParameterKey title;
    private static final ParameterKey file;
    private static final ParameterKey archivedDir;
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
        archivedDir = new ParameterKey("archivedDir", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                name,
                title,
                file,
                archivedDir,
                charset,
                sampleInterval,
                lastLines
        };
    }

    private String domainName;

    private String instanceName;

    /**
     * Instantiates a new LogInfo.
     */
    public LogInfo() {
        super(parameterKeys);
    }

    /**
     * Gets the name of the domain this log belongs to.
     * @return the domain name
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Sets the name of the domain this log belongs to.
     * @param domainName the domain name
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * Gets the name of the instance this log belongs to.
     * @return the instance name
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Sets the name of the instance this log belongs to.
     * @param instanceName the instance name
     */
    void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * Gets the name of the log configuration.
     * @return the log name
     */
    public String getName() {
        return getString(name);
    }

    /**
     * Sets the name of the log configuration.
     * @param name the log name
     */
    public void setName(String name) {
        putValue(LogInfo.name, name);
    }

    /**
     * Gets the display title of the log.
     * @return the log title
     */
    public String getTitle() {
        return getString(title);
    }

    /**
     * Sets the display title of the log.
     * @param title the log title
     */
    public void setTitle(String title) {
        putValue(LogInfo.title, title);
    }

    /**
     * Gets the path to the log file.
     * @return the log file path
     */
    public String getFile() {
        return getString(file);
    }

    /**
     * Sets the path to the log file.
     * @param file the log file path
     */
    public void setFile(String file) {
        putValue(LogInfo.file, file);
    }

    /**
     * Gets the path to the directory where archived log files are stored.
     * @return the archived log directory path
     */
    public String getArchivedDir() {
        return getString(archivedDir);
    }

    /**
     * Sets the path to the directory where archived log files are stored.
     * @param archivedDir the archived log directory path
     */
    public void setArchivedDir(String archivedDir) {
        putValue(LogInfo.archivedDir, archivedDir);
    }

    /**
     * Gets the character set of the log file.
     * @return the character set
     */
    public String getCharset() {
        return getString(charset);
    }

    /**
     * Sets the character set of the log file.
     * @param charset the character set
     */
    public void setCharset(String charset) {
        putValue(LogInfo.charset, charset);
    }

    /**
     * Gets the sample interval in seconds for tailing the log.
     * @return the sample interval
     */
    public int getSampleInterval() {
        return getInt(sampleInterval, 0);
    }

    /**
     * Sets the sample interval in seconds for tailing the log.
     * @param sampleInterval the sample interval
     */
    public void setSampleInterval(int sampleInterval) {
        putValue(LogInfo.sampleInterval, sampleInterval);
    }

    /**
     * Gets the number of last lines to read from the log file initially.
     * @return the number of last lines
     */
    public int getLastLines() {
        return getInt(lastLines, 0);
    }

    /**
     * Sets the number of last lines to read from the log file initially.
     * @param lastLines the number of last lines
     */
    public void setLastLines(int lastLines) {
        putValue(LogInfo.lastLines, lastLines);
    }

    /**
     * Validates that all required parameters are present.
     */
    public void validateRequiredParameters() {
        Assert.hasLength(getString(name), "Missing value of required parameter: " + getQualifiedName(name));
        Assert.hasLength(getString(file), "Missing value of required parameter: " + getQualifiedName(file));
    }

}

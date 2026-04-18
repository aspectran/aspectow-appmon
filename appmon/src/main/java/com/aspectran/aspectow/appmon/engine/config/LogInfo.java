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
package com.aspectran.aspectow.appmon.engine.config;

import com.aspectran.utils.Assert;
import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Contains configuration for monitoring and tailing a specific log file.
 *
 * <p>Created: 2020/02/12</p>
 */
public class LogInfo extends DefaultParameters {

    private static final ParameterKey id;
    private static final ParameterKey title;
    private static final ParameterKey file;
    private static final ParameterKey archivedDir;
    private static final ParameterKey charset;
    private static final ParameterKey sampleInterval;
    private static final ParameterKey lastLines;

    private static final ParameterKey[] parameterKeys;

    static {
        id = new ParameterKey("id", ValueType.STRING);
        file = new ParameterKey("file", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        charset = new ParameterKey("charset", ValueType.STRING);
        sampleInterval = new ParameterKey("sampleInterval", ValueType.INT);
        lastLines = new ParameterKey("lastLines", ValueType.INT);
        archivedDir = new ParameterKey("archivedDir", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                id,
                title,
                file,
                archivedDir,
                charset,
                sampleInterval,
                lastLines
        };
    }

    private String nodeId;

    private String instanceId;

    /**
     * Instantiates a new LogInfo.
     */
    public LogInfo() {
        super(parameterKeys);
    }

    /**
     * Returns the identifier of the node to which this log configuration belongs.
     * @return the node identifier
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Sets the identifier of the node to which this log configuration belongs.
     * @param nodeId the node identifier
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Returns the identifier of the application instance to which this log configuration belongs.
     * @return the instance identifier
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the identifier of the application instance to which this log configuration belongs.
     * @param instanceId the instance identifier
     */
    void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Returns the unique identifier for this log configuration.
     * @return the log identifier
     */
    public String getLogId() {
        return getString(id);
    }

    /**
     * Sets the unique identifier for this log configuration.
     * @param name the log identifier
     */
    public void setLogId(String name) {
        putValue(LogInfo.id, name);
    }

    /**
     * Returns the display title for the log.
     * @return the log title
     */
    public String getTitle() {
        return getString(title);
    }

    /**
     * Sets the display title for the log.
     * @param title the log title
     */
    public void setTitle(String title) {
        putValue(LogInfo.title, title);
    }

    /**
     * Returns the absolute path to the log file being monitored.
     * @return the log file path
     */
    public String getFile() {
        return getString(file);
    }

    /**
     * Sets the absolute path to the log file being monitored.
     * @param file the log file path
     */
    public void setFile(String file) {
        putValue(LogInfo.file, file);
    }

    /**
     * Returns the path to the directory where archived or rotated log files are stored.
     * @return the archived log directory path
     */
    public String getArchivedDir() {
        return getString(archivedDir);
    }

    /**
     * Sets the path to the directory where archived or rotated log files are stored.
     * @param archivedDir the archived log directory path
     */
    public void setArchivedDir(String archivedDir) {
        putValue(LogInfo.archivedDir, archivedDir);
    }

    /**
     * Returns the character encoding of the log file.
     * @return the character set name
     */
    public String getCharset() {
        return getString(charset);
    }

    /**
     * Sets the character encoding of the log file.
     * @param charset the character set name
     */
    public void setCharset(String charset) {
        putValue(LogInfo.charset, charset);
    }

    /**
     * Returns the interval (in milliseconds) at which the log file should be sampled for new entries.
     * @return the sample interval
     */
    public int getSampleInterval() {
        return getInt(sampleInterval, 0);
    }

    /**
     * Sets the interval (in milliseconds) at which the log file should be sampled for new entries.
     * @param sampleInterval the sample interval
     */
    public void setSampleInterval(int sampleInterval) {
        putValue(LogInfo.sampleInterval, sampleInterval);
    }

    /**
     * Returns the number of initial lines to read from the end of the log file upon initialization.
     * @return the number of initial lines
     */
    public int getLastLines() {
        return getInt(lastLines, 0);
    }

    /**
     * Sets the number of initial lines to read from the end of the log file upon initialization.
     * @param lastLines the number of initial lines
     */
    public void setLastLines(int lastLines) {
        putValue(LogInfo.lastLines, lastLines);
    }

    /**
     * Validates that all required configuration parameters for the log are present.
     * @throws IllegalArgumentException if any required parameter is missing
     */
    public void validateRequiredParameters() {
        Assert.hasLength(getString(id), "Missing value of required parameter: " + getQualifiedName(id));
        Assert.hasLength(getString(file), "Missing value of required parameter: " + getQualifiedName(file));
    }

}

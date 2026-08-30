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
package com.aspectran.aspectow.console.build.bridge;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;
import com.aspectran.utils.json.JsonBuilder;

import java.util.List;

/**
 * Represents a structured response message for build events and log streaming.
 *
 * <p>Created: 2026-08-18</p>
 */
public class BuildResponseParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey executionId;
    public static final ParameterKey nodeId;
    public static final ParameterKey status;
    public static final ParameterKey exitCode;
    public static final ParameterKey durationMs;
    public static final ParameterKey line;
    public static final ParameterKey lines;
    public static final ParameterKey gitBranch;
    public static final ParameterKey gitCommitBefore;
    public static final ParameterKey gitCommitAfter;
    public static final ParameterKey gitCommitMsg;
    public static final ParameterKey startedAt;
    public static final ParameterKey scriptName;
    public static final ParameterKey error;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        executionId = new ParameterKey("executionId", ValueType.STRING);
        nodeId = new ParameterKey("nodeId", ValueType.STRING);
        status = new ParameterKey("status", ValueType.STRING);
        exitCode = new ParameterKey("exitCode", ValueType.INT);
        durationMs = new ParameterKey("durationMs", ValueType.LONG);
        line = new ParameterKey("line", ValueType.STRING);
        lines = new ParameterKey("lines", ValueType.STRING, true);
        gitBranch = new ParameterKey("gitBranch", ValueType.STRING);
        gitCommitBefore = new ParameterKey("gitCommitBefore", ValueType.STRING);
        gitCommitAfter = new ParameterKey("gitCommitAfter", ValueType.STRING);
        gitCommitMsg = new ParameterKey("gitCommitMsg", ValueType.STRING);
        startedAt = new ParameterKey("startedAt", ValueType.STRING);
        scriptName = new ParameterKey("scriptName", ValueType.STRING);
        error = new ParameterKey("error", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                header,
                executionId,
                nodeId,
                status,
                exitCode,
                durationMs,
                line,
                lines,
                gitBranch,
                gitCommitBefore,
                gitCommitAfter,
                gitCommitMsg,
                startedAt,
                scriptName,
                error
        };
    }

    /**
     * Constructs a new BuildResponseParameters.
     */
    public BuildResponseParameters() {
        super(parameterKeys);
    }

    /**
     * Returns the message header/type (e.g. "log", "logBackfill", "status", "subscribed").
     * @return the message header
     */
    public String getHeader() {
        return getString(header);
    }

    /**
     * Sets the message header/type.
     * @param value the message header
     * @return this parameters instance
     */
    public BuildResponseParameters setHeader(String value) {
        putValue(header, value);
        return this;
    }

    /**
     * Returns the build execution ID.
     * @return the execution ID
     */
    public String getExecutionId() {
        return getString(executionId);
    }

    /**
     * Sets the build execution ID.
     * @param value the execution ID
     * @return this parameters instance
     */
    public BuildResponseParameters setExecutionId(String value) {
        putValue(executionId, value);
        return this;
    }

    /**
     * Returns the target node ID.
     * @return the node ID
     */
    public String getNodeId() {
        return getString(nodeId);
    }

    /**
     * Sets the target node ID.
     * @param value the node ID
     * @return this parameters instance
     */
    public BuildResponseParameters setNodeId(String value) {
        putValue(nodeId, value);
        return this;
    }

    /**
     * Returns the build execution status.
     * @return the status
     */
    public String getStatus() {
        return getString(status);
    }

    /**
     * Sets the build execution status.
     * @param value the status
     * @return this parameters instance
     */
    public BuildResponseParameters setStatus(String value) {
        putValue(status, value);
        return this;
    }

    /**
     * Returns the process exit code.
     * @return the exit code
     */
    public Integer getExitCode() {
        return getInt(exitCode);
    }

    /**
     * Sets the process exit code.
     * @param value the exit code
     * @return this parameters instance
     */
    public BuildResponseParameters setExitCode(Integer value) {
        putValue(exitCode, value);
        return this;
    }

    /**
     * Returns the execution duration in milliseconds.
     * @return the duration in milliseconds
     */
    public Long getDurationMs() {
        return getLong(durationMs);
    }

    /**
     * Sets the execution duration in milliseconds.
     * @param value the duration in milliseconds
     * @return this parameters instance
     */
    public BuildResponseParameters setDurationMs(Long value) {
        putValue(durationMs, value);
        return this;
    }

    /**
     * Returns a single log output line.
     * @return the log line
     */
    public String getLine() {
        return getString(line);
    }

    /**
     * Sets a single log output line.
     * @param value the log line
     * @return this parameters instance
     */
    public BuildResponseParameters setLine(String value) {
        putValue(line, value);
        return this;
    }

    /**
     * Returns the list of backfilled log lines.
     * @return list of log lines
     */
    public List<String> getLines() {
        return getStringList(lines);
    }

    /**
     * Sets the list of backfilled log lines.
     * @param value list of log lines
     * @return this parameters instance
     */
    public BuildResponseParameters setLines(List<String> value) {
        putValue(lines, value);
        return this;
    }

    /**
     * Returns the Git branch or tag name.
     * @return the Git branch
     */
    public String getGitBranch() {
        return getString(gitBranch);
    }

    /**
     * Sets the Git branch or tag name.
     * @param value the Git branch
     * @return this parameters instance
     */
    public BuildResponseParameters setGitBranch(String value) {
        putValue(gitBranch, value);
        return this;
    }

    /**
     * Returns the Git commit hash before the build execution.
     * @return the commit hash before execution
     */
    public String getGitCommitBefore() {
        return getString(gitCommitBefore);
    }

    /**
     * Sets the Git commit hash before the build execution.
     * @param value the commit hash before execution
     * @return this parameters instance
     */
    public BuildResponseParameters setGitCommitBefore(String value) {
        putValue(gitCommitBefore, value);
        return this;
    }

    /**
     * Returns the Git commit hash after the build execution.
     * @return the commit hash after execution
     */
    public String getGitCommitAfter() {
        return getString(gitCommitAfter);
    }

    /**
     * Sets the Git commit hash after the build execution.
     * @param value the commit hash after execution
     * @return this parameters instance
     */
    public BuildResponseParameters setGitCommitAfter(String value) {
        putValue(gitCommitAfter, value);
        return this;
    }

    /**
     * Returns the Git commit message.
     * @return the Git commit message
     */
    public String getGitCommitMsg() {
        return getString(gitCommitMsg);
    }

    /**
     * Sets the Git commit message.
     * @param value the Git commit message
     * @return this parameters instance
     */
    public BuildResponseParameters setGitCommitMsg(String value) {
        putValue(gitCommitMsg, value);
        return this;
    }

    /**
     * Returns the started timestamp string.
     * @return the started timestamp
     */
    public String getStartedAt() {
        return getString(startedAt);
    }

    /**
     * Sets the started timestamp string.
     * @param value the started timestamp
     * @return this parameters instance
     */
    public BuildResponseParameters setStartedAt(String value) {
        putValue(startedAt, value);
        return this;
    }

    /**
     * Returns the script name executed.
     * @return the script name
     */
    public String getScriptName() {
        return getString(scriptName);
    }

    /**
     * Sets the script name executed.
     * @param value the script name
     * @return this parameters instance
     */
    public BuildResponseParameters setScriptName(String value) {
        putValue(scriptName, value);
        return this;
    }

    /**
     * Returns the error summary string.
     * @return the error summary
     */
    public String getError() {
        return getString(error);
    }

    /**
     * Sets the error summary string.
     * @param value the error summary
     * @return this parameters instance
     */
    public BuildResponseParameters setError(String value) {
        putValue(error, value);
        return this;
    }

    @Override
    public String toString() {
        try {
            return new JsonBuilder()
                    .prettyPrint(false)
                    .nullWritable(false)
                    .put(this)
                    .toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

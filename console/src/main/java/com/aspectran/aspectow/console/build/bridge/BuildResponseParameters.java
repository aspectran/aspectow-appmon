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
                error
        };
    }

    public BuildResponseParameters() {
        super(parameterKeys);
    }

    public String getHeader() {
        return getString(header);
    }

    public BuildResponseParameters setHeader(String value) {
        putValue(header, value);
        return this;
    }

    public String getExecutionId() {
        return getString(executionId);
    }

    public BuildResponseParameters setExecutionId(String value) {
        putValue(executionId, value);
        return this;
    }

    public String getNodeId() {
        return getString(nodeId);
    }

    public BuildResponseParameters setNodeId(String value) {
        putValue(nodeId, value);
        return this;
    }

    public String getStatus() {
        return getString(status);
    }

    public BuildResponseParameters setStatus(String value) {
        putValue(status, value);
        return this;
    }

    public Integer getExitCode() {
        return getInt(exitCode);
    }

    public BuildResponseParameters setExitCode(Integer value) {
        putValue(exitCode, value);
        return this;
    }

    public Long getDurationMs() {
        return getLong(durationMs);
    }

    public BuildResponseParameters setDurationMs(Long value) {
        putValue(durationMs, value);
        return this;
    }

    public String getLine() {
        return getString(line);
    }

    public BuildResponseParameters setLine(String value) {
        putValue(line, value);
        return this;
    }

    public List<String> getLines() {
        return getStringList(lines);
    }

    public BuildResponseParameters setLines(List<String> value) {
        putValue(lines, value);
        return this;
    }

    public String getGitBranch() {
        return getString(gitBranch);
    }

    public BuildResponseParameters setGitBranch(String value) {
        putValue(gitBranch, value);
        return this;
    }

    public String getGitCommitBefore() {
        return getString(gitCommitBefore);
    }

    public BuildResponseParameters setGitCommitBefore(String value) {
        putValue(gitCommitBefore, value);
        return this;
    }

    public String getGitCommitAfter() {
        return getString(gitCommitAfter);
    }

    public BuildResponseParameters setGitCommitAfter(String value) {
        putValue(gitCommitAfter, value);
        return this;
    }

    public String getGitCommitMsg() {
        return getString(gitCommitMsg);
    }

    public BuildResponseParameters setGitCommitMsg(String value) {
        putValue(gitCommitMsg, value);
        return this;
    }

    public String getError() {
        return getString(error);
    }

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

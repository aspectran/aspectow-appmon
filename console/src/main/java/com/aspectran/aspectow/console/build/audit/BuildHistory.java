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
package com.aspectran.aspectow.console.build.audit;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Domain model representing a build and deployment audit record (asc_build_history).
 *
 * <p>Created: 2026-08-18</p>
 */
public class BuildHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long historyId;

    private String executionId;

    private String targetNodeId;

    private String scriptName;

    private String requester;

    private String status; // PENDING, RUNNING, SUCCESS, FAILED, CANCELLED, TIMEOUT

    private Integer exitCode;

    private Instant startedAt;

    private Instant finishedAt;

    private Long durationMs;

    private String gitBranch;

    private String gitCommitBefore;

    private String gitCommitAfter;

    private String gitCommitMsg;

    private String integrityHash;

    private String errorSummary;

    private Instant createdAt;

    // Optional joined log record
    private BuildLog buildLog;

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public String getGitCommitBefore() {
        return gitCommitBefore;
    }

    public void setGitCommitBefore(String gitCommitBefore) {
        this.gitCommitBefore = gitCommitBefore;
    }

    public String getGitCommitAfter() {
        return gitCommitAfter;
    }

    public void setGitCommitAfter(String gitCommitAfter) {
        this.gitCommitAfter = gitCommitAfter;
    }

    public String getGitCommitMsg() {
        return gitCommitMsg;
    }

    public void setGitCommitMsg(String gitCommitMsg) {
        this.gitCommitMsg = gitCommitMsg;
    }

    public String getIntegrityHash() {
        return integrityHash;
    }

    public void setIntegrityHash(String integrityHash) {
        this.integrityHash = integrityHash;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public BuildLog getBuildLog() {
        return buildLog;
    }

    public void setBuildLog(BuildLog buildLog) {
        this.buildLog = buildLog;
    }

}

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

    /**
     * Returns the unique history record ID.
     * @return the history record ID
     */
    public Long getHistoryId() {
        return historyId;
    }

    /**
     * Sets the unique history record ID.
     * @param historyId the history record ID
     */
    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    /**
     * Returns the build execution ID.
     * @return the execution ID
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * Sets the build execution ID.
     * @param executionId the execution ID
     */
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    /**
     * Returns the target node ID where the build was executed.
     * @return the target node ID
     */
    public String getTargetNodeId() {
        return targetNodeId;
    }

    /**
     * Sets the target node ID where the build was executed.
     * @param targetNodeId the target node ID
     */
    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    /**
     * Returns the script name executed.
     * @return the script name
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Sets the script name executed.
     * @param scriptName the script name
     */
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the username who requested the build.
     * @return the requester username
     */
    public String getRequester() {
        return requester;
    }

    /**
     * Sets the username who requested the build.
     * @param requester the requester username
     */
    public void setRequester(String requester) {
        this.requester = requester;
    }

    /**
     * Returns the build execution status.
     * @return the execution status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the build execution status.
     * @param status the execution status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the process exit code.
     * @return the exit code
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * Sets the process exit code.
     * @param exitCode the exit code
     */
    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * Returns the start timestamp of the build.
     * @return the started timestamp
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Sets the start timestamp of the build.
     * @param startedAt the started timestamp
     */
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Returns the finish timestamp of the build.
     * @return the finished timestamp
     */
    public Instant getFinishedAt() {
        return finishedAt;
    }

    /**
     * Sets the finish timestamp of the build.
     * @param finishedAt the finished timestamp
     */
    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    /**
     * Returns the execution duration in milliseconds.
     * @return the duration in milliseconds
     */
    public Long getDurationMs() {
        return durationMs;
    }

    /**
     * Sets the execution duration in milliseconds.
     * @param durationMs the duration in milliseconds
     */
    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * Returns the Git branch or tag name.
     * @return the Git branch
     */
    public String getGitBranch() {
        return gitBranch;
    }

    /**
     * Sets the Git branch or tag name.
     * @param gitBranch the Git branch
     */
    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    /**
     * Returns the Git commit hash before the build execution.
     * @return the commit hash before execution
     */
    public String getGitCommitBefore() {
        return gitCommitBefore;
    }

    /**
     * Sets the Git commit hash before the build execution.
     * @param gitCommitBefore the commit hash before execution
     */
    public void setGitCommitBefore(String gitCommitBefore) {
        this.gitCommitBefore = gitCommitBefore;
    }

    /**
     * Returns the Git commit hash after the build execution.
     * @return the commit hash after execution
     */
    public String getGitCommitAfter() {
        return gitCommitAfter;
    }

    /**
     * Sets the Git commit hash after the build execution.
     * @param gitCommitAfter the commit hash after execution
     */
    public void setGitCommitAfter(String gitCommitAfter) {
        this.gitCommitAfter = gitCommitAfter;
    }

    /**
     * Returns the Git commit message.
     * @return the Git commit message
     */
    public String getGitCommitMsg() {
        return gitCommitMsg;
    }

    /**
     * Sets the Git commit message.
     * @param gitCommitMsg the Git commit message
     */
    public void setGitCommitMsg(String gitCommitMsg) {
        this.gitCommitMsg = gitCommitMsg;
    }

    /**
     * Returns the SHA-256 cryptographic integrity digest.
     * @return the integrity hash
     */
    public String getIntegrityHash() {
        return integrityHash;
    }

    /**
     * Sets the SHA-256 cryptographic integrity digest.
     * @param integrityHash the integrity hash
     */
    public void setIntegrityHash(String integrityHash) {
        this.integrityHash = integrityHash;
    }

    /**
     * Returns the summary of errors if the build failed.
     * @return the error summary
     */
    public String getErrorSummary() {
        return errorSummary;
    }

    /**
     * Sets the summary of errors if the build failed.
     * @param errorSummary the error summary
     */
    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    /**
     * Returns the record creation timestamp.
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the record creation timestamp.
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the associated build log entity.
     * @return the build log entity
     */
    public BuildLog getBuildLog() {
        return buildLog;
    }

    /**
     * Sets the associated build log entity.
     * @param buildLog the build log entity
     */
    public void setBuildLog(BuildLog buildLog) {
        this.buildLog = buildLog;
    }

}

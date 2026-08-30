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
package com.aspectran.aspectow.console.build.manager;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BuildExecutionInfo represents the execution state and metadata of a build/deploy task.
 *
 * <p>Created: 2026-08-18</p>
 */
public class BuildExecutionInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = -6974843859535704263L;

    public enum Status {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED,
        TIMEOUT
    }

    private String executionId;

    private String targetNodeId;

    private String targetHost;

    private String scriptName;

    private String scriptCategory;

    private Map<String, Object> parameters = new ConcurrentHashMap<>();

    private String triggerType = "MANUAL";

    private String requesterId;

    private String requesterName;

    private String requesterIp;

    private String gitBranch;

    private String gitCommitBefore;

    private String gitCommitAfter;

    private String gitCommitMsg;

    private Instant startedAt;

    private Instant finishedAt;

    private Long durationMs;

    private Status status = Status.PENDING;

    private Integer exitCode;

    private String errorSummary;

    private String integrityHash;

    /**
     * Returns the execution ID.
     * @return the execution ID
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * Sets the execution ID.
     * @param executionId the execution ID
     */
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    /**
     * Returns the target node ID.
     * @return the target node ID
     */
    public String getTargetNodeId() {
        return targetNodeId;
    }

    /**
     * Sets the target node ID.
     * @param targetNodeId the target node ID
     */
    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    /**
     * Returns the target host name or IP address.
     * @return the target host
     */
    public String getTargetHost() {
        return targetHost;
    }

    /**
     * Sets the target host name or IP address.
     * @param targetHost the target host
     */
    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    /**
     * Returns the build script name.
     * @return the script name
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Sets the build script name.
     * @param scriptName the script name
     */
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the script category.
     * @return the script category
     */
    public String getScriptCategory() {
        return scriptCategory;
    }

    /**
     * Sets the script category.
     * @param scriptCategory the script category
     */
    public void setScriptCategory(String scriptCategory) {
        this.scriptCategory = scriptCategory;
    }

    /**
     * Returns the execution parameters map.
     * @return the parameters map
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Sets the execution parameters map.
     * @param parameters the parameters map
     */
    public void setParameters(Map<String, Object> parameters) {
        if (parameters != null) {
            this.parameters = new ConcurrentHashMap<>(parameters);
        }
    }

    /**
     * Returns the trigger type (e.g. MANUAL, SCHEDULED, WEBHOOK).
     * @return the trigger type
     */
    public String getTriggerType() {
        return triggerType;
    }

    /**
     * Sets the trigger type.
     * @param triggerType the trigger type
     */
    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    /**
     * Returns the requester user ID.
     * @return the requester ID
     */
    public String getRequesterId() {
        return requesterId;
    }

    /**
     * Sets the requester user ID.
     * @param requesterId the requester ID
     */
    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    /**
     * Returns the requester username or display name.
     * @return the requester name
     */
    public String getRequesterName() {
        return requesterName;
    }

    /**
     * Sets the requester username or display name.
     * @param requesterName the requester name
     */
    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    /**
     * Returns the IP address of the requester.
     * @return the requester IP
     */
    public String getRequesterIp() {
        return requesterIp;
    }

    /**
     * Sets the IP address of the requester.
     * @param requesterIp the requester IP
     */
    public void setRequesterIp(String requesterIp) {
        this.requesterIp = requesterIp;
    }

    /**
     * Returns the Git branch or tag.
     * @return the Git branch
     */
    public String getGitBranch() {
        return gitBranch;
    }

    /**
     * Sets the Git branch or tag.
     * @param gitBranch the Git branch
     */
    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    /**
     * Returns the Git commit hash before execution.
     * @return the commit hash before execution
     */
    public String getGitCommitBefore() {
        return gitCommitBefore;
    }

    /**
     * Sets the Git commit hash before execution.
     * @param gitCommitBefore the commit hash before execution
     */
    public void setGitCommitBefore(String gitCommitBefore) {
        this.gitCommitBefore = gitCommitBefore;
    }

    /**
     * Returns the Git commit hash after execution.
     * @return the commit hash after execution
     */
    public String getGitCommitAfter() {
        return gitCommitAfter;
    }

    /**
     * Sets the Git commit hash after execution.
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
     * Returns the started timestamp.
     * @return the started timestamp
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Sets the started timestamp.
     * @param startedAt the started timestamp
     */
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Returns the finished timestamp.
     * @return the finished timestamp
     */
    public Instant getFinishedAt() {
        return finishedAt;
    }

    /**
     * Sets the finished timestamp.
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
     * Returns the execution status.
     * @return the execution status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the execution status.
     * @param status the execution status
     */
    public void setStatus(Status status) {
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
     * Returns the error summary if failed.
     * @return the error summary
     */
    public String getErrorSummary() {
        return errorSummary;
    }

    /**
     * Sets the error summary if failed.
     * @param errorSummary the error summary
     */
    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    /**
     * Returns the SHA-256 integrity hash.
     * @return the integrity hash
     */
    public String getIntegrityHash() {
        return integrityHash;
    }

    /**
     * Sets the SHA-256 integrity hash.
     * @param integrityHash the integrity hash
     */
    public void setIntegrityHash(String integrityHash) {
        this.integrityHash = integrityHash;
    }

    /**
     * Returns whether the execution is in a terminal/completed state.
     * @return true if finished; false otherwise
     */
    public boolean isFinished() {
        return status == Status.SUCCESS || status == Status.FAILED
                || status == Status.CANCELLED || status == Status.TIMEOUT;
    }

}

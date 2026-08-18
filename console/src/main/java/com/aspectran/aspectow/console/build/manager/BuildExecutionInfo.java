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

    private static final long serialVersionUID = 1L;

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

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public String getScriptCategory() {
        return scriptCategory;
    }

    public void setScriptCategory(String scriptCategory) {
        this.scriptCategory = scriptCategory;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        if (parameters != null) {
            this.parameters = new ConcurrentHashMap<>(parameters);
        }
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterIp() {
        return requesterIp;
    }

    public void setRequesterIp(String requesterIp) {
        this.requesterIp = requesterIp;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public String getIntegrityHash() {
        return integrityHash;
    }

    public void setIntegrityHash(String integrityHash) {
        this.integrityHash = integrityHash;
    }

    public boolean isFinished() {
        return status == Status.SUCCESS || status == Status.FAILED
                || status == Status.CANCELLED || status == Status.TIMEOUT;
    }

}

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

import com.aspectran.aspectow.console.build.manager.BuildExecutionInfo;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * BuildAuditService handles compliance audit record persistence, GZIP log compression,
 * SHA-256 integrity verification, and audit report generation.
 * Extends InstantActivitySupport to ensure transactional context (consoleTxAspect)
 * is present even when invoked from background virtual threads or event callbacks.
 *
 * <p>Created: 2026-08-18</p>
 */
@Component
@Bean(id = "buildAuditService")
public class BuildAuditService extends InstantActivitySupport {

    private static final Logger logger = LoggerFactory.getLogger(BuildAuditService.class);

    private final BuildHistoryMapper buildHistoryMapper;

    @Autowired
    public BuildAuditService(BuildHistoryMapper buildHistoryMapper) {
        this.buildHistoryMapper = buildHistoryMapper;
    }

    /**
     * Records the initial PENDING / RUNNING state of a build execution in the database.
     * @param info the build execution info
     * @param requester the username who requested the build
     * @return the created BuildHistory entity
     */
    public BuildHistory startAudit(@NonNull BuildExecutionInfo info, String requester) {
        return instantActivity(() -> {
            BuildHistory history = null;
            if (StringUtils.hasText(info.getExecutionId()) && StringUtils.hasText(info.getTargetNodeId())) {
                history = buildHistoryMapper.getBuildHistoryByExecutionIdAndNodeId(info.getExecutionId(), info.getTargetNodeId());
            } else if (StringUtils.hasText(info.getExecutionId())) {
                history = buildHistoryMapper.getBuildHistoryByExecutionId(info.getExecutionId());
            }

            if (history != null) {
                if (info.getStatus() != null) {
                    history.setStatus(info.getStatus().name());
                }
                if (info.getStartedAt() != null) {
                    history.setStartedAt(info.getStartedAt());
                }
                if (StringUtils.hasText(requester) && !"SYSTEM".equals(requester)) {
                    history.setRequester(requester);
                }
                if (info.getGitBranch() != null) {
                    history.setGitBranch(info.getGitBranch());
                }
                if (info.getGitCommitBefore() != null) {
                    history.setGitCommitBefore(info.getGitCommitBefore());
                }
                buildHistoryMapper.updateBuildHistory(history);
                return history;
            }

            history = new BuildHistory();
            history.setExecutionId(info.getExecutionId());
            history.setTargetNodeId(info.getTargetNodeId());
            history.setScriptName(info.getScriptName());
            history.setRequester(StringUtils.hasText(requester) ? requester : "SYSTEM");
            history.setStatus(info.getStatus() != null ? info.getStatus().name() : BuildExecutionInfo.Status.RUNNING.name());
            history.setStartedAt(info.getStartedAt() != null ? info.getStartedAt() : Instant.now());
            history.setGitBranch(info.getGitBranch());
            history.setGitCommitBefore(info.getGitCommitBefore());

            try {
                buildHistoryMapper.insertBuildHistory(history);
                if (logger.isDebugEnabled()) {
                    logger.debug("Build audit record started with ID [{}] for execution [{}]",
                            history.getHistoryId(), info.getExecutionId());
                }
            } catch (Exception e) {
                logger.error("Failed to insert initial build audit history for [{}]", info.getExecutionId(), e);
            }
            return history;
        });
    }

    /**
     * Finalizes and persists the completed build audit record with compressed logs and SHA-256 digest.
     * @param info the completed build execution info
     * @param logLines the list of captured console output lines
     */
    public void completeAudit(@NonNull BuildExecutionInfo info, List<String> logLines) {
        try {
            instantActivity(() -> {
                BuildHistory history;
                if (info.getTargetNodeId() != null) {
                    history = buildHistoryMapper.getBuildHistoryByExecutionIdAndNodeId(info.getExecutionId(), info.getTargetNodeId());
                } else {
                    history = buildHistoryMapper.getBuildHistoryByExecutionId(info.getExecutionId());
                }
                if (history == null) {
                    history = new BuildHistory();
                    history.setExecutionId(info.getExecutionId());
                    history.setTargetNodeId(info.getTargetNodeId());
                    history.setScriptName(info.getScriptName());
                    history.setRequester("SYSTEM");
                    history.setStartedAt(info.getStartedAt() != null ? info.getStartedAt() : Instant.now());
                    buildHistoryMapper.insertBuildHistory(history);
                }

                if (info.getStartedAt() != null) {
                    history.setStartedAt(info.getStartedAt());
                }
                history.setStatus(info.getStatus() != null ? info.getStatus().name() : BuildExecutionInfo.Status.FAILED.name());
                history.setExitCode(info.getExitCode());
                history.setFinishedAt(info.getFinishedAt() != null ? info.getFinishedAt() : Instant.now());
                history.setDurationMs(info.getDurationMs());
                history.setGitBranch(info.getGitBranch());
                history.setGitCommitBefore(info.getGitCommitBefore());
                history.setGitCommitAfter(info.getGitCommitAfter());
                history.setGitCommitMsg(info.getGitCommitMsg());
                history.setErrorSummary(info.getErrorSummary());

                // Build log payload
                String rawLogContent = (logLines != null && !logLines.isEmpty()) ? String.join("\n", logLines) : "";
                int lineCount = logLines != null ? logLines.size() : 0;
                long byteSize = rawLogContent.getBytes(StandardCharsets.UTF_8).length;

                // Compress logs using GZIP + Base64
                String storedLogContent;
                String compressedYn;
                if (byteSize > 1024) {
                    storedLogContent = compressGzip(rawLogContent);
                    compressedYn = "Y";
                } else {
                    storedLogContent = rawLogContent;
                    compressedYn = "N";
                }

                // Calculate SHA-256 integrity hash
                String integrityHash = calculateIntegrityHash(history, rawLogContent);
                history.setIntegrityHash(integrityHash);

                buildHistoryMapper.updateBuildHistory(history);

                // Insert build log if not present
                BuildLog existingLog = buildHistoryMapper.getBuildLogByHistoryId(history.getHistoryId());
                if (existingLog == null) {
                    BuildLog buildLog = new BuildLog();
                    buildLog.setHistoryId(history.getHistoryId());
                    buildLog.setExecutionId(history.getExecutionId());
                    buildLog.setLogContent(storedLogContent);
                    buildLog.setCompressedYn(compressedYn);
                    buildLog.setLineCount(lineCount);
                    buildLog.setByteSize(byteSize);
                    buildHistoryMapper.insertBuildLog(buildLog);
                }

                logger.info("Build audit record completed: historyId={}, executionId={}, status={}, lines={}, integrityHash={}",
                        history.getHistoryId(), history.getExecutionId(), history.getStatus(), lineCount, integrityHash);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to complete build audit record for execution [{}]", info.getExecutionId(), e);
        }
    }

    /**
     * Updates the intermediate status (RUNNING, PENDING, etc.) of a build execution in the database.
     * @param info the build execution info
     */
    public void updateStatus(@NonNull BuildExecutionInfo info) {
        try {
            instantActivity(() -> {
                BuildHistory history;
                if (info.getTargetNodeId() != null) {
                    history = buildHistoryMapper.getBuildHistoryByExecutionIdAndNodeId(info.getExecutionId(), info.getTargetNodeId());
                } else {
                    history = buildHistoryMapper.getBuildHistoryByExecutionId(info.getExecutionId());
                }
                if (history != null) {
                    if (info.getStatus() != null) {
                        history.setStatus(info.getStatus().name());
                    }
                    if (info.getGitBranch() != null) {
                        history.setGitBranch(info.getGitBranch());
                    }
                    if (info.getGitCommitBefore() != null) {
                        history.setGitCommitBefore(info.getGitCommitBefore());
                    }
                    buildHistoryMapper.updateBuildHistory(history);
                }
                return null;
            });
        } catch (Exception e) {
            logger.trace("Failed to update build audit status for execution [{}]", info.getExecutionId(), e);
        }
    }

    /**
     * Verifies the cryptographic integrity of a persisted build history record.
     * @param historyId the history ID
     * @return true if valid and unmodified; false otherwise
     */
    public boolean verifyIntegrity(Long historyId) {
        return instantActivity(() -> {
            BuildHistory history = buildHistoryMapper.getBuildHistoryById(historyId);
            if (history == null || StringUtils.isEmpty(history.getIntegrityHash())) {
                return false;
            }

            String rawLogs = getDecompressedLogsInternal(historyId);
            String calculated = calculateIntegrityHash(history, rawLogs);
            return history.getIntegrityHash().equalsIgnoreCase(calculated);
        });
    }

    /**
     * Searches build history records with criteria and pagination.
     * @param query query criteria
     * @return list of build history records
     */
    public List<BuildHistory> searchHistory(BuildAuditQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return instantActivity(() -> buildHistoryMapper.searchBuildHistory(query));
    }

    /**
     * Counts matching build history records.
     * @param query query criteria
     * @return total matching count
     */
    public long countHistory(BuildAuditQuery query) {
        if (query == null) {
            return 0;
        }
        return instantActivity(() -> buildHistoryMapper.countBuildHistory(query));
    }

    /**
     * Retrieves detailed build history including logs by history ID.
     * @param historyId the history ID
     * @return the build history entity
     */
    public BuildHistory getHistoryDetail(Long historyId) {
        return instantActivity(() -> buildHistoryMapper.getBuildHistoryById(historyId));
    }

    /**
     * Retrieves the latest build history record for a specific node ID.
     * @param targetNodeId the target node ID
     * @return the latest build history entity
     */
    public BuildHistory getLatestBuildHistory(String targetNodeId) {
        if (StringUtils.isEmpty(targetNodeId)) {
            return null;
        }
        return instantActivity(() -> buildHistoryMapper.getLatestBuildHistoryByNodeId(targetNodeId));
    }

    /**
     * Retrieves the latest build history record for each specified target node.
     * @param targetNodeIds the collection of target node IDs
     * @return list of latest build history entities per node
     */
    public List<BuildHistory> getLatestBuildHistories(Collection<String> targetNodeIds) {
        if (targetNodeIds == null || targetNodeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return instantActivity(() -> buildHistoryMapper.getLatestBuildHistories(targetNodeIds));
    }

    /**
     * Retrieves detailed build history by execution ID and target node ID.
     * @param executionId the execution ID
     * @param targetNodeId the target node ID
     * @return the build history entity
     */
    public BuildHistory getHistoryDetailByExecutionIdAndNodeId(String executionId, String targetNodeId) {
        if (StringUtils.isEmpty(executionId)) {
            return null;
        }
        return instantActivity(() -> buildHistoryMapper.getBuildHistoryByExecutionIdAndNodeId(executionId, targetNodeId));
    }

    /**
     * Retrieves detailed build history by execution ID.
     * @param executionId the execution ID
     * @return the build history entity
     */
    public BuildHistory getHistoryDetailByExecutionId(String executionId) {
        if (StringUtils.isEmpty(executionId)) {
            return null;
        }
        return instantActivity(() -> buildHistoryMapper.getBuildHistoryByExecutionId(executionId));
    }

    /**
     * Retrieves all build history master records by execution ID.
     * @param executionId the execution ID
     * @return list of build history entities for the execution
     */
    public List<BuildHistory> getBuildHistoriesByExecutionId(String executionId) {
        if (StringUtils.isEmpty(executionId)) {
            return Collections.emptyList();
        }
        return instantActivity(() -> buildHistoryMapper.getBuildHistoriesByExecutionId(executionId));
    }

    /**
     * Retrieves all decompressed console logs by execution ID grouped by target node ID.
     * @param executionId the execution ID
     * @return map of node ID to list of log lines
     */
    public java.util.Map<String, List<String>> getNodeLogsByExecutionId(String executionId) {
        if (StringUtils.isEmpty(executionId)) {
            return Collections.emptyMap();
        }
        return instantActivity(() -> {
            List<BuildHistory> histories = buildHistoryMapper.getBuildHistoriesByExecutionId(executionId);
            if (histories == null || histories.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, List<String>> result = new HashMap<>();
            for (BuildHistory h : histories) {
                if (h.getHistoryId() != null && h.getTargetNodeId() != null) {
                    String raw = getDecompressedLogsInternal(h.getHistoryId());
                    List<String> lines = (StringUtils.hasText(raw))
                            ? java.util.Arrays.asList(raw.split("\n"))
                            : Collections.emptyList();
                    result.put(h.getTargetNodeId(), lines);
                }
            }
            return result;
        });
    }

    /**
     * Retrieves and decompresses the console logs for a build history.
     * @param historyId the history ID
     * @return raw log string
     */
    public String getDecompressedLogs(Long historyId) {
        return instantActivity(() -> getDecompressedLogsInternal(historyId));
    }

    @Nullable
    private String getDecompressedLogsInternal(Long historyId) {
        BuildLog buildLog = buildHistoryMapper.getBuildLogByHistoryId(historyId);
        if (buildLog == null || buildLog.getLogContent() == null) {
            return "";
        }
        if ("Y".equalsIgnoreCase(buildLog.getCompressedYn())) {
            try {
                return decompressGzip(buildLog.getLogContent());
            } catch (Exception e) {
                logger.error("Failed to decompress GZIP log for history ID {}", historyId, e);
                return "[Error decompressing log: " + e.getMessage() + "]";
            }
        } else {
            return buildLog.getLogContent();
        }
    }

    /**
     * Retrieves and decompresses the console logs for an execution ID.
     * @param executionId the execution ID
     * @return raw log string
     */
    public String getDecompressedLogsByExecutionId(String executionId) {
        if (StringUtils.isEmpty(executionId)) {
            return "";
        }
        return instantActivity(() -> {
            BuildLog buildLog = buildHistoryMapper.getBuildLogByExecutionId(executionId);
            if (buildLog == null || buildLog.getLogContent() == null) {
                return "";
            }
            if ("Y".equalsIgnoreCase(buildLog.getCompressedYn())) {
                try {
                    return decompressGzip(buildLog.getLogContent());
                } catch (Exception e) {
                    logger.error("Failed to decompress GZIP log for execution ID {}", executionId, e);
                    return "[Error decompressing log: " + e.getMessage() + "]";
                }
            } else {
                return buildLog.getLogContent();
            }
        });
    }

    /**
     * Exports build history records as a CSV compliance audit report.
     * @param query query criteria
     * @return CSV formatted string
     */
    public String exportCsvReport(BuildAuditQuery query) {
        return instantActivity(() -> {
            List<BuildHistory> list = buildHistoryMapper.searchBuildHistory(query);
            StringBuilder sb = new StringBuilder();
            sb.append("History ID,Execution ID,Target Node,Script Name,Requester,Status,Exit Code,Started At,Finished ")
                    .append("At,Duration (ms),Git Branch,Before Commit,After Commit,Integrity Hash\n");

            for (BuildHistory h : list) {
                sb.append(escapeCsv(h.getHistoryId())).append(",")
                        .append(escapeCsv(h.getExecutionId())).append(",")
                        .append(escapeCsv(h.getTargetNodeId())).append(",")
                        .append(escapeCsv(h.getScriptName())).append(",")
                        .append(escapeCsv(h.getRequester())).append(",")
                        .append(escapeCsv(h.getStatus())).append(",")
                        .append(escapeCsv(h.getExitCode())).append(",")
                        .append(escapeCsv(h.getStartedAt())).append(",")
                        .append(escapeCsv(h.getFinishedAt())).append(",")
                        .append(escapeCsv(h.getDurationMs())).append(",")
                        .append(escapeCsv(h.getGitBranch())).append(",")
                        .append(escapeCsv(h.getGitCommitBefore())).append(",")
                        .append(escapeCsv(h.getGitCommitAfter())).append(",")
                        .append(escapeCsv(h.getIntegrityHash())).append("\n");
            }
            return sb.toString();
        });
    }

    /**
     * Computes a deterministic SHA-256 hash combining the build metadata and the full raw log content.
     */
    @NonNull
    private String calculateIntegrityHash(@NonNull BuildHistory history, String rawLogContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String delimiter = "|";
            String rawPayload = (history.getExecutionId() != null ? history.getExecutionId() : "") + delimiter +
                    (history.getTargetNodeId() != null ? history.getTargetNodeId() : "") + delimiter +
                    (history.getScriptName() != null ? history.getScriptName() : "") + delimiter +
                    (history.getRequester() != null ? history.getRequester() : "") + delimiter +
                    (history.getStatus() != null ? history.getStatus() : "") + delimiter +
                    (history.getExitCode() != null ? history.getExitCode() : "") + delimiter +
                    (history.getDurationMs() != null ? history.getDurationMs() : "") + delimiter +
                    (history.getGitCommitBefore() != null ? history.getGitCommitBefore() : "") + delimiter +
                    (history.getGitCommitAfter() != null ? history.getGitCommitAfter() : "") + delimiter +
                    sha256Hex(rawLogContent != null ? rawLogContent : "");

            byte[] hashBytes = digest.digest(rawPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            return "";
        }
    }

    @NonNull
    private String sha256Hex(String text) {
        if (StringUtils.isEmpty(text)) {
            return StringUtils.EMPTY;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return StringUtils.EMPTY;
        }
    }

    private String compressGzip(String data) throws IOException {
        if (data == null || data.isEmpty()) {
            return data;
        }
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(obj)) {
            gzip.write(data.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(obj.toByteArray());
    }

    private String decompressGzip(String compressedBase64) throws IOException {
        if (compressedBase64 == null || compressedBase64.isEmpty()) {
            return compressedBase64;
        }
        byte[] bytes = Base64.getDecoder().decode(compressedBase64);
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    @NonNull
    private String escapeCsv(Object value) {
        if (value == null) {
            return StringUtils.EMPTY;
        }
        String str = value.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

}

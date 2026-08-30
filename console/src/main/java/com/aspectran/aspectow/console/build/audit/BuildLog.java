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
 * Domain model representing build output logs (asc_build_log).
 *
 * <p>Created: 2026-08-18</p>
 */
public class BuildLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long logId;

    private Long historyId;

    private String executionId;

    private String logContent;

    private String compressedYn; // 'Y' or 'N'

    private Integer lineCount;

    private Long byteSize;

    private Instant createdAt;

    /**
     * Returns the log record ID.
     * @return the log ID
     */
    public Long getLogId() {
        return logId;
    }

    /**
     * Sets the log record ID.
     * @param logId the log ID
     */
    public void setLogId(Long logId) {
        this.logId = logId;
    }

    /**
     * Returns the foreign key reference to asc_build_history.
     * @return the history ID
     */
    public Long getHistoryId() {
        return historyId;
    }

    /**
     * Sets the foreign key reference to asc_build_history.
     * @param historyId the history ID
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
     * Returns the raw or compressed log content string.
     * @return the log content string
     */
    public String getLogContent() {
        return logContent;
    }

    /**
     * Sets the raw or compressed log content string.
     * @param logContent the log content string
     */
    public void setLogContent(String logContent) {
        this.logContent = logContent;
    }

    /**
     * Returns whether the log content is GZIP-compressed ('Y' or 'N').
     * @return 'Y' if compressed; 'N' otherwise
     */
    public String getCompressedYn() {
        return compressedYn;
    }

    /**
     * Sets whether the log content is GZIP-compressed ('Y' or 'N').
     * @param compressedYn 'Y' if compressed; 'N' otherwise
     */
    public void setCompressedYn(String compressedYn) {
        this.compressedYn = compressedYn;
    }

    /**
     * Returns the total line count of the log output.
     * @return the line count
     */
    public Integer getLineCount() {
        return lineCount;
    }

    /**
     * Sets the total line count of the log output.
     * @param lineCount the line count
     */
    public void setLineCount(Integer lineCount) {
        this.lineCount = lineCount;
    }

    /**
     * Returns the raw uncompressed byte size of the log output.
     * @return the byte size
     */
    public Long getByteSize() {
        return byteSize;
    }

    /**
     * Sets the raw uncompressed byte size of the log output.
     * @param byteSize the byte size
     */
    public void setByteSize(Long byteSize) {
        this.byteSize = byteSize;
    }

    /**
     * Returns the creation timestamp.
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}

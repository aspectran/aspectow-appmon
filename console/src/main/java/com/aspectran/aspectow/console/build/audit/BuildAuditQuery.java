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

import com.aspectran.aspectow.console.common.pagination.PageInfo;

import java.io.Serial;
import java.io.Serializable;

/**
 * Query criteria object for filtering and searching build &amp; deployment audit history.
 *
 * <p>Created: 2026-08-18</p>
 */
public class BuildAuditQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = -1855769322127615562L;

    private String targetNodeId;

    private String scriptName;

    private String requester;

    private String status;

    private String searchKeyword;

    private String startDate;

    private String endDate;

    private String timeZone;

    private PageInfo pageInfo;

    /**
     * Returns the target node ID to filter by.
     * @return the target node ID
     */
    public String getTargetNodeId() {
        return targetNodeId;
    }

    /**
     * Sets the target node ID to filter by.
     * @param targetNodeId the target node ID
     */
    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    /**
     * Returns the script name to filter by.
     * @return the script name
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Sets the script name to filter by.
     * @param scriptName the script name
     */
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the requester username to filter by.
     * @return the requester username
     */
    public String getRequester() {
        return requester;
    }

    /**
     * Sets the requester username to filter by.
     * @param requester the requester username
     */
    public void setRequester(String requester) {
        this.requester = requester;
    }

    /**
     * Returns the build execution status to filter by.
     * @return the build execution status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the build execution status to filter by.
     * @param status the build execution status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the search keyword.
     * @return the search keyword
     */
    public String getSearchKeyword() {
        return searchKeyword;
    }

    /**
     * Sets the search keyword.
     * @param searchKeyword the search keyword
     */
    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    /**
     * Returns the start date string for time-range filtering.
     * @return the start date string
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date string for time-range filtering.
     * @param startDate the start date string
     */
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    /**
     * Returns the end date string for time-range filtering.
     * @return the end date string
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date string for time-range filtering.
     * @param endDate the end date string
     */
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    /**
     * Returns the client time zone ID.
     * @return the time zone ID
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Sets the client time zone ID.
     * @param timeZone the time zone ID
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Returns the pagination information.
     * @return the pagination information
     */
    public PageInfo getPageInfo() {
        return pageInfo;
    }

    /**
     * Sets the pagination information.
     * @param pageInfo the pagination information
     */
    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

}

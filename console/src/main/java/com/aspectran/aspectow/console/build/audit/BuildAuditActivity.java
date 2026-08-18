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

import com.aspectran.aspectow.console.cluster.NodeConsoleHelper;
import com.aspectran.aspectow.console.common.pagination.PageInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BuildAuditActivity handles views and REST API endpoints for build &amp; deployment compliance audit trail.
 *
 * <p>Created: 2026-08-18</p>
 */
@Component("/cluster/build/audit")
@Profile("[console.ui, console.custom-ui]")
public class BuildAuditActivity {

    private final BuildAuditService buildAuditService;

    private final NodeManager nodeManager;

    private final NodeConsoleHelper nodeConsoleHelper;

    @Autowired
    public BuildAuditActivity(BuildAuditService buildAuditService,
                              NodeManager nodeManager,
                              NodeConsoleHelper nodeConsoleHelper) {
        this.buildAuditService = buildAuditService;
        this.nodeManager = nodeManager;
        this.nodeConsoleHelper = nodeConsoleHelper;
    }

    /**
     * Displays the build audit records page.
     * @param translet the current translet
     * @return model for rendering the view
     */
    @Request("/")
    @Dispatch("cluster/audit")
    @Action("page")
    public Map<String, Object> auditPage(@NonNull Translet translet) {
        String clusterMode = nodeManager.getClusterConfig().getMode();
        List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(true);

        String targetNodeId = translet.getParameter("nodeId");
        String status = translet.getParameter("status");
        String searchKeyword = translet.getParameter("q");

        BuildAuditQuery query = new BuildAuditQuery();
        query.setTargetNodeId(targetNodeId);
        query.setStatus(status);
        query.setSearchKeyword(searchKeyword);

        PageInfo pageInfo = PageInfo.of(translet, 15);
        long totalCount = buildAuditService.countHistory(query);
        pageInfo.setTotalElements(totalCount);
        query.setPageInfo(pageInfo);

        List<BuildHistory> historyList = buildAuditService.searchHistory(query);

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Build & Deployment Audit Trail");
        model.put("style", "audit-page");
        model.put("group", "cluster-menu");
        model.put("clusterMode", clusterMode);
        model.put("nodes", nodes);
        model.put("historyList", historyList);
        model.put("pageInfo", pageInfo);
        model.put("targetNodeId", targetNodeId);
        model.put("status", status);
        model.put("searchKeyword", searchKeyword);
        return model;
    }

    /**
     * Searches build history records and returns JSON result.
     * @param translet current translet
     * @return REST response
     */
    @RequestToGet("/list")
    public RestResponse getHistoryList(@NonNull Translet translet) {
        try {
            String targetNodeId = translet.getParameter("nodeId");
            String status = translet.getParameter("status");
            String scriptName = translet.getParameter("scriptName");
            String requester = translet.getParameter("requester");
            String searchKeyword = translet.getParameter("q");
            String startDate = translet.getParameter("startDate");
            String endDate = translet.getParameter("endDate");

            BuildAuditQuery query = new BuildAuditQuery();
            query.setTargetNodeId(targetNodeId);
            query.setStatus(status);
            query.setScriptName(scriptName);
            query.setRequester(requester);
            query.setSearchKeyword(searchKeyword);
            query.setStartDate(startDate);
            query.setEndDate(endDate);

            PageInfo pageInfo = PageInfo.of(translet, 15);
            long totalCount = buildAuditService.countHistory(query);
            pageInfo.setTotalElements(totalCount);
            query.setPageInfo(pageInfo);

            List<BuildHistory> list = buildAuditService.searchHistory(query);

            Map<String, Object> result = new HashMap<>();
            result.put("list", list);
            result.put("total", totalCount);
            result.put("page", pageInfo.getNumber());
            result.put("size", pageInfo.getSize());
            return new SuccessResponse(result).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    /**
     * Retrieves detailed record and verifies cryptographic SHA-256 integrity.
     * @param historyId history ID
     * @return detail response with verification status
     */
    @RequestToGet("/detail/${historyId}")
    public RestResponse getHistoryDetail(Long historyId) {
        if (historyId == null) {
            return new FailureResponse().setError("error", "History ID is required");
        }
        try {
            BuildHistory history = buildAuditService.getHistoryDetail(historyId);
            if (history == null) {
                return new FailureResponse().setError("error", "Record not found");
            }

            boolean verified = buildAuditService.verifyIntegrity(historyId);

            Map<String, Object> result = new HashMap<>();
            result.put("history", history);
            result.put("integrityVerified", verified);
            return new SuccessResponse(result).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    /**
     * Retrieves raw decompressed console logs for a specific build history.
     * @param historyId history ID
     * @return log text response
     */
    @RequestToGet("/log/${historyId}")
    public RestResponse getHistoryLog(Long historyId) {
        if (historyId == null) {
            return new FailureResponse().setError("error", "History ID is required");
        }
        try {
            String logs = buildAuditService.getDecompressedLogs(historyId);
            return new SuccessResponse(logs).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    /**
     * Exports build history records as a CSV audit file.
     * @param translet current translet
     */
    @RequestToGet("/export")
    public void exportCsv(@NonNull Translet translet) throws Exception {
        String targetNodeId = translet.getParameter("nodeId");
        String status = translet.getParameter("status");
        String searchKeyword = translet.getParameter("q");
        String startDate = translet.getParameter("startDate");
        String endDate = translet.getParameter("endDate");

        BuildAuditQuery query = new BuildAuditQuery();
        query.setTargetNodeId(targetNodeId);
        query.setStatus(status);
        query.setSearchKeyword(searchKeyword);
        query.setStartDate(startDate);
        query.setEndDate(endDate);

        String csvData = buildAuditService.exportCsvReport(query);

        var response = translet.getResponseAdapter();
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"aspectow_build_audit_report.csv\"");
        response.getOutputStream().write(csvData.getBytes(StandardCharsets.UTF_8));
    }

}

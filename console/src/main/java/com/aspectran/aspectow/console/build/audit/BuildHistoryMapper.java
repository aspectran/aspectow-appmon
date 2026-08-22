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

import com.aspectran.aspectow.console.common.db.tx.ConsoleSqlMapperProvider;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlMapperAccess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis Mapper interface for asc_build_history and asc_build_log.
 *
 * <p>Created: 2026-08-18</p>
 */
@Mapper
public interface BuildHistoryMapper {

    /**
     * Inserts a new build history master record.
     * @param history the build history entity
     * @return number of rows affected
     */
    int insertBuildHistory(BuildHistory history);

    /**
     * Updates an existing build history master record upon completion.
     * @param history the build history entity
     * @return number of rows affected
     */
    int updateBuildHistory(BuildHistory history);

    /**
     * Retrieves a single build history master record by its PK.
     * @param historyId the history ID
     * @return the build history entity, or null if not found
     */
    BuildHistory getBuildHistoryById(@Param("historyId") Long historyId);

    /**
     * Retrieves a single build history master record by its execution ID.
     * @param executionId the unique execution ID
     * @return the build history entity, or null if not found
     */
    BuildHistory getBuildHistoryByExecutionId(@Param("executionId") String executionId);

    /**
     * Retrieves the latest build history record for a specific target node ID.
     * @param targetNodeId the target node ID
     * @return the latest build history entity, or null if not found
     */
    BuildHistory getLatestBuildHistoryByNodeId(@Param("targetNodeId") String targetNodeId);

    /**
     * Retrieves the latest build history record for each specified target node.
     * @param targetNodeIds the collection of target node IDs
     * @return list of latest build history entities per node
     */
    List<BuildHistory> getLatestBuildHistories(@Param("targetNodeIds") Collection<String> targetNodeIds);

    /**
     * Searches build history records matching the given criteria with pagination.
     * @param query the search and pagination criteria
     * @return list of matching build history records
     */
    List<BuildHistory> searchBuildHistory(BuildAuditQuery query);

    /**
     * Counts total build history records matching the given criteria.
     * @param query the search criteria
     * @return total matching count
     */
    long countBuildHistory(BuildAuditQuery query);

    /**
     * Inserts console logs for a build history.
     * @param buildLog the build log entity
     * @return number of rows affected
     */
    int insertBuildLog(BuildLog buildLog);

    /**
     * Retrieves the build log entity for a given history ID.
     * @param historyId the history ID
     * @return the build log entity, or null if not found
     */
    BuildLog getBuildLogByHistoryId(@Param("historyId") Long historyId);

    /**
     * Retrieves the build log entity for a given execution ID.
     * @param executionId the execution ID
     * @return the build log entity, or null if not found
     */
    BuildLog getBuildLogByExecutionId(@Param("executionId") String executionId);

    /**
     * Deletes build history records older than the specified retention days.
     * @param retentionDays retention threshold in days
     * @return number of rows deleted
     */
    int purgeOldHistories(@Param("retentionDays") int retentionDays);

    /**
     * Data Access Object (DAO) for {@link BuildHistoryMapper}.
     * Provides a convenient way to access the mapper methods using Aspectran's bean container.
     */
    @Component
    @Bean("console.buildHistoryDao")
    class Dao extends SqlMapperAccess<BuildHistoryMapper> implements BuildHistoryMapper {

        /**
         * Constructs a new Dao.
         * @param sqlMapperProvider the SQL mapper provider
         */
        @Autowired
        public Dao(ConsoleSqlMapperProvider sqlMapperProvider) {
            super(sqlMapperProvider);
        }

        @Override
        public int insertBuildHistory(BuildHistory history) {
            return mapper().insertBuildHistory(history);
        }

        @Override
        public int updateBuildHistory(BuildHistory history) {
            return mapper().updateBuildHistory(history);
        }

        @Override
        public BuildHistory getBuildHistoryById(Long historyId) {
            return mapper().getBuildHistoryById(historyId);
        }

        @Override
        public BuildHistory getBuildHistoryByExecutionId(String executionId) {
            return mapper().getBuildHistoryByExecutionId(executionId);
        }

        @Override
        public BuildHistory getLatestBuildHistoryByNodeId(String targetNodeId) {
            return mapper().getLatestBuildHistoryByNodeId(targetNodeId);
        }

        @Override
        public List<BuildHistory> getLatestBuildHistories(Collection<String> targetNodeIds) {
            return mapper().getLatestBuildHistories(targetNodeIds);
        }

        @Override
        public List<BuildHistory> searchBuildHistory(BuildAuditQuery query) {
            return mapper().searchBuildHistory(query);
        }

        @Override
        public long countBuildHistory(BuildAuditQuery query) {
            return mapper().countBuildHistory(query);
        }

        @Override
        public int insertBuildLog(BuildLog buildLog) {
            return mapper().insertBuildLog(buildLog);
        }

        @Override
        public BuildLog getBuildLogByHistoryId(Long historyId) {
            return mapper().getBuildLogByHistoryId(historyId);
        }

        @Override
        public BuildLog getBuildLogByExecutionId(String executionId) {
            return mapper().getBuildLogByExecutionId(executionId);
        }

        @Override
        public int purgeOldHistories(int retentionDays) {
            return mapper().purgeOldHistories(retentionDays);
        }
    }

}

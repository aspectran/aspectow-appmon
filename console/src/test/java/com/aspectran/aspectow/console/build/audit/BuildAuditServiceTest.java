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
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildAuditServiceTest {

    @Test
    void testIntegrityHashConsistency() throws Exception {
        BuildAuditService service = new BuildAuditService(null);

        BuildHistory history = new BuildHistory();
        history.setExecutionId("bld_test12345");
        history.setTargetNodeId("node-1");
        history.setScriptName("1-pull.sh");
        history.setRequester("admin");
        history.setStatus("SUCCESS");
        history.setExitCode(0);
        history.setDurationMs(1500L);
        history.setGitCommitBefore("commit_before_123");
        history.setGitCommitAfter("commit_after_456");

        List<String> logLines = new ArrayList<>();
        logLines.add("[BUILD STARTED] Script: 1-pull.sh");
        logLines.add("Development environment detected.");
        logLines.add("Skipping git pull in development mode.");
        logLines.add("[BUILD SUCCESS] Process finished with exit code 0");

        String rawLogContent = String.join("\n", logLines);

        Method calculateMethod = BuildAuditService.class.getDeclaredMethod("calculateIntegrityHash", BuildHistory.class, String.class);
        calculateMethod.setAccessible(true);

        String hash1 = (String) calculateMethod.invoke(service, history, rawLogContent);
        assertNotNull(hash1);
        assertFalse(hash1.isEmpty());

        BuildHistory dbHistory = new BuildHistory();
        dbHistory.setHistoryId(1L);
        dbHistory.setExecutionId("bld_test12345");
        dbHistory.setTargetNodeId("node-1");
        dbHistory.setScriptName("1-pull.sh");
        dbHistory.setRequester("admin");
        dbHistory.setStatus("SUCCESS");
        dbHistory.setExitCode(0);
        dbHistory.setDurationMs(1500L);
        dbHistory.setGitCommitBefore("commit_before_123");
        dbHistory.setGitCommitAfter("commit_after_456");
        dbHistory.setIntegrityHash(hash1);

        String dbRawLogs = rawLogContent;
        String hash2 = (String) calculateMethod.invoke(service, dbHistory, dbRawLogs);

        assertEquals(hash1, hash2);
        assertTrue(dbHistory.getIntegrityHash().equalsIgnoreCase(hash2));
    }

    @Test
    void testGzipCompressionDecompressionIntegrity() throws Exception {
        BuildAuditService service = new BuildAuditService(null);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("Log line ").append(i).append(": Build step progressing smoothly...\n");
        }
        String largeLog = sb.toString();

        Method compressMethod = BuildAuditService.class.getDeclaredMethod("compressGzip", String.class);
        compressMethod.setAccessible(true);
        String compressed = (String) compressMethod.invoke(service, largeLog);
        assertNotNull(compressed);

        Method decompressMethod = BuildAuditService.class.getDeclaredMethod("decompressGzip", String.class);
        decompressMethod.setAccessible(true);
        String decompressed = (String) decompressMethod.invoke(service, compressed);

        assertEquals(largeLog, decompressed);
    }

    @Test
    void testNullFieldsIntegrityHash() throws Exception {
        BuildAuditService service = new BuildAuditService(null);

        BuildHistory history = new BuildHistory();
        history.setExecutionId("bld_test_nulls");
        history.setTargetNodeId("node-1");
        history.setScriptName("1-pull.sh");
        history.setRequester("SYSTEM");
        history.setStatus("SUCCESS");
        history.setExitCode(0);
        // gitCommitBefore and gitCommitAfter are null
        history.setGitCommitBefore(null);
        history.setGitCommitAfter(null);
        history.setDurationMs(null);

        Method calculateMethod = BuildAuditService.class.getDeclaredMethod("calculateIntegrityHash", BuildHistory.class, String.class);
        calculateMethod.setAccessible(true);

        String hash1 = (String) calculateMethod.invoke(service, history, "");
        assertNotNull(hash1);

        BuildHistory dbHistory = new BuildHistory();
        dbHistory.setExecutionId("bld_test_nulls");
        dbHistory.setTargetNodeId("node-1");
        dbHistory.setScriptName("1-pull.sh");
        dbHistory.setRequester("SYSTEM");
        dbHistory.setStatus("SUCCESS");
        dbHistory.setExitCode(0);
        dbHistory.setGitCommitBefore(null);
        dbHistory.setGitCommitAfter(null);
        dbHistory.setDurationMs(null);

        String hash2 = (String) calculateMethod.invoke(service, dbHistory, null);
        assertEquals(hash1, hash2);
    }

    @Test
    void testFullAuditLifecycleWithH2() throws Exception {
        PooledDataSource dataSource =
                new PooledDataSource(
                        "org.h2.Driver",
                        "jdbc:h2:mem:audit_test_db;DB_CLOSE_DELAY=-1;MODE=MariaDB",
                        "sa",
                        ""
                );

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("create table asc_build_history (" +
                    "history_id bigint not null auto_increment," +
                    "execution_id varchar(64) not null," +
                    "target_node_id varchar(100) not null," +
                    "script_name varchar(100) not null," +
                    "requester varchar(50) default 'SYSTEM' not null," +
                    "status varchar(20) default 'PENDING' not null," +
                    "exit_code int," +
                    "started_at timestamp default current_timestamp not null," +
                    "finished_at timestamp," +
                    "duration_ms bigint," +
                    "git_branch varchar(100)," +
                    "git_commit_before varchar(64)," +
                    "git_commit_after varchar(64)," +
                    "git_commit_msg varchar(500)," +
                    "integrity_hash varchar(64)," +
                    "error_summary varchar(1000)," +
                    "created_at timestamp default current_timestamp not null," +
                    "primary key (history_id)" +
                    ");");
            stmt.execute("create table asc_build_log (" +
                    "log_id bigint not null auto_increment," +
                    "history_id bigint not null," +
                    "execution_id varchar(64) not null," +
                    "log_content clob," +
                    "compressed_yn char(1) default 'N' not null," +
                    "line_count int default 0 not null," +
                    "byte_size bigint default 0 not null," +
                    "created_at timestamp default current_timestamp not null," +
                    "primary key (log_id)," +
                    "foreign key (history_id) references asc_build_history(history_id) on delete cascade" +
                    ");");
        }

        Environment env = new Environment(
                "test",
                new JdbcTransactionFactory(),
                dataSource
        );
        Configuration config = new Configuration(env);
        config.setMapUnderscoreToCamelCase(true);
        config.addMapper(BuildHistoryMapper.class);

        // Load XML mapper
        try (InputStream is = getClass().getResourceAsStream("/com/aspectran/aspectow/console/config/db/mapper/BuildHistoryMapper.xml")) {
            XMLMapperBuilder xmlMapperBuilder =
                    new XMLMapperBuilder(is, config, "BuildHistoryMapper.xml", config.getSqlFragments());
            xmlMapperBuilder.parse();
        }

        SqlSessionFactory sqlSessionFactory =
                new SqlSessionFactoryBuilder().build(config);

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            BuildHistoryMapper mapper = session.getMapper(BuildHistoryMapper.class);

            // 1. startAudit simulation
            BuildHistory history = new BuildHistory();
            history.setExecutionId("bld_exec_001");
            history.setTargetNodeId("node-1");
            history.setScriptName("1-pull.sh");
            history.setRequester("admin");
            history.setStatus("RUNNING");
            history.setStartedAt(java.time.Instant.now());
            mapper.insertBuildHistory(history);
            assertNotNull(history.getHistoryId());
            long historyId = history.getHistoryId();

            // 2. updateStatus simulation
            history.setGitBranch("main");
            history.setGitCommitBefore("commit_before_abc");
            mapper.updateBuildHistory(history);

            // 3. completeAudit simulation
            BuildExecutionInfo info = new BuildExecutionInfo();
            info.setExecutionId("bld_exec_001");
            info.setTargetNodeId("node-1");
            info.setScriptName("1-pull.sh");
            info.setStatus(BuildExecutionInfo.Status.SUCCESS);
            info.setExitCode(0);
            info.setStartedAt(history.getStartedAt());
            info.setFinishedAt(java.time.Instant.now());
            info.setDurationMs(1200L);
            info.setGitBranch("main");
            info.setGitCommitBefore("commit_before_abc");
            info.setGitCommitAfter("commit_after_xyz");
            info.setGitCommitMsg("Update documentation");

            List<String> logLines = List.of(
                    "[BUILD STARTED] Script: 1-pull.sh, Time: " + info.getStartedAt(),
                    "Development environment detected.",
                    "Skipping git pull in development mode.",
                    "[BUILD SUCCESS] Process finished with exit code 0"
            );
            String rawLogContent = String.join("\n", logLines);

            BuildHistory loaded = mapper.getBuildHistoryByExecutionIdAndNodeId(info.getExecutionId(), info.getTargetNodeId());
            assertNotNull(loaded);
            loaded.setStatus(info.getStatus().name());
            loaded.setExitCode(info.getExitCode());
            loaded.setFinishedAt(info.getFinishedAt());
            loaded.setDurationMs(info.getDurationMs());
            loaded.setGitBranch(info.getGitBranch());
            loaded.setGitCommitBefore(info.getGitCommitBefore());
            loaded.setGitCommitAfter(info.getGitCommitAfter());
            loaded.setGitCommitMsg(info.getGitCommitMsg());

            BuildAuditService service = new BuildAuditService(mapper);
            Method calculateMethod = BuildAuditService.class.getDeclaredMethod("calculateIntegrityHash", BuildHistory.class, String.class);
            calculateMethod.setAccessible(true);
            String integrityHash = (String) calculateMethod.invoke(service, loaded, rawLogContent);

            loaded.setIntegrityHash(integrityHash);
            mapper.updateBuildHistory(loaded);

            BuildLog buildLog = new BuildLog();
            buildLog.setHistoryId(loaded.getHistoryId());
            buildLog.setExecutionId(loaded.getExecutionId());
            buildLog.setLogContent(rawLogContent);
            buildLog.setCompressedYn("N");
            buildLog.setLineCount(logLines.size());
            buildLog.setByteSize((long) rawLogContent.getBytes(StandardCharsets.UTF_8).length);
            mapper.insertBuildLog(buildLog);

            // 4. verifyIntegrity simulation
            BuildHistory verifiedHistory = mapper.getBuildHistoryById(historyId);
            assertNotNull(verifiedHistory);
            assertNotNull(verifiedHistory.getIntegrityHash());

            BuildLog fetchedLog = mapper.getBuildLogByHistoryId(historyId);
            assertNotNull(fetchedLog);
            assertEquals(rawLogContent, fetchedLog.getLogContent());

            String recalculatedHash = (String) calculateMethod.invoke(service, verifiedHistory, fetchedLog.getLogContent());
            System.out.println("Stored Hash:       " + verifiedHistory.getIntegrityHash());
            System.out.println("Recalculated Hash: " + recalculatedHash);

            assertEquals(verifiedHistory.getIntegrityHash(), recalculatedHash);
        }
    }
}

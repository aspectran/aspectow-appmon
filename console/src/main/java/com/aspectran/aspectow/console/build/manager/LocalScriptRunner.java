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

import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.aware.ActivityContextAware;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * LocalScriptRunner executes build/deployment scripts residing in BASE_DIR.
 *
 * <p>Created: 2026-08-18</p>
 */
@Component
public class LocalScriptRunner implements ActivityContextAware {

    private static final Logger logger = LoggerFactory.getLogger(LocalScriptRunner.class);

    private static final Set<String> ALLOWED_SCRIPTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "1-pull.sh", "1-pull.bat",
            "2-build.sh", "2-build.bat",
            "3-deploy_config.sh", "3-deploy_config.bat",
            "4-deploy_webapps.sh", "4-deploy_webapps.bat",
            "5-pull_build_deploy.sh", "5-pull_build_deploy.bat",
            "6-pull_deploy.sh", "6-pull_deploy.bat",
            "7-pull_deploy_config_only.sh", "7-pull_deploy_config_only.bat",
            "8-pull_deploy_webapps_only.sh", "8-pull_deploy_webapps_only.bat",
            "9-pull_deploy_config_webapps_only.sh", "9-pull_deploy_config_webapps_only.bat",
            "daemon.sh", "daemon.bat",
            "service.sh"
    )));

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private ActivityContext activityContext;

    private final ReentrantLock buildLock = new ReentrantLock();

    private final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();

    private final Map<String, List<String>> logRingBuffers = new ConcurrentHashMap<>();

    private static final int MAX_LOG_BUFFER_SIZE = 10000;

    @Override
    public void setActivityContext(@NonNull ActivityContext activityContext) {
        this.activityContext = activityContext;
    }

    public File getBaseDir() {
        File dir = null;
        if (activityContext != null && activityContext.getApplicationAdapter() != null) {
            java.nio.file.Path basePath = activityContext.getApplicationAdapter().getBasePath();
            if (basePath != null) {
                dir = basePath.toFile();
            }
        }
        if (dir == null) {
            String sysBasePath = System.getProperty("aspectran.basePath");
            if (sysBasePath != null) {
                dir = new File(sysBasePath);
            } else {
                dir = new File(".");
            }
        }
        // The deployment scripts and daemon.sh reside in the BASE_DIR, which is the parent of the 'app' directory
        if ("app".equals(dir.getName()) && dir.getParentFile() != null) {
            dir = dir.getParentFile();
        }
        return dir;
    }

    /**
     * Resolves the script file by checking BASE_DIR and fallback locations.
     * @param scriptName the name of the script
     * @return the resolved script File, or null if not found
     */
    public File resolveScriptFile(String scriptName) {
        File baseDir = getBaseDir();
        File scriptFile = new File(baseDir, scriptName);
        if (scriptFile.exists()) {
            return scriptFile;
        }
        if (baseDir.getParentFile() != null) {
            File parentScript = new File(baseDir.getParentFile(), scriptName);
            if (parentScript.exists()) {
                return parentScript;
            }
        }
        return scriptFile;
    }

    public Set<String> getAllowedScripts() {
        return ALLOWED_SCRIPTS;
    }

    public boolean isBusy() {
        return buildLock.isLocked();
    }

    /**
     * Checks if a script is allowed to be executed.
     * @param scriptName the script file name
     * @return true if allowed
     */
    public boolean isScriptAllowed(String scriptName) {
        if (StringUtils.isEmpty(scriptName)) {
            return false;
        }
        String simpleName = new File(scriptName).getName();
        return ALLOWED_SCRIPTS.contains(simpleName);
    }

    /**
     * Retrieves the recent log buffer for an execution.
     * @param executionId the execution ID
     * @return list of log lines
     */
    public List<String> getLogBuffer(String executionId) {
        List<String> logs = logRingBuffers.get(executionId);
        return logs != null ? new ArrayList<>(logs) : Collections.emptyList();
    }

    /**
     * Cancels a currently running execution.
     * @param executionId the execution ID
     * @return true if cancelled successfully
     */
    public boolean cancel(String executionId) {
        Process process = activeProcesses.get(executionId);
        if (process != null && process.isAlive()) {
            logger.info("Cancelling build execution: {}", executionId);
            destroyProcessTree(process);
            return true;
        }
        return false;
    }

    private void destroyProcessTree(Process process) {
        try {
            process.descendants().forEach(ph -> {
                try {
                    ph.destroy();
                } catch (Exception ignored) {
                }
            });
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        } catch (Exception e) {
            logger.warn("Error while killing process tree", e);
            process.destroyForcibly();
        }
    }

    /**
     * Runs a script asynchronously.
     * @param info the execution metadata
     * @param logConsumer callback for real-time log lines
     * @param completionCallback callback called when execution completes
     */
    public void runAsync(BuildExecutionInfo info,
                         Consumer<String> logConsumer,
                         Consumer<BuildExecutionInfo> completionCallback) {
        Thread.ofVirtual().start(() -> {
            run(info, logConsumer, completionCallback);
        });
    }

    /**
     * Executes the script synchronously on the current thread.
     */
    public void run(BuildExecutionInfo info,
                    Consumer<String> logConsumer,
                    Consumer<BuildExecutionInfo> completionCallback) {
        String scriptName = info.getScriptName();
        if (!isScriptAllowed(scriptName)) {
            info.setStatus(BuildExecutionInfo.Status.FAILED);
            info.setErrorSummary("Script not allowed: " + scriptName);
            info.setFinishedAt(Instant.now());
            if (completionCallback != null) {
                completionCallback.accept(info);
            }
            return;
        }

        if (!buildLock.tryLock()) {
            info.setStatus(BuildExecutionInfo.Status.FAILED);
            info.setErrorSummary("Another build or deployment is already running on this node");
            info.setFinishedAt(Instant.now());
            if (completionCallback != null) {
                completionCallback.accept(info);
            }
            return;
        }

        File scriptFile = resolveScriptFile(scriptName);
        File baseDir = (scriptFile.getParentFile() != null ? scriptFile.getParentFile() : getBaseDir());
        if (!scriptFile.exists()) {
            buildLock.unlock();
            info.setStatus(BuildExecutionInfo.Status.FAILED);
            info.setErrorSummary("Script file not found: " + scriptFile.getAbsolutePath());
            info.setFinishedAt(Instant.now());
            if (completionCallback != null) {
                completionCallback.accept(info);
            }
            return;
        }

        // Special Handling for Daemon / Service Restart (Detached Execution)
        if ("daemon.sh".equals(scriptName) || "service.sh".equals(scriptName)) {
            String action = (info.getParameters() != null && info.getParameters().get("action") != null)
                    ? String.valueOf(info.getParameters().get("action"))
                    : "restart";

            info.setStatus(BuildExecutionInfo.Status.RUNNING);
            info.setStartedAt(Instant.now());

            List<String> logBuffer = Collections.synchronizedList(new ArrayList<>());
            logRingBuffers.put(info.getExecutionId(), logBuffer);

            DetachedRestartRunner restartRunner = new DetachedRestartRunner();
            boolean success = restartRunner.executeDetached(baseDir, scriptName, action, line -> {
                appendLog(info.getExecutionId(), logBuffer, line, logConsumer);
            });

            buildLock.unlock();

            info.setStatus(success ? BuildExecutionInfo.Status.SUCCESS : BuildExecutionInfo.Status.FAILED);
            info.setExitCode(success ? 0 : 1);
            info.setFinishedAt(Instant.now());
            info.setDurationMs(Duration.between(info.getStartedAt(), info.getFinishedAt()).toMillis());
            if (completionCallback != null) {
                completionCallback.accept(info);
            }
            return;
        }

        List<String> logBuffer = Collections.synchronizedList(new ArrayList<>());
        logRingBuffers.put(info.getExecutionId(), logBuffer);

        info.setStatus(BuildExecutionInfo.Status.RUNNING);
        info.setStartedAt(Instant.now());

        // Capture Git information before execution
        captureGitInfoBefore(baseDir, info);

        Process process = null;
        try {
            List<String> command = buildCommandLine(scriptFile, info.getParameters());
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(baseDir);
            pb.redirectErrorStream(true);

            // Optional environment setup
            Map<String, String> env = pb.environment();
            env.put("BASE_DIR", baseDir.getAbsolutePath());
            if (info.getParameters() != null) {
                for (Map.Entry<String, Object> entry : info.getParameters().entrySet()) {
                    if (entry.getValue() != null) {
                        env.put("PARAM_" + entry.getKey().toUpperCase(), entry.getValue().toString());
                    }
                }
            }

            process = pb.start();
            activeProcesses.put(info.getExecutionId(), process);

            String startMsg = String.format("[BUILD STARTED] Script: %s, Time: %s",
                    scriptName, info.getStartedAt());
            appendLog(info.getExecutionId(), logBuffer, startMsg, logConsumer);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog(info.getExecutionId(), logBuffer, line, logConsumer);
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            info.setFinishedAt(Instant.now());
            info.setDurationMs(Duration.between(info.getStartedAt(), info.getFinishedAt()).toMillis());

            if (!finished) {
                destroyProcessTree(process);
                info.setStatus(BuildExecutionInfo.Status.TIMEOUT);
                info.setErrorSummary("Execution timed out after 30 minutes");
                appendLog(info.getExecutionId(), logBuffer, "[BUILD TIMEOUT] Execution timed out", logConsumer);
            } else {
                int exitCode = process.exitValue();
                info.setExitCode(exitCode);
                if (exitCode == 0) {
                    info.setStatus(BuildExecutionInfo.Status.SUCCESS);
                    appendLog(info.getExecutionId(), logBuffer,
                            "[BUILD SUCCESS] Process finished with exit code 0", logConsumer);
                } else {
                    info.setStatus(BuildExecutionInfo.Status.FAILED);
                    info.setErrorSummary("Process exited with non-zero code: " + exitCode);
                    appendLog(info.getExecutionId(), logBuffer,
                            "[BUILD FAILED] Process exited with code " + exitCode, logConsumer);
                }
            }

            // Capture Git information after execution
            captureGitInfoAfter(baseDir, info);

        } catch (InterruptedException e) {
            info.setFinishedAt(Instant.now());
            info.setDurationMs(Duration.between(info.getStartedAt(), info.getFinishedAt()).toMillis());
            info.setStatus(BuildExecutionInfo.Status.CANCELLED);
            info.setErrorSummary("Execution cancelled by user");
            appendLog(info.getExecutionId(), logBuffer, "[BUILD CANCELLED] Execution interrupted", logConsumer);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error executing script {}", scriptName, e);
            info.setFinishedAt(Instant.now());
            info.setStatus(BuildExecutionInfo.Status.FAILED);
            info.setErrorSummary(e.getMessage());
            appendLog(info.getExecutionId(), logBuffer, "[BUILD ERROR] " + e.getMessage(), logConsumer);
        } finally {
            activeProcesses.remove(info.getExecutionId());
            buildLock.unlock();
            if (completionCallback != null) {
                completionCallback.accept(info);
            }
        }
    }

    private void appendLog(String executionId, List<String> buffer, String line, Consumer<String> consumer) {
        if (buffer.size() >= MAX_LOG_BUFFER_SIZE) {
            buffer.remove(0);
        }
        buffer.add(line);
        if (consumer != null) {
            try {
                consumer.accept(line);
            } catch (Exception e) {
                logger.trace("Error invoking log consumer: {}", e.getMessage());
            }
        }
    }

    private List<String> buildCommandLine(File scriptFile, Map<String, Object> params) {
        List<String> command = new ArrayList<>();
        if (IS_WINDOWS) {
            command.add("cmd.exe");
            command.add("/c");
            command.add(scriptFile.getAbsolutePath());
        } else {
            command.add("sh");
            command.add(scriptFile.getAbsolutePath());
        }

        if (params != null && !params.isEmpty()) {
            if (params.containsKey("branch")) {
                command.add(params.get("branch").toString());
            }
        }
        return command;
    }

    private void captureGitInfoBefore(File baseDir, BuildExecutionInfo info) {
        File gitDir = resolveGitDir(baseDir);
        if (gitDir != null) {
            info.setGitCommitBefore(runGitCommand(gitDir, "rev-parse", "HEAD"));
            info.setGitBranch(runGitCommand(gitDir, "branch", "--show-current"));
        }
    }

    private void captureGitInfoAfter(File baseDir, BuildExecutionInfo info) {
        File gitDir = resolveGitDir(baseDir);
        if (gitDir != null) {
            info.setGitCommitAfter(runGitCommand(gitDir, "rev-parse", "HEAD"));
            info.setGitCommitMsg(runGitCommand(gitDir, "log", "-1", "--pretty=format:%s"));
        }
    }

    private File resolveGitDir(File baseDir) {
        File dotBuild = new File(baseDir, ".build");
        if (dotBuild.isDirectory()) {
            File[] files = dotBuild.listFiles(File::isDirectory);
            if (files != null && files.length > 0) {
                for (File dir : files) {
                    if (new File(dir, ".git").exists()) {
                        return dir;
                    }
                }
            }
        }
        if (new File(baseDir, ".git").exists()) {
            return baseDir;
        }
        return null;
    }

    private String runGitCommand(File workingDir, String... args) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("git");
            cmd.addAll(Arrays.asList(args));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workingDir);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = r.readLine();
                p.waitFor(3, TimeUnit.SECONDS);
                return (line != null ? line.trim() : null);
            }
        } catch (Exception e) {
            logger.debug("Failed to run git command {}: {}", Arrays.toString(args), e.getMessage());
            return null;
        }
    }

}

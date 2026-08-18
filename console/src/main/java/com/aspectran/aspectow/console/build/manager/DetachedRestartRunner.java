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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * DetachedRestartRunner safely spawns server restart scripts (daemon.sh / service.sh)
 * into a completely detached OS session/process group so that the child process
 * survives the parent JVM termination.
 *
 * <p>Created: 2026-08-18</p>
 */
public class DetachedRestartRunner {

    private static final Logger logger = LoggerFactory.getLogger(DetachedRestartRunner.class);

    private static final boolean IS_WINDOWS = System.getProperty("os.name")
            .toLowerCase(Locale.ROOT).contains("win");

    private static final File NULL_FILE = new File(IS_WINDOWS ? "NUL" : "/dev/null");

    /**
     * Executes a detached daemon control command (e.g. restart, stop).
     * @param baseDir the node base directory where daemon.sh / service.sh resides
     * @param scriptName the script name (daemon.sh or service.sh)
     * @param action the action argument (restart, stop, start)
     * @param logConsumer consumer for logging output
     * @return true if successfully spawned; false otherwise
     */
    public boolean executeDetached(@NonNull File baseDir,
                                   @NonNull String scriptName,
                                   String action,
                                   Consumer<String> logConsumer) {
        String targetAction = (action != null && !action.isEmpty()) ? action : "restart";
        File scriptFile = new File(baseDir, scriptName);

        if (!scriptFile.exists()) {
            String errorMsg = "[ERROR] Script file not found in BASE_DIR: " + scriptFile.getAbsolutePath();
            logger.error(errorMsg);
            if (logConsumer != null) {
                logConsumer.accept(errorMsg);
            }
            return false;
        }

        if (logConsumer != null) {
            logConsumer.accept(String.format("[DETACHED] Spawning detached restart process: %s %s in %s",
                    scriptName, targetAction, baseDir.getAbsolutePath()));
            logConsumer.accept("[DETACHED] Parent JVM will terminate shortly. Server will restart in the background...");
        }

        try {
            List<String> command = buildDetachedCommand(scriptFile, targetAction);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(baseDir);

            // Redirect stdout/stderr to DISCARD and stdin to null device
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.redirectInput(ProcessBuilder.Redirect.from(NULL_FILE));

            Process process = pb.start();
            logger.info("Detached restart command successfully initiated with PID: {}", process.pid());

            if (logConsumer != null) {
                logConsumer.accept("[DETACHED] Process spawned with PID " + process.pid() + ". Awaiting node recovery...");
            }
            return true;
        } catch (IOException e) {
            logger.error("Failed to spawn detached restart process for script: {}", scriptName, e);
            if (logConsumer != null) {
                logConsumer.accept("[ERROR] Failed to spawn detached process: " + e.getMessage());
            }
            return false;
        }
    }

    @NonNull
    private List<String> buildDetachedCommand(@NonNull File scriptFile, @NonNull String action) {
        List<String> command = new ArrayList<>();
        if (IS_WINDOWS) {
            command.add("cmd.exe");
            command.add("/c");
            command.add("start");
            command.add("\"\"");
            command.add("/b");
            command.add(scriptFile.getAbsolutePath());
            command.add(action);
        } else {
            // Linux / macOS / Unix: Use nohup with a brief 1-second delay so that
            // the WebSocket notification has time to flush to the client before JVM shuts down.
            command.add("nohup");
            command.add("sh");
            command.add("-c");
            command.add(String.format("sleep 1 && exec \"%s\" %s",
                    scriptFile.getAbsolutePath(), action));
        }
        return command;
    }

}

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
package com.aspectran.aspectow.console.demo;

import com.aspectran.shell.jline.JLineAspectranShell;
import com.aspectran.utils.ResourceUtils;

import java.io.File;
import java.io.IOException;

import static com.aspectran.core.context.config.AspectranConfig.BASE_PATH_PROPERTY;
import static com.aspectran.core.context.config.AspectranConfig.TEMP_PATH_PROPERTY;
import static com.aspectran.core.context.config.AspectranConfig.WORK_PATH_PROPERTY;
import static com.aspectran.logging.LoggingDefaults.LOGS_DIR_PROPERTY;

/**
 * Main entry point for the application.
 */
public class AspectowConsoleDemo2 {

    public static void main(String[] args) {
        try {
            File root = new File(ResourceUtils.getResourceAsFile(""), "../../app");
            File tempDir = new File(root, "temp2");
            File workDir = new File(root, "work2");
            File logsDir = new File(root, "logs2");
            System.setProperty(BASE_PATH_PROPERTY, root.getCanonicalPath()); // for logging configuration
            System.setProperty(LOGS_DIR_PROPERTY, logsDir.getCanonicalPath()); // for logging configuration
            System.setProperty(TEMP_PATH_PROPERTY, tempDir.getCanonicalPath());
            System.setProperty(WORK_PATH_PROPERTY, workDir.getCanonicalPath());
            System.setProperty("tow.server.listener.http.port", "8092");
            System.setProperty("aspectow.console.config.db.h2.path", "~/aspectow-console2");
            JLineAspectranShell.main(new String[] { root.getCanonicalPath(), "config/aspectran-config.apon" });
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

}

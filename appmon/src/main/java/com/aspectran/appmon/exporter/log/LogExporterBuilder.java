/*
 * Copyright (c) 2020-2025 The Aspectran Project
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
package com.aspectran.appmon.exporter.log;

import com.aspectran.appmon.config.LogInfo;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public abstract class LogExporterBuilder {

    private static final Logger logger = LoggerFactory.getLogger(LogExporterBuilder.class);

    @NonNull
    public static void build(@NonNull LogExporterManager logExporterManager,
                             @NonNull List<LogInfo> logInfoList,
                             @NonNull ApplicationAdapter applicationAdapter) throws Exception {
        for (LogInfo logInfo : logInfoList) {
            if (logger.isDebugEnabled()) {
                logger.debug(ToStringBuilder.toString("Create LogExporter", logInfo));
            }

            logInfo.validateRequiredParameters();

            try {
                File logFile = applicationAdapter.getRealPath(logInfo.getFile()).toFile();
                LogExporter logExporter = new LogExporter(logExporterManager, logInfo, logFile);
                logExporterManager.addExporter(logExporter);
            } catch (Exception e) {
                throw new Exception(ToStringBuilder.toString("Failed to create log exporter", logInfo), e);
            }
        }
    }

}

/*
 * Copyright (c) 2020-present The Aspectran Project
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
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * A builder for creating {@link LogExporter} instances.
 * It resolves the log file path and constructs the exporter.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public abstract class LogExporterBuilder {

    private static final Logger logger = LoggerFactory.getLogger(LogExporterBuilder.class);

    /**
     * Builds a new {@link LogExporter}.
     * @param logExporterManager the exporter manager for logs
     * @param logInfo the log configuration
     * @return a new {@link LogExporter} instance
     * @throws Exception if the exporter cannot be built
     */
    @NonNull
    public static LogExporter build(@NonNull ExporterManager logExporterManager,
                                    @NonNull LogInfo logInfo) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(ToStringBuilder.toString("Create LogExporter", logInfo));
        }
        try {
            ApplicationAdapter applicationAdapter = logExporterManager.getAppMonManager().getApplicationAdapter();
            File logFile = applicationAdapter.getRealPath(logInfo.getFile()).toFile();
            return new LogExporter(logExporterManager, logInfo, logFile);
        } catch (Exception e) {
            throw new Exception(ToStringBuilder.toString("Failed to create log exporter", logInfo), e);
        }
    }

}

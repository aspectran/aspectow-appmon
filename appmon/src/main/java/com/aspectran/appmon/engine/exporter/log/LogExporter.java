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
package com.aspectran.appmon.engine.exporter.log;

import com.aspectran.appmon.engine.config.LogInfo;
import com.aspectran.appmon.engine.exporter.AbstractExporter;
import com.aspectran.appmon.engine.exporter.ExporterManager;
import com.aspectran.appmon.engine.exporter.ExporterType;
import com.aspectran.appmon.engine.service.CommandOptions;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An exporter that tails a log file and broadcasts new lines.
 * It uses Apache Commons IO's {@link Tailer} for efficient file monitoring.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class LogExporter extends AbstractExporter {

    private static final Logger logger = LoggerFactory.getLogger(LogExporter.class);

    private static final ExporterType TYPE = ExporterType.LOG;

    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    private static final int DEFAULT_SAMPLE_INTERVAL = 1000;

    private final ExporterManager exporterManager;

    private final LogInfo logInfo;

    private final String prefix;

    /** the Charset to be used for reading the file */
    private final Charset charset;

    /** how frequently to check for file changes; defaults to 1 second */
    private final int sampleInterval;

    private final int lastLines;

    /** the log file to tail */
    private final File logFile;

    private Tailer tailer;

    /**
     * Instantiates a new LogExporter.
     * @param exporterManager the exporter manager
     * @param logInfo the log configuration
     * @param logFile the log file to tail
     */
    public LogExporter(@NonNull ExporterManager exporterManager,
                       @NonNull LogInfo logInfo,
                       @NonNull File logFile) {
        super(TYPE);
        this.exporterManager = exporterManager;
        this.logInfo = logInfo;
        this.prefix = logInfo.getInstanceName() + ":" + TYPE + ":" + logInfo.getName() + ":";
        this.charset = (logInfo.getCharset() != null ? Charset.forName(logInfo.getCharset()): DEFAULT_CHARSET);
        this.sampleInterval = (logInfo.getSampleInterval() > 0 ? logInfo.getSampleInterval() : DEFAULT_SAMPLE_INTERVAL);
        this.lastLines = logInfo.getLastLines();
        this.logFile = logFile;
    }

    @Override
    public String getName() {
        return logInfo.getName();
    }

    @Override
    public void read(@NonNull List<String> messages, CommandOptions commandOptions) {
        if (lastLines > 0) {
            try {
                if (logFile.exists()) {
                    List<String> lines = readLastLines(logFile, lastLines);
                    if (!lines.isEmpty()) {
                        messages.addAll(lines);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to read log file {}", logFile, e);
            }
        }
    }

    @NonNull
    private List<String> readLastLines(File file, int lastLines) throws IOException {
        List<String> list = new ArrayList<>();
        try (ReversedLinesFileReader reversedLinesFileReader = ReversedLinesFileReader.builder()
                .setFile(file)
                .setCharset(charset)
                .get()) {
            int count = 0;
            while (count++ < lastLines) {
                String line = reversedLinesFileReader.readLine();
                if (line == null) {
                    break;
                }
                list.add(prefix + line);
            }
            Collections.reverse(list);
        }
        return list;
    }

    @Override
    public void broadcast(String message) {
        exporterManager.broadcast(prefix + message);
    }

    @Override
    protected void doStart() throws Exception {
        tailer = Tailer.builder()
                .setFile(logFile)
                .setTailerListener(new LogTailerListener(this))
                .setDelayDuration(Duration.ofMillis(sampleInterval))
                .setTailFromEnd(true)
                .get();
    }

    @Override
    protected void doStop() throws Exception {
        if (tailer != null) {
            tailer.close();
            tailer = null;
        }
    }

    @Override
    public String toString() {
        if (isStopped()) {
            return ToStringBuilder.toString(super.toString(), logInfo);
        } else {
            return super.toString();
        }
    }

}

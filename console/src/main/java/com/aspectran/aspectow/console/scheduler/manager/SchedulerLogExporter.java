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
package com.aspectran.aspectow.console.scheduler.manager;

import com.aspectran.aspectow.console.scheduler.bridge.SchedulerBroker;
import com.aspectran.utils.lifecycle.AbstractLifeCycle;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.input.Tailer;
import org.jspecify.annotations.NonNull;
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
 * SchedulerLogExporter tails a specific scheduler log file and broadcasts
 * new lines via the SchedulerBroker.
 */
public class SchedulerLogExporter extends AbstractLifeCycle {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerLogExporter.class);

    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    private final String loggingGroup;

    private final File logFile;

    private final SchedulerBroker broker;

    private final String prefix;

    private final Charset charset;

    private final long sampleInterval;

    private final int lastLines;

    private Tailer tailer;

    public SchedulerLogExporter(String loggingGroup, File logFile, SchedulerBroker broker) {
        this(loggingGroup, logFile, broker, DEFAULT_CHARSET.name());
    }

    public SchedulerLogExporter(String loggingGroup, File logFile, SchedulerBroker broker, String charsetName) {
        this(loggingGroup, logFile, broker, charsetName, 1000L, 1000);
    }

    public SchedulerLogExporter(
            String loggingGroup, File logFile, SchedulerBroker broker,
            String charsetName, long sampleInterval, int lastLines) {
        this.loggingGroup = loggingGroup;
        this.logFile = logFile;
        this.broker = broker;
        this.prefix = "scheduler:log:" + loggingGroup + ":";
        this.charset = (charsetName != null ? Charset.forName(charsetName) : DEFAULT_CHARSET);
        this.sampleInterval = sampleInterval;
        this.lastLines = lastLines;
    }

    public String getLoggingGroup() {
        return loggingGroup;
    }

    public File getLogFile() {
        return logFile;
    }

    /**
     * Reads the last N lines from the log file and adds them to the messages list.
     * @param messages the list to add log lines to
     */
    public void read(@NonNull List<String> messages) {
        if (lastLines > 0 && logFile.exists()) {
            try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder()
                    .setFile(logFile)
                    .setCharset(charset)
                    .get()) {
                List<String> lines = new ArrayList<>();
                String line;
                while (lines.size() < lastLines && (line = reader.readLine()) != null) {
                    lines.add(prefix + line);
                }
                Collections.reverse(lines);
                messages.addAll(lines);
            } catch (IOException e) {
                logger.error("Failed to read last lines from scheduler log file: {}", logFile, e);
            }
        }
    }

    public void broadcast(String message) {
        broker.bridge(prefix + message);
    }

    @Override
    protected void doStart() throws Exception {
        if (logFile.exists()) {
            tailer = Tailer.builder()
                    .setFile(logFile)
                    .setTailerListener(new SchedulerLogTailerListener(this))
                    .setDelayDuration(Duration.ofMillis(sampleInterval))
                    .setTailFromEnd(true)
                    .get();
            logger.info("Started tailing scheduler log file: {}", logFile.getAbsolutePath());
        } else {
            logger.warn("Scheduler log file does not exist: {}", logFile.getAbsolutePath());
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (tailer != null) {
            tailer.close();
            tailer = null;
            logger.info("Stopped tailing scheduler log file: {}", logFile.getAbsolutePath());
        }
    }

}

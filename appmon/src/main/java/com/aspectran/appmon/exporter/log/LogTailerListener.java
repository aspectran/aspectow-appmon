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

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

/**
 * A listener for {@link Tailer} events.
 * It forwards new log lines to the {@link LogExporter} to be broadcast.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class LogTailerListener implements TailerListener {

    private final LogExporter logExporter;

    /**
     * Instantiates a new LogTailerListener.
     * @param logExporter the exporter to which new log lines will be sent
     */
    public LogTailerListener(LogExporter logExporter) {
        this.logExporter = logExporter;
    }

    /**
     * This method is called if the tailed file is not found.
     */
    @Override
    public void init(Tailer tailer) {
        // Not used
    }

    /**
     * This method is called if the tailed file is not found.
     */
    @Override
    public void fileNotFound() {
        // Not used
    }

    /**
     * Called if a file rotation is detected.
     */
    @Override
    public void fileRotated() {
        // Not used
    }

    /**
     * Handles a new line from the tailed file.
     * @param line the new line
     */
    @Override
    public void handle(String line) {
        logExporter.broadcast(line);
    }

    /**
     * Handles an exception thrown by the Tailer.
     * @param e the exception
     */
    @Override
    public void handle(Exception e) {
        // Not used
    }

}

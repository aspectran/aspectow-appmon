/*
 * Copyright (c) 2008-2023 The Aspectran Project
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
package app.logrelay;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

public class LogTailerListener implements TailerListener {

    private final String tailerName;

    private final LogtailEndpoint endpoint;

    public LogTailerListener(String tailerName, LogtailEndpoint endpoint) {
        this.tailerName = tailerName;
        this.endpoint = endpoint;
    }

    public LogtailEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public void init(Tailer tailer) {
    }

    @Override
    public void fileNotFound() {
    }

    @Override
    public void fileRotated() {
    }

    @Override
    public void handle(String line) {
        endpoint.broadcast(tailerName + ":" + line);
    }

    @Override
    public void handle(Exception e) {
    }

}

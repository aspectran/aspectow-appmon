/*
 * Copyright (c) 2018-2025 The Aspectran Project
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
package com.aspectran.aspectow.appmon.exporter.state;

import com.aspectran.aspectow.appmon.config.StateInfo;
import com.aspectran.aspectow.appmon.exporter.Exporter;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.apon.Parameters;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class StateExporter extends Exporter {

    private static final String TYPE = ":state:";

    private final StateExporterManager stateExporterManager;

    private final StateInfo stateInfo;

    private final StateReader stateReader;

    private final String label;

    private final int sampleInterval;

    private Timer timer;

    public StateExporter(@NonNull StateExporterManager stateExporterManager,
                         @NonNull StateInfo stateInfo,
                         @NonNull StateReader stateReader) {
        this.stateExporterManager = stateExporterManager;
        this.stateInfo = stateInfo;
        this.stateReader = stateReader;
        this.label = stateInfo.getGroup() + TYPE + stateInfo.getName() + ":";
        this.sampleInterval = stateInfo.getSampleInterval();
    }

    @Override
    public String getName() {
        return stateInfo.getName();
    }

    @SuppressWarnings("unchecked")
    public <V extends Parameters> V getExporterInfo() {
        return (V) stateInfo;
    }

    @Override
    public void read(@NonNull List<String> messages) {
        String json = stateReader.read();
        if (json != null) {
            messages.add(label + json);
        }
    }

    @Override
    public void broadcast(String message) {
        stateExporterManager.broadcast(label + message);
    }

    private void broadcastIfChanged() {
        String data = stateReader.readIfChanged();
        if (data != null) {
            broadcast(data);
        }
    }

    @Override
    protected void doStart() throws Exception {
        stateReader.start();
        broadcastIfChanged();
        if (sampleInterval > 0 && timer == null) {
            String name = new ToStringBuilder("StatusReadingTimer")
                    .append("statusReader", stateReader)
                    .append("sampleInterval", sampleInterval)
                    .toString();
            timer = new Timer(name);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    broadcastIfChanged();
                }
            }, 0, sampleInterval);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        stateReader.stop();
    }

    @Override
    public String toString() {
        if (isStopped()) {
            return ToStringBuilder.toString(super.toString(), stateInfo);
        } else {
            return super.toString();
        }
    }

}

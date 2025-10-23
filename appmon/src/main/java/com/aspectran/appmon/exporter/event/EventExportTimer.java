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
package com.aspectran.appmon.exporter.event;

import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.scheduling.Scheduler;
import com.aspectran.utils.timer.CyclicTimeout;

import java.util.concurrent.TimeUnit;

/**
 * A timer for scheduling event exporting tasks.
 *
 * <p>Created: 2025-10-23</p>
 */
class EventExportTimer {

    private final Scheduler scheduler;

    private final EventExporter eventExporter;

    private final int sampleInterval;

    private CyclicTimeout samplingTimer;

    EventExportTimer(@NonNull ExporterManager exporterManager, @NonNull EventExporter eventExporter, int sampleInterval) {
        this.scheduler = exporterManager.getScheduler();
        this.eventExporter = eventExporter;
        this.sampleInterval = sampleInterval;
    }

    void schedule() {
        this.samplingTimer = new CyclicTimeout(scheduler) {
            @Override
            public void onTimeoutExpired() {
                eventExporter.broadcastIfChanged();
                scheduleSampling();
            }
        };
        scheduleSampling();
    }

    private void scheduleSampling() {
        samplingTimer.schedule(sampleInterval, TimeUnit.MILLISECONDS);
    }

    void destroy() {
        if (samplingTimer != null) {
            samplingTimer.cancel();
            samplingTimer.destroy();
        }
    }

}

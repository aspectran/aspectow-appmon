package com.aspectran.appmon.engine.exporter.metric;

import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.scheduling.Scheduler;
import com.aspectran.utils.timer.CyclicTimeout;

import java.util.concurrent.TimeUnit;

/**
 * A timer for scheduling metric sampling and exporting tasks.
 * This class manages the periodic collection and broadcasting of metric data.
 *
 * <p>Created: 2025-07-10</p>
 */
public class MetricExportTimer {

    private final Scheduler scheduler;

    private final MetricExporter metricExporter;

    private final MetricReader metricReader;

    private int sampleInterval;

    private int exportInterval;

    private CyclicTimeout samplingTimer;

    private CyclicTimeout exportTimer;

    private MetricData sampledMetricData;

    /**
     * Instantiates a new MetricExportTimer.
     * @param scheduler the scheduler to use for timing tasks
     * @param metricExporter the metric exporter to which data will be sent
     */
    public MetricExportTimer(Scheduler scheduler, @NonNull MetricExporter metricExporter) {
        this.scheduler = scheduler;
        this.metricExporter = metricExporter;
        this.metricReader = metricExporter.getMetricReader();
    }

    /**
     * Schedules the sampling and exporting of metric data.
     * @param sampleInterval the interval in milliseconds for sampling data
     * @param exportInterval the interval in milliseconds for exporting data
     */
    public void schedule(int sampleInterval, int exportInterval) {
        this.sampleInterval = sampleInterval;
        this.exportInterval = exportInterval;

        this.samplingTimer = new CyclicTimeout(scheduler) {
            @Override
            public void onTimeoutExpired() {
                saveMetricData();
                if (exportInterval <= sampleInterval) {
                    exportMetricData();
                }
                scheduleSampling();
            }
        };

        if (exportInterval > sampleInterval) {
            this.exportTimer = new CyclicTimeout(scheduler) {
                @Override
                public void onTimeoutExpired() {
                    exportMetricData();
                    scheduleExporting();
                }
            };
        } else {
            this.exportTimer = null;
        }

        scheduleSampling();
        scheduleExporting();
    }

    private void scheduleSampling() {
        samplingTimer.schedule(sampleInterval, TimeUnit.MILLISECONDS);
    }

    private void scheduleExporting() {
        if (exportTimer != null) {
            exportTimer.schedule(exportInterval, TimeUnit.MILLISECONDS);
        }
    }

    private void saveMetricData() {
        if (sampledMetricData == null) {
            if (metricReader.hasChanges()) {
                sampledMetricData = metricReader.getMetricData(false);
            }
        } else {
            MetricData metricData = metricReader.getMetricData(true);
            if (metricData != null) {
                sampledMetricData = metricData;
            }
        }
    }

    private void exportMetricData() {
        if (sampledMetricData != null) {
            metricExporter.broadcast(sampledMetricData.toJson());
            sampledMetricData = null;
        }
    }

    /**
     * Destroys the timer and cancels all scheduled tasks.
     */
    public void destroy() {
        if (samplingTimer != null) {
            samplingTimer.cancel();
            samplingTimer.destroy();
        }
        if (exportTimer != null) {
            exportTimer.cancel();
            exportTimer.destroy();
        }
    }

}

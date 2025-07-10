package com.aspectran.appmon.exporter;

import com.aspectran.appmon.exporter.metric.MetricData;
import com.aspectran.appmon.exporter.metric.MetricExporter;
import com.aspectran.appmon.exporter.metric.MetricReader;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.thread.Scheduler;
import com.aspectran.utils.timer.CyclicTimeout;

import java.util.concurrent.TimeUnit;

/**
 * <p>Created: 2025-07-10</p>
 */
public class MetricExportTimer {

    private final CyclicTimeout samplingTimer;

    private final CyclicTimeout exportTimer;

    private final MetricExporter metricExporter;

    private final MetricReader metricReader;

    private final int sampleInterval;

    private final int exportInterval;

    private MetricData metricData;

    public MetricExportTimer(
            Scheduler scheduler, @NonNull MetricExporter metricExporter,
            int sampleInterval, int exportInterval) {
        this.samplingTimer = new CyclicTimeout(scheduler) {
            @Override
            public void onTimeoutExpired() {
                saveMetricData();
                if (exportInterval <= sampleInterval) {
                    exportMetricData();
                }
                MetricExportTimer.this.schedule();
            }
        };
        if (exportInterval > sampleInterval) {
            this.exportTimer = new CyclicTimeout(scheduler) {
                @Override
                public void onTimeoutExpired() {
                    exportMetricData();
                    MetricExportTimer.this.schedule();
                }
            };
        } else {
            this.exportTimer = null;
        }
        this.metricExporter = metricExporter;
        this.metricReader = metricExporter.getMetricReader();
        this.sampleInterval = sampleInterval;
        this.exportInterval = exportInterval;
    }

    private void saveMetricData() {
        if (this.metricData == null) {
            this.metricData = metricReader.getMetricData();
        } else {
            this.metricData = metricReader.getMetricData(true);
        }
    }

    private void exportMetricData() {
        if (metricData != null) {
            metricExporter.broadcast(metricData.toJson());
            metricData = null;
        }
    }

    public void schedule() {
        samplingTimer.schedule(sampleInterval, TimeUnit.MILLISECONDS);
        if (exportTimer != null) {
            exportTimer.schedule(exportInterval, TimeUnit.MILLISECONDS);
        }
    }

    public void cancel() {
        if (samplingTimer != null) {
            samplingTimer.cancel();
        }
        if (exportTimer != null) {
            exportTimer.cancel();
        }
    }

    public void destroy() {
        if (samplingTimer != null) {
            samplingTimer.destroy();
        }
        if (exportTimer != null) {
            exportTimer.destroy();
        }
    }

}

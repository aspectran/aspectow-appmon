package com.aspectran.appmon.exporter.metric.undertow;

import com.aspectran.appmon.config.MetricInfo;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.appmon.exporter.metric.AbstractMetricReader;
import com.aspectran.appmon.exporter.metric.MetricData;
import com.aspectran.undertow.server.TowServer;
import com.aspectran.utils.annotation.jsr305.NonNull;
import io.undertow.Undertow;
import org.xnio.management.XnioWorkerMXBean;

/**
 * A {@link com.aspectran.appmon.exporter.metric.MetricReader} for monitoring Undertow's NIO worker threads.
 * <p>It requires specific JVM system properties to be enabled for statistics collection.</p>
 * <pre>
 *   -Djboss.threads.eqe.statistics=true
 *   -Djboss.threads.eqe.statistics.active-count=true
 * </pre>
 * <p>Or to set the system parameters in aspectran-config.apon:</p>
 * <pre>
 *   system: {
 *     properties: {
 *       jboss.threads.eqe.statistics: true
 *       jboss.threads.eqe.statistics.active-count: true
 *     }
 *   }
 * </pre>
 *
 * <p>Created: 2025-07-07</p>
 */
public class NioWorkerMetricsReader extends AbstractMetricReader {

    private String serverId;

    private XnioWorkerMXBean metrics;

    private int oldActive;

    /**
     * Instantiates a new NioWorkerMetricsReader.
     * @param exporterManager the exporter manager
     * @param metricInfo the metric configuration
     */
    public NioWorkerMetricsReader(
            @NonNull ExporterManager exporterManager,
            @NonNull MetricInfo metricInfo) {
        super(exporterManager, metricInfo);
    }

    @Override
    public void init() throws Exception {
        getMetricInfo().checkHasTargetParameter();
        serverId = getMetricInfo().getTarget();
    }

    @Override
    public void start() {
        try {
            TowServer towServer = getExporterManager().getBean(serverId);
            Undertow undertow = towServer.getUndertow();
            metrics = undertow.getWorker().getMXBean();
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve session manager with " + getMetricInfo().getTarget(), e);
        }
    }

    @Override
    public void stop() {
        if (metrics != null) {
            metrics = null;
        }
    }

    @Override
    public MetricData getMetricData(boolean greater) {
        if (metrics == null) {
            return null;
        }

        int active = metrics.getBusyWorkerThreadCount();
        if (greater && active <= oldActive) {
            return null;
        }

        int total = metrics.getWorkerPoolSize();
        int max = metrics.getMaxWorkerPoolSize();

        oldActive = active;

        return new MetricData(getMetricInfo())
                .setFormat("{active}/{total}")
                .putData("workerName", metrics.getName())
                .putData("active", active)
                .putData("total", total)
                .putData("max", max);
    }

    @Override
    public boolean hasChanges() {
        if (metrics == null) {
            return false;
        }
        return (metrics.getBusyWorkerThreadCount() != oldActive);
    }

}

package com.aspectran.appmon.exporter.event.status.undertow;

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.appmon.exporter.event.status.AbstractStatusReader;
import com.aspectran.appmon.exporter.event.status.StatusInfo;
import com.aspectran.undertow.server.TowServer;
import com.aspectran.utils.annotation.jsr305.NonNull;
import io.undertow.Undertow;
import org.xnio.management.XnioWorkerMXBean;

/**
 * The following JVM system properties must be set:
 * <pre>
*   -Djboss.threads.eqe.statistics=true
*   -Djboss.threads.eqe.statistics.active-count=true
 * </pre>
 * Or to set the system parameters in aspectran-config.apon:
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
public class NioWorkerMetricsReader extends AbstractStatusReader {

    private String serverId;

    private XnioWorkerMXBean metrics;

    private int oldPoolSize;

    private int oldActiveCount;

    public NioWorkerMetricsReader(
            @NonNull ExporterManager exporterManager,
            @NonNull EventInfo eventInfo) {
        super(exporterManager, eventInfo);
    }

    @Override
    public void init() throws Exception {
        getEventInfo().checkHasTargetParameter();
        serverId = getEventInfo().getTarget();
    }

    @Override
    public void start() {
        try {
            TowServer towServer = getExporterManager().getBean(serverId);
            Undertow undertow = towServer.getUndertow();
            metrics = undertow.getWorker().getMXBean();
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve session manager with " + getEventInfo().getTarget(), e);
        }
    }

    @Override
    public void stop() {
        if (metrics != null) {
            metrics = null;
        }
    }

    @Override
    protected StatusInfo getStatusInfo() {
        if (metrics == null) {
            return null;
        }

        int activeCount = metrics.getBusyWorkerThreadCount();
        int poolSize = metrics.getWorkerPoolSize();
        int maxPoolSize = metrics.getMaxWorkerPoolSize();

        String text = activeCount + "/" + poolSize;

        return new StatusInfo(getEventInfo())
                .setText(text)
                .putData("workerName", metrics.getName())
                .putData("activeCount", activeCount)
                .putData("poolSize", poolSize)
                .putData("maxPoolSize", maxPoolSize);
    }

    @Override
    protected boolean hasChanges() {
        if (metrics == null) {
            return false;
        }

        boolean changed = (metrics.getBusyWorkerThreadCount() != oldActiveCount ||
                metrics.getWorkerPoolSize() != oldPoolSize);

        if (changed) {
            oldActiveCount = metrics.getBusyWorkerThreadCount();
            oldPoolSize = metrics.getWorkerPoolSize();
            return true;
        } else {
            return false;
        }
    }

}

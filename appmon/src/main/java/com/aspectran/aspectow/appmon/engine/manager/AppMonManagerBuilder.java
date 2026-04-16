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
package com.aspectran.aspectow.appmon.engine.manager;

import com.aspectran.aspectow.appmon.engine.config.AppMonConfig;
import com.aspectran.aspectow.appmon.engine.config.DomainInfoHolder;
import com.aspectran.aspectow.appmon.engine.config.EventInfo;
import com.aspectran.aspectow.appmon.engine.config.InstanceInfo;
import com.aspectran.aspectow.appmon.engine.config.InstanceInfoHolder;
import com.aspectran.aspectow.appmon.engine.config.LogInfo;
import com.aspectran.aspectow.appmon.engine.config.MetricInfo;
import com.aspectran.aspectow.appmon.engine.config.PollingConfig;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterType;
import com.aspectran.aspectow.appmon.engine.exporter.event.ChartDataExporter;
import com.aspectran.aspectow.appmon.engine.exporter.event.ChartDataExporterBuilder;
import com.aspectran.aspectow.appmon.engine.exporter.event.EventExporter;
import com.aspectran.aspectow.appmon.engine.exporter.event.EventExporterBuilder;
import com.aspectran.aspectow.appmon.engine.exporter.log.LogExporter;
import com.aspectran.aspectow.appmon.engine.exporter.log.LogExporterBuilder;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricExporter;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricExporterBuilder;
import com.aspectran.aspectow.appmon.engine.persist.counter.EventCounter;
import com.aspectran.aspectow.appmon.engine.persist.counter.EventCounterBuilder;
import com.aspectran.aspectow.node.config.NodeConfig;
import com.aspectran.aspectow.node.config.NodeConfigBuilder;
import com.aspectran.aspectow.node.config.NodeConfigResolver;
import com.aspectran.aspectow.node.manager.NodeReporter;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.Assert;
import com.aspectran.utils.SystemUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.aspectran.aspectow.appmon.engine.schedule.CounterPersistSchedule.DEFAULT_SAMPLE_INTERVAL_IN_MINUTES;

/**
 * A builder for creating and configuring the main {@link AppMonManager} instance.
 * It orchestrates the entire setup process, including the creation of exporters and other sub-components.
 *
 * <p>Created: 2024-12-17</p>
 */
public abstract class AppMonManagerBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AppMonManagerBuilder.class);

    public static final String APPMON_DOMAIN_PROPERTY_NAME = "appmon.domain";

    public static final String DEFAULT_DOMAIN = "localhost";

    /**
     * Builds a fully configured {@link AppMonManager} instance.
     * @param context the activity context
     * @param appMonConfig the application monitoring configuration
     * @return a new, fully initialized {@link AppMonManager} instance
     * @throws Exception if the build process fails
     */
    @NonNull
    public static AppMonManager build(ActivityContext context, AppMonConfig appMonConfig) throws Exception {
        Assert.notNull(context, "context must not be null");
        Assert.notNull(appMonConfig, "appMonConfig must not be null");

        NodeConfig nodeConfig;
        if (context.getBeanRegistry().containsBean(NodeConfigResolver.class)) {
            NodeConfigResolver nodeConfigResolver = context.getBeanRegistry().getBean(NodeConfigResolver.class);
            nodeConfig = nodeConfigResolver.resolveConfig();
        } else {
            nodeConfig = NodeConfigBuilder.build();
        }

        String nodeId = nodeConfig.getNodeInfo().getName();
        AppMonManager appMonManager = createAppMonManager(context, appMonConfig, nodeId);

        RedisConnectionPool connectionPool = context.getBeanRegistry().getBean(RedisConnectionPool.class);
        NodeReporter nodeReporter = new NodeReporter(nodeConfig, connectionPool);
        appMonManager.setNodeReporter(nodeReporter);

        if (context.getBeanRegistry().containsBean(RedisMessagePublisher.class)) {
            RedisMessagePublisher messagePublisher = context.getBeanRegistry().getBean(RedisMessagePublisher.class);
            appMonManager.getMessageRelayManager().setMessagePublisher(messagePublisher);
        }

        for (InstanceInfo instanceInfo : appMonConfig.getInstanceInfoList()) {
            String instanceName = instanceInfo.getName();

            List<EventInfo> eventInfoList = appMonConfig.getEventInfoList(instanceName);
            if (eventInfoList != null && !eventInfoList.isEmpty()) {
                buildEventExporters(appMonManager, instanceName, eventInfoList);
            }

            List<MetricInfo> metricInfoList = appMonConfig.getMetricInfoList(instanceName);
            if (metricInfoList != null && !metricInfoList.isEmpty()) {
                buildMetricExporters(appMonManager, instanceName, metricInfoList);
            }

            List<LogInfo> logInfoList = appMonConfig.getLogInfoList(instanceName);
            if (logInfoList != null && !logInfoList.isEmpty()) {
                buildLogExporters(appMonManager, instanceName, logInfoList);
            }
        }

        return appMonManager;
    }

    private static void buildEventExporters(
            AppMonManager appMonManager,
            String instanceName,
            @NonNull List<EventInfo> eventInfoList) throws Exception {
        ExporterManager eventExporterManager = new ExporterManager(ExporterType.EVENT, appMonManager, instanceName);
        ExporterManager dataExporterManager = new ExporterManager(ExporterType.DATA, appMonManager, instanceName);
        for (EventInfo eventInfo : eventInfoList) {
            eventInfo.validateRequiredParameters();

            EventCounter eventCounter = EventCounterBuilder.build(eventInfo);
            if (eventCounter != null) {
                appMonManager.getPersistManager().getCounterPersist().addEventCounter(eventCounter);

                EventExporter eventExporter = EventExporterBuilder.build(eventExporterManager, eventInfo, eventCounter.getEventCount());
                eventExporterManager.addExporter(eventExporter);

                ChartDataExporter chartDataExporter = ChartDataExporterBuilder.build(dataExporterManager, eventInfo);
                eventCounter.addEventRollupListener(chartDataExporter);
                dataExporterManager.addExporter(chartDataExporter);
            } else {
                EventExporter eventExporter = EventExporterBuilder.build(eventExporterManager, eventInfo, null);
                eventExporterManager.addExporter(eventExporter);
            }
        }
        appMonManager.getMessageRelayManager().addExporterManager(eventExporterManager);
        appMonManager.getMessageRelayManager().addExporterManager(dataExporterManager);
    }

    private static void buildMetricExporters(
            AppMonManager appMonManager,
            String instanceName,
            @NonNull List<MetricInfo> metricInfoList) throws Exception {
        ExporterManager metricExporterManager = new ExporterManager(ExporterType.METRIC, appMonManager, instanceName);
        for (MetricInfo metricInfo : metricInfoList) {
            metricInfo.validateRequiredParameters();

            MetricExporter eventExporter = MetricExporterBuilder.build(metricExporterManager, metricInfo);
            metricExporterManager.addExporter(eventExporter);
        }
        appMonManager.getMessageRelayManager().addExporterManager(metricExporterManager);
    }

    private static void buildLogExporters(
            AppMonManager appMonManager,
            String instanceName,
            @NonNull List<LogInfo> logInfoList) throws Exception {
        ExporterManager logExporterManager = new ExporterManager(ExporterType.LOG, appMonManager, instanceName);
        for (LogInfo logInfo : logInfoList) {
            logInfo.validateRequiredParameters();

            LogExporter logExporter = LogExporterBuilder.build(logExporterManager, logInfo);
            logExporterManager.addExporter(logExporter);
        }
        appMonManager.getMessageRelayManager().addExporterManager(logExporterManager);
    }

    @NonNull
    private static AppMonManager createAppMonManager(
            ActivityContext context,
            @NonNull AppMonConfig appMonConfig,
            String nodeId) throws Exception {
        PollingConfig pollingConfig = appMonConfig.getPollingConfig();
        if (pollingConfig == null) {
            pollingConfig = new PollingConfig();
        }

        int counterPersistInterval = appMonConfig.getCounterPersistInterval(DEFAULT_SAMPLE_INTERVAL_IN_MINUTES);

        DomainInfoHolder domainInfoHolder = new DomainInfoHolder(appMonConfig.getDomainInfoList());

        String currentDomain = resolveCurrentDomain();
        if (!domainInfoHolder.hasDomain(currentDomain) && !DEFAULT_DOMAIN.equals(currentDomain)) {
            throw new Exception("Unknown domain in AppMon: " + currentDomain);
        }
        logger.info("Current AppMon domain: {}", currentDomain);

        InstanceInfoHolder instanceInfoHolder = new InstanceInfoHolder(currentDomain, appMonConfig.getInstanceInfoList());

        AppMonManager appMonManager = new AppMonManager(nodeId, currentDomain, pollingConfig, counterPersistInterval,
                domainInfoHolder, instanceInfoHolder);
        appMonManager.setActivityContext(context);
        return appMonManager;
    }

    private static String resolveCurrentDomain() {
        String domain = SystemUtils.getProperty(APPMON_DOMAIN_PROPERTY_NAME);
        return (domain != null ? domain : DEFAULT_DOMAIN);
    }

}

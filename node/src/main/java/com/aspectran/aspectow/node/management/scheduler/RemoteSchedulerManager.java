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
package com.aspectran.aspectow.node.management.scheduler;

import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBridge;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBroker;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerSession;
import com.aspectran.aspectow.node.management.scheduler.log.SchedulerLogExporter;
import com.aspectran.aspectow.node.management.scheduler.remote.RemoteSchedulerMessageListener;
import com.aspectran.aspectow.node.manager.ClusterEventListener;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeMessagePublisher;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.aware.ApplicationAdapterAware;
import com.aspectran.core.scheduler.service.SchedulerService;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.logging.LoggingDefaults;
import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.json.JsonBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RemoteSchedulerManager orchestrates scheduler management across the cluster.
 * It manages local execution, remote dispatching via Redis, and broadcasting
 * results to connected clients.
 */
public class RemoteSchedulerManager implements ApplicationAdapterAware, InitializableBean,
        DisposableBean, ClusterEventListener {

    private static final Logger logger = LoggerFactory.getLogger(RemoteSchedulerManager.class);

    public static final String OP_SERVICES = "services";

    private static final String OP_ENABLE = "enable";

    private static final String OP_DISABLE = "disable";

    private static final String OP_PREVIOUS_LOGS = "previousLogs";

    private static final String STATE_UPDATED = "stateUpdated";

    private final Map<String, SchedulerLogExporter> logExporters = new ConcurrentHashMap<>();

    private final Map<String, SchedulerBridge> sessionBridgeMap = new ConcurrentHashMap<>();

    private final NodeManager nodeManager;

    private final NodeMessagePublisher messagePublisher;

    private final LocalSchedulerService localSchedulerService;

    private final SchedulerBroker broker;

    private ApplicationAdapter applicationAdapter;

    private RemoteSchedulerMessageListener bridgeHandler;

    /**
     * Constructs a new RemoteSchedulerManager.
     * @param nodeManager the node manager
     */
    @Autowired
    public RemoteSchedulerManager(@NonNull NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.messagePublisher = nodeManager.getNodeMessagePublisher();
        this.localSchedulerService = new LocalSchedulerService();
        this.broker = new SchedulerBroker(this);
    }

    /**
     * Sets the application adapter.
     * @param applicationAdapter the application adapter to set
     */
    @Override
    public void setApplicationAdapter(ApplicationAdapter applicationAdapter) {
        this.applicationAdapter = applicationAdapter;
    }

    /**
     * Initializes the remote scheduler manager and registers message listeners.
     * @throws Exception if initialization fails
     */
    @Override
    public void initialize() throws Exception {
        logger.info("Initializing RemoteSchedulerManager for node: {}", getNodeId());

        if (nodeManager.getNodeMessageSubscriber() != null) {
            this.bridgeHandler = new RemoteSchedulerMessageListener(this);
            nodeManager.getNodeMessageSubscriber().addListener(this.bridgeHandler);
        }

        if (nodeManager.getClusterEventSubscriber() != null) {
            nodeManager.getClusterEventSubscriber().addListener(this);
        }
    }

    @Override
    public void destroy() {
        if (nodeManager.getNodeMessageSubscriber() != null && this.bridgeHandler != null) {
            nodeManager.getNodeMessageSubscriber().removeListener(this.bridgeHandler);
        }
        if (nodeManager.getClusterEventSubscriber() != null) {
            nodeManager.getClusterEventSubscriber().removeListener(this);
        }
    }

    /**
     * Gets the node manager.
     * @return the node manager
     */
    public NodeManager getNodeManager() {
        return nodeManager;
    }

    /**
     * Gets the node message publisher.
     * @return the message publisher
     */
    public NodeMessagePublisher getMessagePublisher() {
        return messagePublisher;
    }

    /**
     * Checks if running in gateway mode.
     * @return true if gateway mode is active, false otherwise
     */
    public boolean isGatewayMode() {
        return (messagePublisher != null);
    }

    /**
     * Gets the node ID.
     * @return the node ID
     */
    public String getNodeId() {
        return nodeManager.getNodeId();
    }

    /**
     * Checks if the given node ID matches this node's ID.
     * @param targetNodeId the node ID to check
     * @return true if it is the same node, false otherwise
     */
    public boolean isSameNode(String targetNodeId) {
        return (targetNodeId != null && targetNodeId.equals(getNodeId()));
    }

    /**
     * Gets the scheduler broker.
     * @return the scheduler broker
     */
    public SchedulerBroker getBroker() {
        return broker;
    }

    /**
     * Registers a session.
     * @param sessionId the session ID
     * @param schedulerBridge the scheduler bridge
     */
    public void registerSession(String sessionId, SchedulerBridge schedulerBridge) {
        sessionBridgeMap.put(sessionId, schedulerBridge);
    }

    /**
     * Unregisters a session.
     * @param sessionId the session ID to unregister
     */
    public void unregisterSession(String sessionId) {
        sessionBridgeMap.remove(sessionId);
    }

    /**
     * Starts all discovered log exporters.
     */
    public synchronized void startExporters() {
        discoverLogFiles();
        for (SchedulerLogExporter exporter : logExporters.values()) {
            try {
                exporter.start();
            } catch (Exception e) {
                logger.error("Failed to start scheduler log exporter for context: {}", exporter.getLoggingGroup(), e);
            }
        }
    }

    /**
     * Stops all active log exporters.
     */
    public synchronized void stopExporters() {
        for (SchedulerLogExporter exporter : logExporters.values()) {
            try {
                exporter.stop();
            } catch (Exception e) {
                logger.error("Failed to stop scheduler log exporter for context: {}", exporter.getLoggingGroup(), e);
            }
        }
    }

    private void discoverLogFiles() {
        String baseLogDir = System.getProperty(LoggingDefaults.LOGS_DIR_PROPERTY);
        File logsDir;
        if (StringUtils.hasText(baseLogDir)) {
            logsDir = applicationAdapter.getRealPath(baseLogDir).toFile();
        } else {
            logsDir = applicationAdapter.getRealPath(LoggingDefaults.DEFAULT_LOGS_DIR).toFile();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Discovering scheduler log files in: {}", logsDir.getAbsolutePath());
        }

        String logCharset = System.getProperty(LoggingDefaults.LOG_CHARSET_PROPERTY);

        for (CoreService service : CoreServiceHolder.getAllServices()) {
            if (service.getServiceLifeCycle().isActive()) {
                SchedulerService schedulerService = service.getSchedulerService();
                if (schedulerService != null) {
                    String loggingGroup = schedulerService.getLoggingGroup();
                    if (!logExporters.containsKey(loggingGroup)) {
                        File logFile = new File(logsDir, loggingGroup + "-scheduler.log");
                        if (logFile.exists()) {
                            logger.info("Discovered scheduler log file: {}", logFile.getAbsolutePath());
                            // Currently using default settings; future: apply injected overrides
                            SchedulerLogExporter exporter = new SchedulerLogExporter(
                                    getNodeId(), loggingGroup, logFile, broker, logCharset);
                            logExporters.put(loggingGroup, exporter);
                        } else if (logger.isDebugEnabled()) {
                            logger.debug("Scheduler log file not found: {}", logFile.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    /**
     * Dispatches a management request to a specific node or handles it locally.
     * @param request the structured request parameters
     */
    public void process(@NonNull SchedulerRequestParameters request) {
        String command = request.getCommand();
        if ("bulkEnable".equals(command)) {
            request.setCommand(OP_ENABLE);
        } else if ("bulkDisable".equals(command)) {
            request.setCommand(OP_DISABLE);
        }

        String[] targetNodeIds = request.getTargetNodeIds();
        if (targetNodeIds != null && targetNodeIds.length > 0) {
            for (String targetId : targetNodeIds) {
                SchedulerRequestParameters subRequest = new SchedulerRequestParameters();
                try {
                    subRequest.readFrom(request.toString());
                } catch (Exception e) {
                    logger.error("Failed to copy scheduler request for target node {}", targetId, e);
                    continue;
                }
                subRequest.setTargetNodeId(targetId);
                subRequest.removeValue(SchedulerRequestParameters.targetNodeIds);
                process(subRequest);
            }
            return;
        }

        String targetNodeId = request.getTargetNodeId();
        if (isSameNode(targetNodeId)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Executing local scheduler request: {}", request.getCommand());
            }
            SchedulerResponseParameters response = execute(request);
            if (response != null) {
                String header = response.getHeader();
                String sessionId = request.getSessionId();
                String message = response.toString();
                if (STATE_UPDATED.equals(header)) {
                    bridge(message);
                } else if (sessionId != null) {
                    bridge(sessionId, message);
                }
            }
        } else {
            dispatch(targetNodeId, request);
        }
    }

    private void dispatch(String targetNodeId, @NonNull SchedulerRequestParameters request) {
        if (messagePublisher != null) {
            try {
                String message = SchedulerBroker.CONTROL_REQUEST + request;
                messagePublisher.publishControl(SchedulerBroker.CATEGORY_SCHEDULER, targetNodeId, message);
                if (logger.isDebugEnabled()) {
                    logger.debug("Scheduler request dispatched to node {}: {}", targetNodeId, request.getCommand());
                }
            } catch (Exception e) {
                logger.error("Failed to dispatch scheduler request to node {}", targetNodeId, e);
            }
        } else {
            logger.warn("Cannot dispatch request to node {}: Redis publisher not available", targetNodeId);
        }
    }

    /**
     * Processes an incoming message received from the cluster relay.
     * @param request the structured request parameters
     */
    public void processRemotely(SchedulerRequestParameters request) {
        Assert.notNull(request, "Scheduler request cannot be null");
        try {
            SchedulerResponseParameters response = execute(request);
            if (response != null && messagePublisher != null) {
                String gatewayNodeId = request.getNodeId();
                String sessionId = request.getSessionId();
                String message = response.toString();
                if (STATE_UPDATED.equals(response.getHeader())) {
                    bridge(message);
                } else {
                    bridgeRemotely(gatewayNodeId, sessionId, message);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process scheduler request: {}", request, e);
        }
    }

    /**
     * Executes the management logic for a given request on the local node.
     * @param request the structured request parameters
     * @return the execution result as JSON string, or null if unhandled
     */
    @Nullable
    private SchedulerResponseParameters execute(SchedulerRequestParameters request) {
        SchedulerResponseParameters response = null;
        try {
            String command = request.getCommand();
            switch (command) {
                case OP_SERVICES -> response = localSchedulerService.getSchedulesAsJson();
                case OP_ENABLE -> response = performStateChange(request, false);
                case OP_DISABLE -> response = performStateChange(request, true);
                case OP_PREVIOUS_LOGS -> response = readPreviousLogs(request);
            }
        } catch (Exception e) {
            logger.error("Failed to execute scheduler request: {}", request, e);
        }
        if (response != null) {
            response.setNodeId(getNodeId());
        }
        return response;
    }

    @Nullable
    private SchedulerResponseParameters readPreviousLogs(@NonNull SchedulerRequestParameters request) {
        String loggingGroup = request.getLoggingGroup();
        int loadedLines = request.getLoadedLines();
        if (loggingGroup != null) {
            List<String> lines = null;
            SchedulerLogExporter exporter = logExporters.get(loggingGroup);
            if (exporter != null) {
                lines = exporter.readPreviousLines(loadedLines);
            }
            if (lines == null) {
                lines = Collections.emptyList();
            }
            JsonBuilder jsonBuilder = new JsonBuilder().put(lines);
            return new SchedulerResponseParameters()
                    .setHeader(OP_PREVIOUS_LOGS)
                    .setOwner(loggingGroup)
                    .setData(jsonBuilder.toJsonString());
        }
        return null;
    }

    @Nullable
    private SchedulerResponseParameters performStateChange(@NonNull SchedulerRequestParameters request, boolean disabled) {
        String serviceName = request.getServiceName();
        String scheduleId = request.getScheduleId();
        String jobName = request.getJobName();

        if (jobName != null) {
            return localSchedulerService.updateState(serviceName, "job", jobName, disabled);
        } else if (scheduleId != null) {
            return localSchedulerService.updateState(serviceName, "schedule", scheduleId, disabled);
        }
        return null;
    }

    /**
     * Bridges a message locally and Relays it to remote nodes.
     * @param message the message payload
     */
    public void bridge(String message) {
        broker.bridge(message, true);
    }

    /**
     * Bridges a message locally only.
     * @param message the message payload
     */
    public void bridgeRemotely(String message) {
        broker.bridge(message, false);
    }

    /**
     * Relays a message to a specific remote node and session.
     * @param nodeId the remote node ID
     * @param sessionId the remote session ID
     * @param message the message payload
     */
    public void bridgeRemotely(String nodeId, String sessionId, String message) {
        broker.bridgeRemotely(nodeId, sessionId, message);
    }

    /**
     * Relays log data to a remote node.
     * @param nodeId the remote node ID
     * @param message the log message
     */
    public void bridgeLogRemotely(String nodeId, String message) {
        broker.bridgeLog(nodeId, message, false);
    }

    /**
     * Bridges a message to a specific local session.
     * @param sessionId the local session ID
     * @param message the message payload
     */
    public void bridge(String sessionId, String message) {
        bridge(null, sessionId, message);
    }

    /**
     * Bridges a message to a specific session, verifying node ID if supplied.
     * @param nodeId the node ID, or null
     * @param sessionId the session ID
     * @param message the message payload
     */
    public void bridge(String nodeId, String sessionId, String message) {
        SchedulerBridge bridge = sessionBridgeMap.get(sessionId);
        if (bridge != null) {
            SchedulerSession session = bridge.findSchedulerSession(sessionId);
            if (session != null) {
                bridge.bridge(session, message);
            }
        }
    }

    @Override
    public void onNodeJoined(NodeInfo info) {
        if (sessionBridgeMap.isEmpty()) {
            return;
        }
        if (isGatewayMode()) {
            broker.subscribeRemoteNode(info.getId());
        }
        try {
            SchedulerResponseParameters response = new SchedulerResponseParameters()
                    .setHeader("nodeJoined")
                    .setNodeId(info.getId());
            String message = response.toString();
            for (Map.Entry<String, SchedulerBridge> entry : sessionBridgeMap.entrySet()) {
                String sessionId = entry.getKey();
                SchedulerBridge bridge = entry.getValue();
                SchedulerSession session = bridge.findSchedulerSession(sessionId);
                if (session != null) {
                    bridge.bridge(session, message);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to broadcast join event of node {}", info.getId(), e);
        }
    }

    @Override
    public void onNodeLeft(String leftNodeId) {
        if (sessionBridgeMap.isEmpty()) {
            return;
        }
        try {
            SchedulerResponseParameters response = new SchedulerResponseParameters()
                    .setHeader("nodeLeft")
                    .setNodeId(leftNodeId);
            String message = response.toString();
            for (Map.Entry<String, SchedulerBridge> entry : sessionBridgeMap.entrySet()) {
                String sessionId = entry.getKey();
                SchedulerBridge bridge = entry.getValue();
                SchedulerSession session = bridge.findSchedulerSession(sessionId);
                if (session != null) {
                    bridge.bridge(session, message);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to broadcast left event of node {}", leftNodeId, e);
        }
    }

}

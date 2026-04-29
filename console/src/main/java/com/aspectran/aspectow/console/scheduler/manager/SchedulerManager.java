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
package com.aspectran.aspectow.console.scheduler.manager;

import com.aspectran.aspectow.console.scheduler.bridge.SchedulerBroker;
import com.aspectran.aspectow.console.scheduler.bridge.SchedulerRequestParameters;
import com.aspectran.aspectow.console.scheduler.bridge.redis.SchedulerMessageBridgeHandler;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeMessageProtocol;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SchedulerManager orchestrates scheduler management across the cluster.
 * It manages local execution, remote dispatching via Redis, and broadcasting
 * results to connected clients.
 */
@Component
@Bean(id = "schedulerManager")
public class SchedulerManager implements InitializableBean {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerManager.class);

    private static final String OP_LIST = "list";
    private static final String OP_ENABLE = "enable";
    private static final String OP_DISABLE = "disable";

    private final NodeManager nodeManager;

    private final LocalSchedulerService localSchedulerService;

    private final SchedulerBroker broker;

    public SchedulerManager(@NonNull NodeManager nodeManager, LocalSchedulerService localSchedulerService) {
        this.nodeManager = nodeManager;
        this.localSchedulerService = localSchedulerService;
        this.broker = new SchedulerBroker(nodeManager.getNodeId(), nodeManager.getRedisMessagePublisher());
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing SchedulerManager for node: {}", nodeManager.getNodeId());

        if (nodeManager.getRedisMessageSubscriber() != null) {
            SchedulerMessageBridgeHandler bridgeHandler = new SchedulerMessageBridgeHandler(this);
            nodeManager.getRedisMessageSubscriber().addListener(bridgeHandler);
        }
    }

    public SchedulerBroker getBroker() {
        return broker;
    }

    /**
     * Dispatches a management request to a specific node or handles it locally.
     * @param targetNodeId the ID of the node to receive the request
     * @param request the structured request parameters
     */
    public void dispatch(String targetNodeId, SchedulerRequestParameters request) {
        if (nodeManager.getNodeId().equals(targetNodeId)) {
            logger.debug("Executing local scheduler request: {}", request.getCommand());
            String response = execute(request);
            if (response != null) {
                broadcast(response);
            }
        } else {
            if (nodeManager.getRedisMessagePublisher() != null) {
                try {
                    // Convert to string only when sending over the network (Redis)
                    String message = "command:" + request.toString() + ";" + targetNodeId;
                    nodeManager.getRedisMessagePublisher().publishRelay(NodeMessageProtocol.CATEGORY_SCHEDULER, message);
                    logger.debug("Scheduler request dispatched to node {}: {}", targetNodeId, request.getCommand());
                } catch (Exception e) {
                    logger.error("Failed to dispatch scheduler request to node {}", targetNodeId, e);
                }
            } else {
                logger.warn("Cannot dispatch request to node {}: Redis publisher not available", targetNodeId);
            }
        }
    }

    /**
     * Processes an incoming message received from the cluster relay.
     * @param message the raw relay message
     */
    public void process(String message) {
        if (StringUtils.isEmpty(message) || !message.startsWith("command:")) {
            return;
        }

        String payload = message.substring(8);
        int idx = payload.indexOf(';');
        String requestData;
        String targetNodeId = null;

        if (idx != -1) {
            requestData = payload.substring(0, idx);
            targetNodeId = payload.substring(idx + 1);
        } else {
            requestData = payload;
        }

        if (targetNodeId == null || targetNodeId.equals(nodeManager.getNodeId())) {
            try {
                SchedulerRequestParameters request = new SchedulerRequestParameters();
                request.readFrom(requestData);

                String response = execute(request);
                if (response != null && nodeManager.getRedisMessagePublisher() != null) {
                    nodeManager.getRedisMessagePublisher().publishRelay(NodeMessageProtocol.CATEGORY_SCHEDULER, response);
                }
            } catch (Exception e) {
                logger.error("Failed to process scheduler relay message", e);
            }
        }
    }

    /**
     * Executes the management logic for a given request on the local node.
     * @param request the structured request parameters
     * @return the execution result as JSON string, or null if unhandled
     */
    private String execute(SchedulerRequestParameters request) {
        try {
            String command = request.getCommand();
            if (OP_LIST.equals(command)) {
                return localSchedulerService.getSchedulesAsJson();
            } else if (OP_ENABLE.equals(command)) {
                return performStateChange(request, false);
            } else if (OP_DISABLE.equals(command)) {
                return performStateChange(request, true);
            }
        } catch (Exception e) {
            logger.error("Failed to execute scheduler request", e);
        }
        return null;
    }

    private String performStateChange(SchedulerRequestParameters request, boolean disabled) {
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
     * Broadcasts a management result to all connected clients on this node.
     * @param response the result payload in JSON format
     */
    public void broadcast(String response) {
        if (logger.isTraceEnabled()) {
            logger.trace("Broadcasting scheduler result to local clients: {}", response);
        }
        if (broker != null) {
            broker.bridge(response);
        }
    }

}

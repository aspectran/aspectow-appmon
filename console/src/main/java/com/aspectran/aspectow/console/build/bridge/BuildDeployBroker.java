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
package com.aspectran.aspectow.console.build.bridge;

import com.aspectran.aspectow.console.build.manager.BuildExecutionInfo;
import com.aspectran.aspectow.node.manager.NodeMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * BuildDeployBroker coordinates the broadcasting of real-time logs and
 * status changes to connected client sessions and Redis relays.
 *
 * <p>Created: 2026-08-18</p>
 */
public class BuildDeployBroker {

    private static final Logger logger = LoggerFactory.getLogger(BuildDeployBroker.class);

    public static final String CATEGORY_BUILD = "build_deploy";

    private final String nodeId;

    private final NodeMessagePublisher messagePublisher;

    private final Set<BuildDeployBridge> bridges = new CopyOnWriteArraySet<>();

    public BuildDeployBroker(String nodeId, NodeMessagePublisher messagePublisher) {
        this.nodeId = nodeId;
        this.messagePublisher = messagePublisher;
    }

    public String getNodeId() {
        return nodeId;
    }

    public NodeMessagePublisher getMessagePublisher() {
        return messagePublisher;
    }

    public void addBridge(BuildDeployBridge bridge) {
        bridges.add(bridge);
    }

    public void removeBridge(BuildDeployBridge bridge) {
        bridges.remove(bridge);
    }

    public void bridge(String data) {
        for (BuildDeployBridge bridge : bridges) {
            try {
                bridge.bridge(data);
            } catch (Exception e) {
                logger.warn("Failed to bridge build data via {}: {}",
                        bridge.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    public void bridge(BuildDeploySession session, String data) {
        for (BuildDeployBridge bridge : bridges) {
            try {
                bridge.bridge(session, data);
            } catch (Exception e) {
                logger.warn("Failed to bridge build data to session {} via {}: {}",
                        session.getSessionId(), bridge.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Broadcasts a single log line to connected clients.
     */
    public void broadcastLogLine(String executionId, String targetNodeId, String line) {
        BuildResponseParameters res = new BuildResponseParameters()
                .setHeader("log")
                .setExecutionId(executionId)
                .setNodeId(targetNodeId != null ? targetNodeId : nodeId)
                .setLine(line);
        bridge(res.toString());
    }

    /**
     * Sends backfilled logs to a specific connected session.
     */
    public void sendLogBackfill(BuildDeploySession session, String executionId, String targetNodeId, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        BuildResponseParameters res = new BuildResponseParameters()
                .setHeader("logBackfill")
                .setExecutionId(executionId)
                .setNodeId(targetNodeId != null ? targetNodeId : nodeId)
                .setLines(lines);
        bridge(session, res.toString());
    }

    /**
     * Broadcasts a status change event to connected clients.
     */
    public void broadcastStatusChanged(BuildExecutionInfo info) {
        BuildResponseParameters res = new BuildResponseParameters()
                .setHeader("status")
                .setExecutionId(info.getExecutionId())
                .setNodeId(info.getTargetNodeId() != null ? info.getTargetNodeId() : nodeId)
                .setStatus(info.getStatus().name())
                .setExitCode(info.getExitCode())
                .setDurationMs(info.getDurationMs())
                .setGitBranch(info.getGitBranch())
                .setGitCommitBefore(info.getGitCommitBefore())
                .setGitCommitAfter(info.getGitCommitAfter())
                .setGitCommitMsg(info.getGitCommitMsg())
                .setError(info.getErrorSummary());
        bridge(res.toString());
    }

}

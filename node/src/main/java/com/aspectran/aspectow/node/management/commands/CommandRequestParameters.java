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
package com.aspectran.aspectow.node.management.commands;

import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Represents a structured message for remote command execution.
 * It encapsulates the command type, routing information, and the command payload.
 */
public class CommandRequestParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey requestId;
    public static final ParameterKey nodeId;
    public static final ParameterKey sessionId;
    public static final ParameterKey targetNodeId;
    public static final ParameterKey targetGroup;
    public static final ParameterKey targetAll;
    public static final ParameterKey targetServices;
    public static final ParameterKey command;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        requestId = new ParameterKey("requestId", ValueType.STRING);
        nodeId = new ParameterKey("nodeId", ValueType.STRING);
        sessionId = new ParameterKey("sessionId", ValueType.STRING);
        targetNodeId = new ParameterKey("targetNodeId", ValueType.STRING);
        targetGroup = new ParameterKey("targetGroup", ValueType.STRING);
        targetAll = new ParameterKey("targetAll", ValueType.BOOLEAN);
        targetServices = new ParameterKey("targetServices", ValueType.BOOLEAN);
        command = new ParameterKey("command", CommandParameters.class);

        parameterKeys = new ParameterKey[] {
                header,
                requestId,
                nodeId,
                sessionId,
                targetNodeId,
                targetGroup,
                targetAll,
                targetServices,
                command
        };
    }

    /**
     * Instantiates a new CommandRequestParameters.
     */
    public CommandRequestParameters() {
        super(parameterKeys);
    }

    /**
     * Returns the command request header.
     * @return the header
     */
    public String getHeader() {
        return getString(header);
    }

    /**
     * Sets the command request header.
     * @param header the header to set
     */
    public void setHeader(String header) {
        putValue(CommandRequestParameters.header, header);
    }

    /**
     * Returns the command request ID.
     * @return the request ID
     */
    public String getRequestId() {
        return getString(requestId);
    }

    /**
     * Sets the command request ID.
     * @param requestId the request ID to set
     */
    public void setRequestId(String requestId) {
        putValue(CommandRequestParameters.requestId, requestId);
    }

    /**
     * Returns the ID of the node initiating the request.
     * @return the node ID
     */
    public String getNodeId() {
        return getString(nodeId);
    }

    /**
     * Sets the ID of the node initiating the request.
     * @param nodeId the node ID to set
     */
    public void setNodeId(String nodeId) {
        putValue(CommandRequestParameters.nodeId, nodeId);
    }

    /**
     * Returns the session ID associated with the command request.
     * @return the session ID
     */
    public String getSessionId() {
        return getString(sessionId);
    }

    /**
     * Sets the session ID associated with the command request.
     * @param sessionId the session ID to set
     */
    public void setSessionId(String sessionId) {
        putValue(CommandRequestParameters.sessionId, sessionId);
    }

    /**
     * Returns the ID of the target node for the command.
     * @return the target node ID
     */
    public String getTargetNodeId() {
        return getString(targetNodeId);
    }

    /**
     * Sets the ID of the target node for the command.
     * @param targetNodeId the target node ID to set
     */
    public void setTargetNodeId(String targetNodeId) {
        putValue(CommandRequestParameters.targetNodeId, targetNodeId);
    }

    /**
     * Returns the target group name for the command execution.
     * @return the target group name
     */
    public String getTargetGroup() {
        return getString(targetGroup);
    }

    /**
     * Sets the target group name for the command execution.
     * @param targetGroup the target group name to set
     */
    public void setTargetGroup(String targetGroup) {
        putValue(CommandRequestParameters.targetGroup, targetGroup);
    }

    /**
     * Returns whether the command is targeted at all nodes in the cluster.
     * @return true if target is all nodes, false otherwise
     */
    public boolean isTargetAll() {
        return getBoolean(targetAll, false);
    }

    /**
     * Sets whether the command is targeted at all nodes in the cluster.
     * @param targetAll true if target is all nodes, false otherwise
     */
    public void setTargetAll(boolean targetAll) {
        putValue(CommandRequestParameters.targetAll, targetAll);
    }

    /**
     * Returns whether the command is targeted at all service nodes in the cluster.
     * @return true if target is all service nodes, false otherwise
     */
    public boolean isTargetServices() {
        return getBoolean(targetServices, false);
    }

    /**
     * Sets whether the command is targeted at all service nodes in the cluster.
     * @param targetServices true if target is all service nodes, false otherwise
     */
    public void setTargetServices(boolean targetServices) {
        putValue(CommandRequestParameters.targetServices, targetServices);
    }

    /**
     * Returns the command payload parameters.
     * @return the command parameters
     */
    public CommandParameters getCommand() {
        return getParameters(command);
    }

    /**
     * Sets the command payload parameters.
     * @param command the command parameters to set
     */
    public void setCommand(CommandParameters command) {
        putValue(CommandRequestParameters.command, command);
    }

}

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

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;
import com.aspectran.utils.json.JsonBuilder;

/**
 * Represents a structured outgoing message for remote command results.
 */
public class CommandResponseParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey requestId;
    public static final ParameterKey nodeId;
    public static final ParameterKey result;
    public static final ParameterKey error;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        requestId = new ParameterKey("requestId", ValueType.STRING);
        nodeId = new ParameterKey("nodeId", ValueType.STRING);
        result = new ParameterKey("result", ValueType.TEXT);
        error = new ParameterKey("error", ValueType.TEXT);

        parameterKeys = new ParameterKey[] {
                header,
                requestId,
                nodeId,
                result,
                error
        };
    }

    /**
     * Instantiates a new CommandResponseParameters.
     */
    public CommandResponseParameters() {
        super(parameterKeys);
    }

    /**
     * Returns the command response header.
     * @return the header
     */
    public String getHeader() {
        return getString(header);
    }

    /**
     * Sets the command response header.
     * @param headerValue the header value
     * @return this instance
     */
    public CommandResponseParameters setHeader(String headerValue) {
        putValue(header, headerValue);
        return this;
    }

    /**
     * Returns the associated request ID.
     * @return the request ID
     */
    public String getRequestId() {
        return getString(requestId);
    }

    /**
     * Sets the associated request ID.
     * @param requestIdValue the request ID value
     * @return this instance
     */
    public CommandResponseParameters setRequestId(String requestIdValue) {
        putValue(requestId, requestIdValue);
        return this;
    }

    /**
     * Returns the ID of the node sending the response.
     * @return the node ID
     */
    public String getNodeId() {
        return getString(nodeId);
    }

    /**
     * Sets the ID of the node sending the response.
     * @param nodeId the node ID
     * @return this instance
     */
    public CommandResponseParameters setNodeId(String nodeId) {
        putValue(CommandResponseParameters.nodeId, nodeId);
        return this;
    }

    /**
     * Returns the result of the command execution.
     * @return the result text
     */
    public String getResult() {
        return getString(result);
    }

    /**
     * Sets the result of the command execution.
     * @param resultValue the result text
     * @return this instance
     */
    public CommandResponseParameters setResult(String resultValue) {
        putValue(result, resultValue);
        return this;
    }

    /**
     * Returns the error message, if any, that occurred during command execution.
     * @return the error message
     */
    public String getError() {
        return getString(error);
    }

    /**
     * Sets the error message that occurred during command execution.
     * @param errorValue the error message
     * @return this instance
     */
    public CommandResponseParameters setError(String errorValue) {
        putValue(error, errorValue);
        return this;
    }

    /**
     * Returns a JSON representation of the parameters.
     * @return the JSON string representation
     */
    @Override
    public String toString() {
        try {
            return new JsonBuilder()
                    .prettyPrint(false)
                    .nullWritable(false)
                    .put(this)
                    .toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

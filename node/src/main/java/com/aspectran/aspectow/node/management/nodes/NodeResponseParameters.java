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
package com.aspectran.aspectow.node.management.nodes;

import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.utils.apon.AponRenderStyle;
import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;
import com.aspectran.utils.json.JsonBuilder;

/**
 * Parameters representing a node management response.
 */
public class NodeResponseParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey nodeId;
    public static final ParameterKey node;
    public static final ParameterKey error;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        nodeId = new ParameterKey("nodeId", ValueType.STRING);
        node = new ParameterKey("node", NodeInfo.class);
        error = new ParameterKey("error", ValueType.TEXT);

        parameterKeys = new ParameterKey[] {
                header,
                nodeId,
                node,
                error
        };
    }

    /**
     * Instantiates a new NodeResponseParameters.
     */
    public NodeResponseParameters() {
        super(parameterKeys);
        setRenderStyle(AponRenderStyle.COMPACT);
    }

    /**
     * Returns the response header.
     * @return the header
     */
    public String getHeader() {
        return getString(header);
    }

    /**
     * Sets the response header.
     * @param header the header to set
     * @return this instance
     */
    public NodeResponseParameters setHeader(String header) {
        putValue(NodeResponseParameters.header, header);
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
    public NodeResponseParameters setNodeId(String nodeId) {
        putValue(NodeResponseParameters.nodeId, nodeId);
        return this;
    }

    /**
     * Returns the node configuration/state info.
     * @return the node info
     */
    public NodeInfo getNode() {
        return getParameters(node);
    }

    /**
     * Sets the node configuration/state info.
     * @param node the node info to set
     * @return this instance
     */
    public NodeResponseParameters setNode(NodeInfo node) {
        putValue(NodeResponseParameters.node, node);
        return this;
    }

    /**
     * Returns the error message, if any.
     * @return the error message
     */
    public String getError() {
        return getString(error);
    }

    /**
     * Sets the error message.
     * @param error the error message to set
     * @return this instance
     */
    public NodeResponseParameters setError(String error) {
        putValue(NodeResponseParameters.error, error);
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

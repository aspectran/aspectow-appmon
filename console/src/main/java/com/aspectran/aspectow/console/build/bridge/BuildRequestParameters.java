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

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.apon.ValueType;
import com.aspectran.utils.apon.VariableParameters;
import com.aspectran.utils.json.JsonBuilder;

/**
 * Represents a structured request message for build and deployment execution.
 *
 * <p>Created: 2026-08-18</p>
 */
public class BuildRequestParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey executionId;
    public static final ParameterKey targetNodeId;
    public static final ParameterKey targetGroup;
    public static final ParameterKey targetAll;
    public static final ParameterKey targetServices;
    public static final ParameterKey scriptName;
    public static final ParameterKey parameters;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        executionId = new ParameterKey("executionId", ValueType.STRING);
        targetNodeId = new ParameterKey("targetNodeId", ValueType.STRING);
        targetGroup = new ParameterKey("targetGroup", ValueType.STRING);
        targetAll = new ParameterKey("targetAll", ValueType.BOOLEAN);
        targetServices = new ParameterKey("targetServices", ValueType.BOOLEAN);
        scriptName = new ParameterKey("scriptName", ValueType.STRING);
        parameters = new ParameterKey("parameters", VariableParameters.class);

        parameterKeys = new ParameterKey[] {
                header,
                executionId,
                targetNodeId,
                targetGroup,
                targetAll,
                targetServices,
                scriptName,
                parameters
        };
    }

    /**
     * Constructs a new BuildRequestParameters.
     */
    public BuildRequestParameters() {
        super(parameterKeys);
    }

    /**
     * Returns the message header/action name (e.g. "execute", "cancel", "join").
     * @return the message header
     */
    public String getHeader() {
        return getString(header);
    }

    /**
     * Sets the message header/action name.
     * @param value the message header
     * @return this parameters instance
     */
    public BuildRequestParameters setHeader(String value) {
        putValue(header, value);
        return this;
    }

    /**
     * Returns the execution ID.
     * @return the execution ID
     */
    public String getExecutionId() {
        return getString(executionId);
    }

    /**
     * Sets the execution ID.
     * @param value the execution ID
     * @return this parameters instance
     */
    public BuildRequestParameters setExecutionId(String value) {
        putValue(executionId, value);
        return this;
    }

    /**
     * Returns the target node ID.
     * @return the target node ID
     */
    public String getTargetNodeId() {
        return getString(targetNodeId);
    }

    /**
     * Sets the target node ID.
     * @param value the target node ID
     * @return this parameters instance
     */
    public BuildRequestParameters setTargetNodeId(String value) {
        putValue(targetNodeId, value);
        return this;
    }

    /**
     * Returns the target node group name.
     * @return the target group name
     */
    public String getTargetGroup() {
        return getString(targetGroup);
    }

    /**
     * Sets the target node group name.
     * @param value the target group name
     * @return this parameters instance
     */
    public BuildRequestParameters setTargetGroup(String value) {
        putValue(targetGroup, value);
        return this;
    }

    /**
     * Returns whether the target is all nodes in the cluster.
     * @return true if targeting all nodes; false otherwise
     */
    public boolean isTargetAll() {
        return getBoolean(targetAll, false);
    }

    /**
     * Sets whether the target is all nodes in the cluster.
     * @param value true to target all nodes
     * @return this parameters instance
     */
    public BuildRequestParameters setTargetAll(boolean value) {
        putValue(targetAll, value);
        return this;
    }

    /**
     * Returns whether the target is service nodes only.
     * @return true if targeting service nodes only; false otherwise
     */
    public boolean isTargetServices() {
        return getBoolean(targetServices, false);
    }

    /**
     * Sets whether the target is service nodes only.
     * @param value true to target service nodes only
     * @return this parameters instance
     */
    public BuildRequestParameters setTargetServices(boolean value) {
        putValue(targetServices, value);
        return this;
    }

    /**
     * Returns the build script name.
     * @return the script name
     */
    public String getScriptName() {
        return getString(scriptName);
    }

    /**
     * Sets the build script name.
     * @param value the script name
     * @return this parameters instance
     */
    public BuildRequestParameters setScriptName(String value) {
        putValue(scriptName, value);
        return this;
    }

    /**
     * Returns the extra parameters map.
     * @return the parameters
     */
    public Parameters getParameters() {
        return getParameters(parameters);
    }

    /**
     * Sets the extra parameters map.
     * @param value the parameters
     * @return this parameters instance
     */
    public BuildRequestParameters setParameters(Parameters value) {
        putValue(parameters, value);
        return this;
    }

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

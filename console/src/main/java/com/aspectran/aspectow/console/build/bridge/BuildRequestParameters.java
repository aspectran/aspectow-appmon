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
    public static final ParameterKey scriptName;
    public static final ParameterKey parameters;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        executionId = new ParameterKey("executionId", ValueType.STRING);
        targetNodeId = new ParameterKey("targetNodeId", ValueType.STRING);
        targetGroup = new ParameterKey("targetGroup", ValueType.STRING);
        targetAll = new ParameterKey("targetAll", ValueType.BOOLEAN);
        scriptName = new ParameterKey("scriptName", ValueType.STRING);
        parameters = new ParameterKey("parameters", VariableParameters.class);

        parameterKeys = new ParameterKey[] {
                header,
                executionId,
                targetNodeId,
                targetGroup,
                targetAll,
                scriptName,
                parameters
        };
    }

    public BuildRequestParameters() {
        super(parameterKeys);
    }

    public String getHeader() {
        return getString(header);
    }

    public BuildRequestParameters setHeader(String value) {
        putValue(header, value);
        return this;
    }

    public String getExecutionId() {
        return getString(executionId);
    }

    public BuildRequestParameters setExecutionId(String value) {
        putValue(executionId, value);
        return this;
    }

    public String getTargetNodeId() {
        return getString(targetNodeId);
    }

    public BuildRequestParameters setTargetNodeId(String value) {
        putValue(targetNodeId, value);
        return this;
    }

    public String getTargetGroup() {
        return getString(targetGroup);
    }

    public BuildRequestParameters setTargetGroup(String value) {
        putValue(targetGroup, value);
        return this;
    }

    public boolean isTargetAll() {
        return getBoolean(targetAll, false);
    }

    public BuildRequestParameters setTargetAll(boolean value) {
        putValue(targetAll, value);
        return this;
    }

    public String getScriptName() {
        return getString(scriptName);
    }

    public BuildRequestParameters setScriptName(String value) {
        putValue(scriptName, value);
        return this;
    }

    public Parameters getParameters() {
        return getParameters(parameters);
    }

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

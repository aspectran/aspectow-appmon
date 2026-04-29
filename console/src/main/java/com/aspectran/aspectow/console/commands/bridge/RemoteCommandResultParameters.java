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
package com.aspectran.aspectow.console.commands.bridge;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;
import com.aspectran.utils.json.JsonBuilder;

/**
 * Represents a structured outgoing message for remote command results.
 */
public class RemoteCommandResultParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey nodeId;
    public static final ParameterKey result;
    public static final ParameterKey error;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        nodeId = new ParameterKey("nodeId", ValueType.STRING);
        result = new ParameterKey("result", ValueType.TEXT);
        error = new ParameterKey("error", ValueType.TEXT);

        parameterKeys = new ParameterKey[] {
                header,
                nodeId,
                result,
                error
        };
    }

    public RemoteCommandResultParameters() {
        super(parameterKeys);
    }

    public RemoteCommandResultParameters setHeader(String headerValue) {
        putValue(header, headerValue);
        return this;
    }

    public RemoteCommandResultParameters setNodeId(String nodeIdValue) {
        putValue(nodeId, nodeIdValue);
        return this;
    }

    public RemoteCommandResultParameters setResult(String resultValue) {
        putValue(result, resultValue);
        return this;
    }

    public RemoteCommandResultParameters setError(String errorValue) {
        putValue(error, errorValue);
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

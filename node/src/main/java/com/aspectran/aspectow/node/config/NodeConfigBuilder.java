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
package com.aspectran.aspectow.node.config;

import com.aspectran.utils.Assert;
import com.aspectran.utils.ResourceUtils;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;

/**
 * A builder class for creating {@link NodeConfig} instances.
 * It provides methods to build configuration from default resources or external files.
 *
 * <p>Created: 2026-04-16</p>
 */
public abstract class NodeConfigBuilder {

    private static final String DEFAULT_NODE_CONFIG_RESOURCE = "com/aspectran/aspectow/node/config/node-config.apon";

    /**
     * Builds the default {@link NodeConfig} from the classpath resource.
     * @return a new {@link NodeConfig} instance
     * @throws IOException if the resource cannot be read
     */
    @NonNull
    public static NodeConfig build() throws IOException {
        try (Reader reader = ResourceUtils.getResourceAsReader(DEFAULT_NODE_CONFIG_RESOURCE)) {
            return new NodeConfig(reader);
        }
    }

    /**
     * Builds a {@link NodeConfig} from the specified URI location.
     * @param configLocation the URI of the configuration file
     * @param encoding the character encoding of the file
     * @return a new {@link NodeConfig} instance
     * @throws IOException if the resource cannot be read
     */
    @NonNull
    public static NodeConfig build(URI configLocation, String encoding) throws IOException {
        Assert.notNull(configLocation, "configLocation must not be null");
        try (Reader reader = ResourceUtils.getReader(configLocation.toURL(), encoding)) {
            return new NodeConfig(reader);
        }
    }

    /**
     * Builds a {@link NodeConfig} from the specified file.
     * @param configFile the configuration file
     * @return a new {@link NodeConfig} instance
     * @throws IOException if the file cannot be read
     */
    @NonNull
    public static NodeConfig build(File configFile) throws IOException {
        Assert.notNull(configFile, "configFile must not be null");
        return new NodeConfig(configFile);
    }

}

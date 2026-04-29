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

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Root configuration for the Aspectow Node Manager.
 * <p>It encapsulates cluster-wide settings and node information, serving
 * as the primary configuration entry point for managing cluster nodes.</p>
 */
public class NodeConfig extends DefaultParameters {

    private static final ParameterKey cluster;
    private static final ParameterKey node;

    private static final ParameterKey[] parameterKeys;

    static {
        cluster = new ParameterKey("cluster", ClusterConfig.class);
        node = new ParameterKey("node", NodeInfo.class, true, true);

        parameterKeys = new ParameterKey[] {
                cluster,
                node
        };
    }

    public NodeConfig() {
        super(parameterKeys);
    }

    public NodeConfig(Reader reader) throws IOException {
        this();
        readFrom(reader);
    }

    public NodeConfig(File configFile) throws IOException {
        this();
        readFrom(configFile);
    }

    /**
     * Returns the cluster-wide configuration.
     * @return the cluster configuration
     */
    public ClusterConfig getClusterConfig() {
        return getParameters(cluster);
    }

    /**
     * Returns the cluster configuration, creating it if it does not exist.
     * @return the cluster configuration
     */
    public ClusterConfig touchClusterConfig() {
        return touchParameters(cluster);
    }

    /**
     * Sets the cluster-wide configuration.
     * @param clusterConfig the cluster configuration
     */
    public void setClusterConfig(ClusterConfig clusterConfig) {
        putValue(cluster, clusterConfig);
    }

    /**
     * Returns a list of information for all registered nodes.
     * @return the list of node information
     */
    public List<NodeInfo> getNodeInfoList() {
        return getParametersList(node);
    }

    /**
     * Adds node information to the configuration.
     * @param nodeInfo the node information to add
     */
    public void putNodeInfo(NodeInfo nodeInfo) {
        putValue(node, nodeInfo);
    }

}

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
package com.aspectran.aspectow.node.manager;

import com.aspectran.aspectow.node.config.NodeInfo;

/**
 * Listener interface for receiving cluster node registry events
 * such as registration and unregistration.
 *
 * <p>Created: 2026-08-15</p>
 */
public interface NodeRegistryListener {

    /**
     * Called when a node is registered or re-registered in the Redis registry.
     * @param nodeInfo the information of the registered node
     */
    void onNodeRegistered(NodeInfo nodeInfo);

    /**
     * Called when a node is unregistered or evicted from the Redis registry.
     * @param nodeId the ID of the unregistered node
     */
    default void onNodeUnregistered(String nodeId) {
        // Default no-op implementation
    }

}

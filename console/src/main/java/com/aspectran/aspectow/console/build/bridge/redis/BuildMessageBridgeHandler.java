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
package com.aspectran.aspectow.console.build.bridge.redis;

import com.aspectran.aspectow.console.build.bridge.BuildDeployBroker;
import com.aspectran.aspectow.console.build.manager.RemoteBuildDeployManager;
import com.aspectran.aspectow.node.manager.NodeMessageListener;

/**
 * BuildMessageBridgeHandler listens to Redis build/deploy relay messages and
 * forwards them to RemoteBuildDeployManager.
 *
 * <p>Created: 2026-08-18</p>
 */
public class BuildMessageBridgeHandler implements NodeMessageListener {

    private final RemoteBuildDeployManager remoteBuildDeployManager;

    public BuildMessageBridgeHandler(RemoteBuildDeployManager remoteBuildDeployManager) {
        this.remoteBuildDeployManager = remoteBuildDeployManager;
    }

    @Override
    public String getCategory() {
        return BuildDeployBroker.CATEGORY_BUILD;
    }

    @Override
    public void onRelayMessage(String nodeId, String message) {
        remoteBuildDeployManager.process(message);
    }

}

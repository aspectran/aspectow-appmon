/*
 * Copyright (c) 2020-present The Aspectran Project
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

import com.aspectran.aspectow.node.config.NodeConfig;
import com.aspectran.aspectow.node.config.NodeConfigBuilder;
import com.aspectran.aspectow.node.config.NodeConfigResolver;
import com.aspectran.aspectow.node.redis.RedisConnectionPoolConfig;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableFactoryBean;
import com.aspectran.core.component.bean.aware.ActivityContextAware;
import com.aspectran.core.context.ActivityContext;
import org.jspecify.annotations.NonNull;

public class NodeManagerFactoryBean implements ActivityContextAware, InitializableFactoryBean<NodeManager>, DisposableBean {

    private ActivityContext context;

    private RedisConnectionPoolConfig redisConnectionPoolConfig;

    private NodeManager nodeManager;

    @Override
    public void setActivityContext(@NonNull ActivityContext context) {
        this.context = context;
    }

    public void setRedisConnectionPoolConfig(RedisConnectionPoolConfig redisConnectionPoolConfig) {
        this.redisConnectionPoolConfig = redisConnectionPoolConfig;
    }

    @Override
    public NodeManager getObject() {
        return nodeManager;
    }

    @Override
    public void initialize() throws Exception {
        NodeConfig nodeConfig;
        if (context.getBeanRegistry().containsBean(NodeConfigResolver.class)) {
            NodeConfigResolver nodeConfigResolver = context.getBeanRegistry().getBean(NodeConfigResolver.class);
            nodeConfig = nodeConfigResolver.resolveConfig();
        } else {
            nodeConfig = NodeConfigBuilder.build();
        }

        nodeManager = NodeManagerBuilder.build(context, nodeConfig, redisConnectionPoolConfig);
        if (nodeManager.getNodeReporter() != null) {
            nodeManager.getNodeReporter().start();
        }
        if (nodeManager.getRedisMessageSubscriber() != null) {
            nodeManager.getRedisMessageSubscriber().start();
        }
    }

    @Override
    public void destroy() throws Exception {
        if (nodeManager != null) {
            nodeManager.destroy();
        }
    }

}

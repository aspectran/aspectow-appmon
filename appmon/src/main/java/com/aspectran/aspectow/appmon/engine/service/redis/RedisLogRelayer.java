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
package com.aspectran.aspectow.appmon.engine.service.redis;

import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.aspectow.node.config.NodeConfig;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RedisLogRelayer listens to all log channels in the cluster and 
 * forwards the messages to the Console's ExportServiceManager.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component
public class RedisLogRelayer extends RedisPubSubAdapter<String, String> 
        implements InitializableBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(RedisLogRelayer.class);

    private static final String LOG_CHANNEL_PATTERN = "aspectow:cluster:logs:";

    private final String clusterName;

    private final RedisConnectionPool connectionPool;

    private final AppMonManager appMonManager;

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    @Autowired
    public RedisLogRelayer(NodeConfig nodeConfig, RedisConnectionPool connectionPool, AppMonManager appMonManager) {
        this.clusterName = nodeConfig.getClusterConfig().getName();
        this.connectionPool = connectionPool;
        this.appMonManager = appMonManager;
    }

    @Override
    public void initialize() throws Exception {
        this.pubSubConnection = connectionPool.getPubSubConnection();
        this.pubSubConnection.addListener(this);
        
        // Subscribe to all log channels for this cluster using pattern
        String pattern = LOG_CHANNEL_PATTERN + clusterName + ":*";
        this.pubSubConnection.sync().psubscribe(pattern);
        logger.info("RedisLogRelayer initialized and subscribed to pattern: {}", pattern);
    }

    @Override
    public void destroy() throws Exception {
        if (pubSubConnection != null) {
            pubSubConnection.removeListener(this);
            pubSubConnection.sync().punsubscribe();
            pubSubConnection.close();
        }
    }

    @Override
    public void message(String pattern, String channel, String message) {
        // Relay the message to the AppMon ExportServiceManager
        // Individual ExportServices will handle filtering based on their sessions
        appMonManager.getExportServiceManager().broadcast(message);
    }

}

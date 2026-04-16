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
import com.aspectran.aspectow.appmon.engine.service.ExportService;
import com.aspectran.aspectow.appmon.engine.service.ServiceSession;
import com.aspectran.aspectow.node.config.NodeConfig;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import io.lettuce.core.api.StatefulRedisConnection;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RedisExportService sends AppMon messages to a Redis Pub/Sub channel 
 * for remote monitoring via the Console.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component
public class RedisExportService implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(RedisExportService.class);

    private static final String LOG_CHANNEL_PREFIX = "aspectow:cluster:logs:";

    private final String clusterName;

    private final String nodeId;

    private final AppMonManager appMonManager;

    private final RedisConnectionPool connectionPool;

    private StatefulRedisConnection<String, String> connection;

    @Autowired
    public RedisExportService(NodeConfig nodeConfig, RedisConnectionPool connectionPool, AppMonManager appMonManager) {
        this.clusterName = nodeConfig.getClusterConfig().getName();
        this.nodeId = nodeConfig.getNodeInfo().getName();
        this.connectionPool = connectionPool;
        this.appMonManager = appMonManager;
    }

    @Initialize
    public void initialize() {
        this.connection = connectionPool.getConnection();
        appMonManager.getExportServiceManager().addExportService(this);
        logger.info("RedisExportService initialized and registered for node: {}", nodeId);
    }

    @Destroy
    public void destroy() {
        appMonManager.getExportServiceManager().removeExportService(this);
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public void broadcast(String message) {
        String channel = LOG_CHANNEL_PREFIX + clusterName + ":" + nodeId;
        if (connection != null) {
            connection.async().publish(channel, message);
        }
    }

    @Override
    public void broadcast(ServiceSession session, String message) {
        broadcast(message);
    }

    @Override
    public boolean isUsingInstance(String instanceName) {
        return true;
    }

}

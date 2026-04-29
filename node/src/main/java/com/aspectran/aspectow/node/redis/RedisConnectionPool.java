/*
 * Copyright (c) 2019-present The Aspectran Project
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
package com.aspectran.aspectow.node.redis;

import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.utils.Assert;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * Redis connection pool based on Lettuce.
 *
 * <p>Created: 2019/12/08</p>
 */
public class RedisConnectionPool implements InitializableBean, DisposableBean {

    private final RedisConnectionPoolConfig poolConfig;

    private RedisClient client;

    private GenericObjectPool<StatefulRedisConnection<String, String>> pool;

    public RedisConnectionPool(RedisConnectionPoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }

    public StatefulRedisConnection<String, String> getConnection() throws Exception {
        Assert.state(pool != null, "No RedisConnectionPool configured");
        return pool.borrowObject();
    }

    public StatefulRedisPubSubConnection<String, String> getPubSubConnection() {
        Assert.state(client != null, "No RedisClient configured");
        return client.connectPubSub();
    }

    @Override
    public void initialize() {
        Assert.state(client == null, "RedisConnectionPool is already initialized");
        RedisURI redisURI = poolConfig.getRedisURI();
        if (redisURI == null) {
            throw new IllegalArgumentException("redisURI must not be null");
        }
        client = RedisClient.create(redisURI);
        if (poolConfig.getClientOptions() != null) {
            client.setOptions(poolConfig.getClientOptions());
        }
        pool = ConnectionPoolSupport
                .createGenericObjectPool(()
                        -> client.connect(), poolConfig);
    }

    @Override
    public void destroy() {
        if (pool != null) {
            pool.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }

}

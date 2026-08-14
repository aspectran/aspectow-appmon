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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis connection pool based on Lettuce.
 *
 * <p>Created: 2019/12/08</p>
 */
public class RedisConnectionPool implements InitializableBean, DisposableBean {

    private final RedisConnectionPoolConfig poolConfig;

    private RedisClient client;

    private GenericObjectPool<StatefulRedisConnection<String, String>> pool;

    /**
     * Instantiates a new RedisConnectionPool with the specified configuration.
     * @param poolConfig the connection pool configuration
     */
    public RedisConnectionPool(RedisConnectionPoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }

    /**
     * Borrows a connection from the pool.
     * @return a stateful Redis connection
     * @throws Exception if a connection cannot be borrowed from the pool
     */
    public StatefulRedisConnection<String, String> getConnection() throws Exception {
        Assert.state(pool != null, "No RedisConnectionPool configured");
        return pool.borrowObject();
    }

    /**
     * Establishes a new pub/sub connection.
     * @return a stateful Redis pub/sub connection
     */
    public StatefulRedisPubSubConnection<String, String> getPubSubConnection() {
        Assert.state(client != null, "No RedisClient configured");
        return client.connectPubSub();
    }

    /**
     * Checks if the connection pool is active and available.
     * @return {@code true} if the pool is initialized and not closed, otherwise {@code false}
     */
    public boolean isAvailable() {
        return (pool != null && !pool.isClosed());
    }

    /**
     * Initializes the Redis client and connection pool.
     */
    @Override
    public void initialize() {
        Assert.state(client == null, "RedisConnectionPool is already initialized");
        RedisURI redisURI = poolConfig.getRedisURI();
        if (redisURI == null) {
            throw new IllegalArgumentException("redisURI must not be null");
        }
        if (redisURI.getTimeout() == null || redisURI.getTimeout().isZero() ||
                redisURI.getTimeout() == RedisURI.DEFAULT_TIMEOUT_DURATION) {
            redisURI.setTimeout(Duration.ofSeconds(5));
        }
        client = RedisClient.create(redisURI);
        if (poolConfig.getClientOptions() != null) {
            client.setOptions(poolConfig.getClientOptions());
        }
        pool = ConnectionPoolSupport
                .createGenericObjectPool(()
                        -> client.connect(), poolConfig);
    }

    /**
     * Closes the connection pool and shuts down the Redis client.
     */
    @Override
    public void destroy() {
        if (pool != null) {
            try {
                pool.close();
            } catch (Exception e) {
                // ignore
            }
            pool = null;
        }
        if (client != null) {
            try {
                client.shutdownAsync(0, 100, TimeUnit.MILLISECONDS).get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                // ignore
            }
            client = null;
        }
    }

}

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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe, lock-free Redis connection pool based on Lettuce multiplexing.
 * <p>Instead of relying on heavy pool synchronization (e.g. Apache Commons Pool2)
 * which causes severe lock contention under high concurrency (e.g. Java 21 Virtual Threads),
 * this implementation maintains a striped set of shared {@link StatefulRedisConnection}
 * instances. Each shared connection is wrapped in a proxy whose {@code close()} method
 * is a no-op, allowing callers to use standard {@code try-with-resources} blocks without
 * closing the underlying multiplexed socket connections.</p>
 *
 * <p>Created: 2019/12/08</p>
 */
public class RedisConnectionPool implements InitializableBean, DisposableBean {

    private final RedisConnectionPoolConfig poolConfig;

    private RedisClient client;

    private StatefulRedisConnection<String, String>[] sharedConnections;

    private StatefulRedisConnection<String, String>[] proxyConnections;

    private final AtomicInteger connectionIndex = new AtomicInteger();

    /**
     * Instantiates a new RedisConnectionPool with the specified configuration.
     * @param poolConfig the connection pool configuration
     */
    public RedisConnectionPool(RedisConnectionPoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }

    /**
     * Obtains a shared stateful Redis connection from the striped pool.
     * <p>The returned connection is thread-safe, multiplexed, and wrapped in a proxy
     * whose {@code close()} method is a no-op so that callers using
     * {@code try-with-resources} will not close the underlying socket connection.</p>
     * @return a thread-safe, shared stateful Redis connection
     * @throws Exception if the connection pool is not initialized
     */
    public StatefulRedisConnection<String, String> getConnection() throws Exception {
        Assert.state(proxyConnections != null && proxyConnections.length > 0, "RedisConnectionPool is not initialized");
        int idx = (connectionIndex.getAndIncrement() & 0x7FFFFFFF) % proxyConnections.length;
        return proxyConnections[idx];
    }

    /**
     * Establishes and returns a new dedicated, unpooled Redis connection.
     * <p>The caller is responsible for closing this connection (e.g. via {@code try-with-resources}).
     * Useful for transactions (MULTI/EXEC) or blocking commands where isolation is required.</p>
     * @return a new dedicated stateful Redis connection
     */
    public StatefulRedisConnection<String, String> getDedicatedConnection() {
        Assert.state(client != null, "RedisConnectionPool is not initialized");
        return client.connect();
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
     * @return {@code true} if the pool is initialized and at least one shared connection is open, otherwise {@code false}
     */
    public boolean isAvailable() {
        if (client == null || sharedConnections == null || sharedConnections.length == 0) {
            return false;
        }
        for (StatefulRedisConnection<String, String> connection : sharedConnections) {
            if (connection != null && connection.isOpen()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Initializes the Redis client and striped shared connections.
     */
    @Override
    @SuppressWarnings("unchecked")
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

        int poolSize = poolConfig.getMinIdle();
        if (poolSize <= 0) {
            poolSize = 8;
        }
        poolSize = Math.clamp(poolSize, 2, 32);

        sharedConnections = (StatefulRedisConnection<String, String>[]) new StatefulRedisConnection<?, ?>[poolSize];
        proxyConnections = (StatefulRedisConnection<String, String>[]) new StatefulRedisConnection<?, ?>[poolSize];
        for (int i = 0; i < poolSize; i++) {
            sharedConnections[i] = client.connect();
            proxyConnections[i] = wrapSharedConnection(sharedConnections[i]);
        }
    }

    /**
     * Closes all shared connections and shuts down the Redis client.
     */
    @Override
    public void destroy() {
        if (sharedConnections != null) {
            for (StatefulRedisConnection<String, String> connection : sharedConnections) {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            sharedConnections = null;
            proxyConnections = null;
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

    @SuppressWarnings("unchecked")
    @NonNull
    private static StatefulRedisConnection<String, String> wrapSharedConnection(
            StatefulRedisConnection<String, String> connection) {
        return (StatefulRedisConnection<String, String>) Proxy.newProxyInstance(
                StatefulRedisConnection.class.getClassLoader(),
                new Class<?>[] { StatefulRedisConnection.class },
                new SharedConnectionInvocationHandler(connection)
        );
    }

    private static class SharedConnectionInvocationHandler implements InvocationHandler {

        private final StatefulRedisConnection<String, String> delegate;

        SharedConnectionInvocationHandler(StatefulRedisConnection<String, String> delegate) {
            this.delegate = delegate;
        }

        @Override
        @Nullable
        public Object invoke(Object proxy, @NonNull Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if ("close".equals(methodName) && (args == null || args.length == 0)) {
                // No-op: keep the shared connection open
                return null;
            }
            if ("closeAsync".equals(methodName) && (args == null || args.length == 0)) {
                return CompletableFuture.completedFuture(null);
            }
            if ("isOpen".equals(methodName) && (args == null || args.length == 0)) {
                return delegate.isOpen();
            }
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

    }

}

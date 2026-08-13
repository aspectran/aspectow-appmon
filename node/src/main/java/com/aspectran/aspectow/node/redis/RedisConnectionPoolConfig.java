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

import com.aspectran.utils.StringUtils;
import com.aspectran.utils.ToStringBuilder;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Properties;

/**
 * Configuration holder for a Lettuce-backed Redis connection pool used by Aspectow Node Manager.
 * <p>Manages Redis endpoint URIs, timeouts, client options, and pooling parameters.</p>
 *
 * <h2>Supported Configuration Properties (via properties or bean setters)</h2>
 * <ul>
 *   <li>{@code aspectow.redis.uri} / {@code uri} — Target Redis connection URI (e.g., {@code "redis://10.0.0.3:6379/5"})</li>
 *   <li>{@code aspectow.redis.timeout} / {@code timeout} — Redis command/connection timeout (e.g., {@code "5s"}, {@code "5000ms"})</li>
 * </ul>
 *
 * <h2>Example Aspectran Bean Definition</h2>
 * <pre>{@code
 * <bean id="redisConnectionPoolConfig" class="com.aspectran.aspectow.node.redis.RedisConnectionPoolConfig">
 *     <property name="timeout" value="5s"/>
 *     <argument>
 *         <bean class="com.aspectran.core.support.PropertiesFactoryBean">
 *             <properties profile="prod">
 *                 <item name="locations" type="array">
 *                     <value>/config/console/redis-prod.properties</value>
 *                 </item>
 *             </properties>
 *         </bean>
 *     </argument>
 * </bean>
 * }</pre>
 *
 * <p>Created: 2019/12/07</p>
 */
public class RedisConnectionPoolConfig extends GenericObjectPoolConfig<StatefulRedisConnection<String, String>> {

    private RedisURI redisURI;

    private ClientOptions clientOptions;

    /**
     * Instantiates a new RedisConnectionPoolConfig with default settings.
     */
    public RedisConnectionPoolConfig() {
        super();
    }

    /**
     * Instantiates a new RedisConnectionPoolConfig using the specified properties.
     * @param properties the properties containing configuration values
     */
    public RedisConnectionPoolConfig(@NonNull Properties properties) {
        super();
        setUri(properties.getProperty("aspectow.redis.uri"));
        String timeout = properties.getProperty("aspectow.redis.timeout");
        if (StringUtils.hasText(timeout)) {
            setTimeout(timeout);
        }
    }

    /**
     * Returns the Redis URI for connection.
     * @return the Redis URI
     */
    public RedisURI getRedisURI() {
        return redisURI;
    }

    /**
     * Sets the Redis URI for connection.
     * @param redisURI the Redis URI
     * @throws IllegalArgumentException if the {@code redisURI} is null
     */
    public void setRedisURI(RedisURI redisURI) {
        if (redisURI == null) {
            throw new IllegalArgumentException("redisURI must not be null");
        }
        this.redisURI = redisURI;
    }

    /**
     * Sets the connection URI for Redis.
     * @param uri the URI string
     * @throws IllegalArgumentException if the {@code uri} is null or empty
     */
    public void setUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            throw new IllegalArgumentException("uri must not be null or empty");
        }
        this.redisURI = RedisURI.create(uri);
    }

    /**
     * Sets the connection timeout for Redis.
     * @param timeout the duration timeout
     */
    public void setTimeout(Duration timeout) {
        if (this.redisURI != null && timeout != null) {
            this.redisURI.setTimeout(timeout);
        }
    }

    /**
     * Sets the connection timeout for Redis as a string (e.g. "5s", "5000ms").
     * @param timeout the timeout string
     */
    public void setTimeout(String timeout) {
        if (StringUtils.hasText(timeout)) {
            setTimeout(parseDuration(timeout));
        }
    }

    private static Duration parseDuration(@NonNull String text) {
        String trimmed = text.trim().toLowerCase();
        try {
            if (trimmed.endsWith("ms")) {
                long ms = Long.parseLong(trimmed.substring(0, trimmed.length() - 2).trim());
                return Duration.ofMillis(ms);
            } else if (trimmed.endsWith("s")) {
                long s = Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim());
                return Duration.ofSeconds(s);
            } else if (trimmed.endsWith("m")) {
                long m = Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim());
                return Duration.ofMinutes(m);
            } else {
                long s = Long.parseLong(trimmed);
                return Duration.ofSeconds(s);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid duration string: " + text, e);
        }
    }

    /**
     * Returns the Lettuce client options.
     * @return the client options, or {@code null} if not configured
     */
    public ClientOptions getClientOptions() {
        return clientOptions;
    }

    /**
     * Sets the Lettuce client options.
     * @param clientOptions the client options to set
     */
    public void setClientOptions(ClientOptions clientOptions) {
        this.clientOptions = clientOptions;
    }

    /**
     * Returns a string representation of the Redis connection pool configuration.
     * @return a string representation of the configuration
     */
    @Override
    public String toString() {
        ToStringBuilder tsb = new ToStringBuilder();
        tsb.append("redisURI", redisURI);
        tsb.append("clientOptions", clientOptions);
        return tsb.toString();
    }

}

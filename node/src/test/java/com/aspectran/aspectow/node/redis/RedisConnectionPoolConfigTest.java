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
package com.aspectran.aspectow.node.redis;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RedisConnectionPoolConfigTest {

    @Test
    void testDefaults() {
        RedisConnectionPoolConfig config = new RedisConnectionPoolConfig();
        assertEquals(64, config.getMaxTotal());
        assertEquals(32, config.getMaxIdle());
        assertEquals(8, config.getMinIdle());
        assertEquals(Duration.ofSeconds(5), config.getMaxWaitDuration());
    }

    @Test
    void testFromProperties() {
        Properties props = new Properties();
        props.setProperty("aspectow.redis.uri", "redis://localhost:6379/0");
        props.setProperty("aspectow.redis.timeout", "10s");
        props.setProperty("aspectow.redis.pool.maxTotal", "128");
        props.setProperty("aspectow.redis.pool.maxIdle", "64");
        props.setProperty("aspectow.redis.pool.minIdle", "16");
        props.setProperty("aspectow.redis.pool.maxWait", "2500ms");

        RedisConnectionPoolConfig config = new RedisConnectionPoolConfig(props);
        assertNotNull(config.getRedisURI());
        assertEquals("localhost", config.getRedisURI().getHost());
        assertEquals(6379, config.getRedisURI().getPort());
        assertEquals(Duration.ofSeconds(10), config.getRedisURI().getTimeout());
        assertEquals(128, config.getMaxTotal());
        assertEquals(64, config.getMaxIdle());
        assertEquals(16, config.getMinIdle());
        assertEquals(Duration.ofMillis(2500), config.getMaxWaitDuration());
    }

    @Test
    void testStringSetters() {
        RedisConnectionPoolConfig config = new RedisConnectionPoolConfig();
        config.setMaxTotal("100");
        config.setMaxIdle("50");
        config.setMinIdle("10");
        config.setMaxWait("8s");

        assertEquals(100, config.getMaxTotal());
        assertEquals(50, config.getMaxIdle());
        assertEquals(10, config.getMinIdle());
        assertEquals(Duration.ofSeconds(8), config.getMaxWaitDuration());
    }

}

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
        assertEquals(8, config.getPoolSize());
    }

    @Test
    void testFromProperties() {
        Properties props = new Properties();
        props.setProperty("aspectow.redis.uri", "redis://localhost:6379/0");
        props.setProperty("aspectow.redis.timeout", "10s");
        props.setProperty("aspectow.redis.pool.size", "16");

        RedisConnectionPoolConfig config = new RedisConnectionPoolConfig(props);
        assertNotNull(config.getRedisURI());
        assertEquals("localhost", config.getRedisURI().getHost());
        assertEquals(6379, config.getRedisURI().getPort());
        assertEquals(Duration.ofSeconds(10), config.getRedisURI().getTimeout());
        assertEquals(16, config.getPoolSize());
    }

    @Test
    void testStringSetters() {
        RedisConnectionPoolConfig config = new RedisConnectionPoolConfig();
        config.setPoolSize("16");
        assertEquals(16, config.getPoolSize());
    }

}

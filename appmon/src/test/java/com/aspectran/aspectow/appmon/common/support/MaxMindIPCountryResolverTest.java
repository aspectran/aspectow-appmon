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
package com.aspectran.aspectow.appmon.common.support;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test cases for {@link MaxMindIPCountryResolver}.
 *
 * <p>Created: 2026-09-06</p>
 */
class MaxMindIPCountryResolverTest {

    @Test
    void resolveCountryCode_withPrivateOrLocalIps_returnsLocaleCountry() {
        MaxMindIPCountryResolver resolver = new MaxMindIPCountryResolver();

        assertEquals("KR", resolver.resolveCountryCode("127.0.0.1", Locale.KOREA));
        assertEquals("KR", resolver.resolveCountryCode("10.0.0.1", Locale.KOREA));
        assertEquals("KR", resolver.resolveCountryCode("192.168.1.1", Locale.KOREA));
        assertEquals("KR", resolver.resolveCountryCode("172.20.0.1", Locale.KOREA));
        assertEquals("KR", resolver.resolveCountryCode("169.254.0.1", Locale.KOREA));
        assertEquals("KR", resolver.resolveCountryCode("localhost", Locale.KOREA));
        assertEquals("KR", resolver.resolveCountryCode("0000:0000:0000:0000:0000:0000:0000:0001", Locale.KOREA));
        assertEquals("KR", resolver.resolveCountryCode("::1", Locale.KOREA));

        assertNull(resolver.resolveCountryCode("127.0.0.1", null));
        assertNull(resolver.resolveCountryCode("10.0.0.1"));
    }

    @Test
    void resolveCountryCode_withoutDatabase_returnsLocaleCountry() {
        MaxMindIPCountryResolver resolver = new MaxMindIPCountryResolver();

        assertEquals("US", resolver.resolveCountryCode("1.1.1.1", Locale.US));
        assertEquals("JP", resolver.resolveCountryCode("8.8.8.8", Locale.JAPAN));
        assertNull(resolver.resolveCountryCode("1.1.1.1"));
    }

    @Test
    void resolveCountryCode_withNonExistentDatabase_gracefullyDegrades() {
        MaxMindIPCountryResolver resolver = new MaxMindIPCountryResolver("/non/existent/GeoLite2-Country.mmdb");

        assertEquals("KR", resolver.resolveCountryCode("1.1.1.1", Locale.KOREA));
        assertNull(resolver.resolveCountryCode("1.1.1.1", null));
    }

    @Test
    void resolveCountryCode_withClasspathLocation_gracefullyDegradesWhenNotFound() {
        MaxMindIPCountryResolver resolver = new MaxMindIPCountryResolver("classpath:non-existent.mmdb");

        assertEquals("KR", resolver.resolveCountryCode("1.1.1.1", Locale.KOREA));
        assertNull(resolver.resolveCountryCode("1.1.1.1", null));
    }

    @Test
    void resolveCountryCode_withRealTestMmdb() {
        MaxMindIPCountryResolver resolver = new MaxMindIPCountryResolver(
                "classpath:com/aspectran/aspectow/appmon/common/support/GeoIP2-Country-Test.mmdb");

        // MaxMind test database sample IPs (IPv4 & IPv6)
        assertEquals("GB", resolver.resolveCountryCode("81.2.69.160"));
        assertEquals("US", resolver.resolveCountryCode("216.160.83.56"));
        assertEquals("SE", resolver.resolveCountryCode("89.160.20.128"));
        assertEquals("KR", resolver.resolveCountryCode("2001:230::1"));
        assertEquals("JP", resolver.resolveCountryCode("2001:218::1"));

        // Cache hit
        assertEquals("GB", resolver.resolveCountryCode("81.2.69.160"));

        // Unmatched IP falls back to locale
        assertEquals("KR", resolver.resolveCountryCode("0.0.0.0", Locale.KOREA));
        assertNull(resolver.resolveCountryCode("0.0.0.0"));

        resolver.destroy();
    }

    @Test
    void lifecycle_setDatabasePathAndDestroy() {
        MaxMindIPCountryResolver resolver = new MaxMindIPCountryResolver();
        resolver.setDatabasePath(null);
        resolver.setMaxCacheSize(1024);
        assertEquals(1024, resolver.getMaxCacheSize());
        resolver.destroy();
    }

}

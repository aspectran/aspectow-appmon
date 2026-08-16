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

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Strategy interface for resolving an ISO 3166-1 alpha-2 country code
 * (e.g., "KR", "US", "JP") from a client IP address.
 *
 * <p>Implementations may utilize local GeoIP databases (such as MaxMind GeoIP2),
 * external OpenAPI services (such as WHOIS), or custom resolution mechanisms.</p>
 *
 * <p>Created: 2026-08-16</p>
 */
public interface IPCountryResolver {

    /**
     * Resolves the country code for the given IP address and optional locale.
     * @param ipAddress the client IP address (IPv4 or IPv6)
     * @param locale the request locale for fallback or resolution assistance (optional)
     * @return the 2-letter country code in uppercase, or {@code null} if resolution fails
     */
    @Nullable
    String resolveCountryCode(String ipAddress, @Nullable Locale locale);

    /**
     * Resolves the country code using only the IP address.
     * @param ipAddress the client IP address
     * @return the 2-letter country code in uppercase, or {@code null} if resolution fails
     */
    @Nullable
    default String resolveCountryCode(String ipAddress) {
        return resolveCountryCode(ipAddress, null);
    }

}

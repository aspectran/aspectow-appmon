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

import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.SystemUtils;
import com.aspectran.utils.cache.Cache;
import com.aspectran.utils.cache.ConcurrentLruCache;
import com.aspectran.utils.net.IpAddressUtils;
import com.maxmind.db.Reader;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MaxMind GeoIP2 / GeoLite2 MMDB database based {@link IPCountryResolver} implementation.
 *
 * <p>Reads country data directly from a local MMDB database file (e.g. {@code GeoLite2-Country.mmdb}
 * or {@code GeoLite2-City.mmdb}) using {@link Reader.FileMode#MEMORY_MAPPED} mode for zero-copy,
 * high-performance lookups with minimal JVM heap footprint.</p>
 *
 * <p>Created: 2026-09-06</p>
 */
public class MaxMindIPCountryResolver implements IPCountryResolver, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(MaxMindIPCountryResolver.class);

    private static final String NONE = "(none)";

    private static final String FAILED = "(failed)";

    private static final int DEFAULT_MAX_CACHE_SIZE = 2048;

    private static final List<String> iso2CountryCodes;

    private String databasePath;

    private int maxCacheSize;

    private Reader reader;

    private Cache<String, String> cache;

    static {
        iso2CountryCodes = List.of(Locale.getISOCountries());
    }

    public MaxMindIPCountryResolver() {
        this(resolveDefaultDatabasePath(), DEFAULT_MAX_CACHE_SIZE);
    }

    public MaxMindIPCountryResolver(String databasePath) {
        this(databasePath, DEFAULT_MAX_CACHE_SIZE);
    }

    public MaxMindIPCountryResolver(String databasePath, int maxCacheSize) {
        Assert.isTrue(maxCacheSize > 0, "maxCacheSize must be positive");
        this.maxCacheSize = maxCacheSize;
        setDatabasePath(databasePath);
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
        closeReader();
        if (StringUtils.hasText(databasePath)) {
            File dbFile = new File(databasePath);
            if (dbFile.isFile()) {
                try {
                    this.reader = new Reader(dbFile, Reader.FileMode.MEMORY_MAPPED);
                    if (logger.isInfoEnabled()) {
                        logger.info("Initialized MaxMind GeoIP database from {}", dbFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    logger.error("Failed to load MaxMind GeoIP database from {}", dbFile.getAbsolutePath(), e);
                    this.reader = null;
                }
            } else {
                logger.warn("MaxMind GeoIP database file not found: {}", databasePath);
                this.reader = null;
            }
            if (cache != null) {
                cache.clear();
            }
            cache = new ConcurrentLruCache<>(maxCacheSize, this::getCountryCode);
        } else {
            if (cache != null) {
                cache.clear();
                cache = null;
            }
        }
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    public void setMaxCacheSize(int maxCacheSize) {
        Assert.isTrue(maxCacheSize > 0, "maxCacheSize must be positive");
        this.maxCacheSize = maxCacheSize;
        if (StringUtils.hasText(databasePath)) {
            if (cache != null) {
                cache.clear();
            }
            cache = new ConcurrentLruCache<>(maxCacheSize, this::getCountryCode);
        }
    }

    @Override
    public void destroy() {
        closeReader();
        if (cache != null) {
            cache.clear();
            cache = null;
        }
    }

    private void closeReader() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                logger.warn("Failed to close MaxMind database reader", e);
            }
            reader = null;
        }
    }

    @Override
    @Nullable
    public String resolveCountryCode(String ipAddress, @Nullable Locale locale) {
        Assert.notNull(ipAddress, "ipAddress must not be null");

        String ip6 = IpAddressUtils.normalizeIPv6(ipAddress);
        if (ip6 != null) {
            ipAddress = ip6;
        }

        if (cache == null || reader == null || isPrivateOrLocalIp(ipAddress)) {
            return getCountryCode(locale);
        }

        String countryCode = cache.get(ipAddress);
        if (countryCode == null || NONE.equals(countryCode) || FAILED.equals(countryCode)) {
            countryCode = getCountryCode(locale);
        }
        return countryCode;
    }

    private String getCountryCode(String ipAddress) {
        if (reader == null) {
            return FAILED;
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            Map<?, ?> record = reader.get(inetAddress, Map.class);
            if (record != null) {
                Object countryObj = record.get("country");
                if (countryObj instanceof Map<?, ?> countryMap) {
                    Object isoCode = countryMap.get("iso_code");
                    if (isoCode instanceof String code && iso2CountryCodes.contains(code)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Country code of IP address {} is {}", ipAddress, code);
                        }
                        return code;
                    }
                }
                Object regCountryObj = record.get("registered_country");
                if (regCountryObj instanceof Map<?, ?> regCountryMap) {
                    Object isoCode = regCountryMap.get("iso_code");
                    if (isoCode instanceof String code && iso2CountryCodes.contains(code)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Country code of IP address {} is {}", ipAddress, code);
                        }
                        return code;
                    }
                }
            }
            return NONE;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("MaxMind lookup failed for IP {}: {}", ipAddress, e.getMessage());
            }
            return FAILED;
        }
    }

    private static boolean isPrivateOrLocalIp(@NonNull String ip) {
        return ip.equals("127.0.0.1") ||
                ip.startsWith("127.") ||
                ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("169.254.") ||
                ip.equals("localhost") ||
                ip.equals("0000:0000:0000:0000:0000:0000:0000:0001") ||
                ip.equals("0000:0000:0000:0000:0000:0000:0000:0000") ||
                ip.startsWith("fe80:") ||
                (ip.startsWith("172.") && is172Private(ip));
    }

    private static boolean is172Private(@NonNull String ip) {
        int secondDot = ip.indexOf('.', 4);
        if (secondDot > 4) {
            try {
                int secondOctet = Integer.parseInt(ip.substring(4, secondDot));
                return secondOctet >= 16 && secondOctet <= 31;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    @Nullable
    private String getCountryCode(Locale locale) {
        return (locale != null ? locale.getCountry() : null);
    }

    @Nullable
    private static String resolveDefaultDatabasePath() {
        String path = SystemUtils.getProperty("geolite2.db.path");
        if (!StringUtils.hasText(path)) {
            path = SystemUtils.getProperty("maxmind.db.path");
        }
        return (StringUtils.hasText(path) ? path : null);
    }

}

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
import com.aspectran.utils.ResourceUtils;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MaxMind GeoIP2 / GeoLite2 MMDB database based {@link IPCountryResolver} implementation.
 *
 * <p>Reads country data directly from an MMDB database file (e.g. {@code GeoLite2-Country.mmdb}
 * or {@code GeoLite2-City.mmdb}) located on the file system or in the classpath
 * (e.g. {@code classpath:GeoLite2-Country.mmdb}).</p>
 *
 * <p>Uses {@link Reader.FileMode#MEMORY_MAPPED} mode for file system resources for zero-copy,
 * high-performance lookups with minimal JVM heap footprint, or loads via stream when packaged
 * inside a JAR.</p>
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
            try {
                this.reader = createReader(databasePath);
                if (logger.isInfoEnabled()) {
                    logger.info("Initialized MaxMind GeoIP database from {}", databasePath);
                }
            } catch (Exception e) {
                logger.warn("Failed to load MaxMind GeoIP database from {}: {}", databasePath, e.getMessage());
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

    @NonNull
    private Reader createReader(@NonNull String location) throws IOException {
        // 1. Explicit classpath resource
        if (location.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
            URL url = ResourceUtils.getURL(location);
            try {
                File file = ResourceUtils.getFile(url);
                if (file.isFile()) {
                    return new Reader(file, Reader.FileMode.MEMORY_MAPPED);
                }
            } catch (FileNotFoundException ignored) {
                // Inside a JAR or not directly in file system
            }
            try (InputStream in = url.openStream()) {
                return new Reader(in);
            }
        }

        // 2. Explicit file: URL
        if (location.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
            File file = ResourceUtils.getFile(location);
            return new Reader(file, Reader.FileMode.MEMORY_MAPPED);
        }

        // 3. Regular file system path
        File file = new File(location);
        if (file.isFile()) {
            return new Reader(file, Reader.FileMode.MEMORY_MAPPED);
        }

        // 4. Fallback: try loading from classpath if not found as a regular file
        try {
            URL url = ResourceUtils.getURL(ResourceUtils.CLASSPATH_URL_PREFIX + location);
            try {
                File classPathFile = ResourceUtils.getFile(url);
                if (classPathFile.isFile()) {
                    return new Reader(classPathFile, Reader.FileMode.MEMORY_MAPPED);
                }
            } catch (FileNotFoundException ignored) {
            }
            try (InputStream in = url.openStream()) {
                return new Reader(in);
            }
        } catch (Exception ignored) {
            // Ignore fallback failure and throw original not found
        }

        throw new FileNotFoundException("MaxMind GeoIP database not found at " + location);
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

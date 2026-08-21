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
package com.aspectran.aspectow.console.auth;

import com.aspectran.core.activity.Translet;
import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.apon.VariableParameters;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.utils.security.TimeLimitedPBTokenIssuer;
import org.jspecify.annotations.Nullable;

/**
 * A utility class for issuing and validating time-limited authentication tokens
 * for the Aspectow Console.
 *
 * <p>Created: 2026-07-12</p>
 */
public final class ConsoleTokenIssuer {

    private static final String MAX_AGE_PARAM = "maxAgeInSeconds";

    private static final String IS_DEMO_PARAM = "isDemo";

    private static final String USERNAME_PARAM = "username";

    private static final int DEFAULT_MAX_AGE = 1800; // 30 minutes

    private ConsoleTokenIssuer() {
    }

    /**
     * Issues a time-limited token with a specified expiration time.
     * @param maxAgeInSeconds the maximum age of the token in seconds
     * @return the generated token
     */
    public static String issueToken(int maxAgeInSeconds) {
        return issueToken(maxAgeInSeconds, null, false);
    }

    /**
     * Issues a time-limited token with a specified expiration time and demo flag.
     * @param maxAgeInSeconds the maximum age of the token in seconds
     * @param isDemo the flag indicating if the token is for a demo user
     * @return the generated token
     */
    public static String issueToken(int maxAgeInSeconds, boolean isDemo) {
        return issueToken(maxAgeInSeconds, null, isDemo);
    }

    /**
     * Issues a time-limited token using the user information extracted from the given Translet.
     * @param translet the current translet
     * @return the generated token
     */
    public static String issueToken(Translet translet) {
        return issueToken(DEFAULT_MAX_AGE, translet);
    }

    /**
     * Issues a time-limited token with a specified expiration time using the user information extracted from the given Translet.
     * @param maxAgeInSeconds the maximum age of the token in seconds
     * @param translet the current translet
     * @return the generated token
     */
    public static String issueToken(int maxAgeInSeconds, Translet translet) {
        String username = null;
        boolean isDemo = false;
        if (translet != null && translet.getSessionAdapter() != null) {
            UserInfo userInfo = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
            if (userInfo != null) {
                username = userInfo.getUsername();
                isDemo = userInfo.hasRole("DEMO");
            }
        }
        return issueToken(maxAgeInSeconds, username, isDemo);
    }

    /**
     * Issues a time-limited token with a specified expiration time, demo flag, and username.
     * @param maxAgeInSeconds the maximum age of the token in seconds
     * @param username the username of the authenticated user
     * @param isDemo the flag indicating if the token is for a demo user
     * @return the generated token
     */
    public static String issueToken(int maxAgeInSeconds, String username, boolean isDemo) {
        Parameters payload = new VariableParameters();
        payload.putValue(MAX_AGE_PARAM, maxAgeInSeconds);
        if (username != null) {
            payload.putValue(USERNAME_PARAM, username);
        }
        payload.putValue(IS_DEMO_PARAM, isDemo);
        return TimeLimitedPBTokenIssuer.createToken(payload, 1000L * maxAgeInSeconds);
    }

    /**
     * Validates the given time-limited token and returns the max age in seconds.
     * If 'maxAgeInSeconds' is not specified in the token, the default is 30 minutes.
     * @param token the token to validate
     * @return the max age in seconds if the token is valid
     * @throws InvalidPBTokenException if the token is invalid or expired
     */
    public static int validateToken(String token) throws InvalidPBTokenException {
        Parameters payload = TimeLimitedPBTokenIssuer.parseToken(token);
        if (payload == null) {
            return DEFAULT_MAX_AGE;
        }
        Integer maxAgeInSeconds = payload.getInt(MAX_AGE_PARAM);
        if (maxAgeInSeconds == null) {
            return DEFAULT_MAX_AGE;
        }
        return maxAgeInSeconds;
    }

    /**
     * Extracts the username from the given token if available.
     * @param token the token to check
     * @return the username, or null if not found
     */
    @Nullable
    public static String getUsername(String token) {
        try {
            Parameters payload = TimeLimitedPBTokenIssuer.parseToken(token);
            if (payload != null) {
                return payload.getString(USERNAME_PARAM);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * Checks if the given token is issued for a demo user.
     * @param token the token to check
     * @return {@code true} if the token is for a demo user; {@code false} otherwise
     */
    public static boolean isDemoToken(String token) {
        try {
            Parameters payload = TimeLimitedPBTokenIssuer.parseToken(token);
            if (payload != null) {
                Boolean isDemo = payload.getBoolean(IS_DEMO_PARAM);
                return (isDemo != null && isDemo);
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

}

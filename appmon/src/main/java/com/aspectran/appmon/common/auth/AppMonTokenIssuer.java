/*
 * Copyright (c) 2008-2024 The Aspectran Project
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
package com.aspectran.appmon.common.auth;

import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.apon.VariableParameters;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.utils.security.TimeLimitedPBTokenIssuer;

/**
 * A utility class for issuing and validating time-limited authentication tokens
 * for the Application Monitor.
 *
 * <p>Created: 2025-11-04</p>
 */
public final class AppMonTokenIssuer {

    private static final String MAX_AGE_PARAM = "maxAgeInSeconds";

    private static final int DEFAULT_MAX_AGE = 1800; // 30 minutes

    private AppMonTokenIssuer() {
    }

    /**
     * Issues a time-limited token with a specified expiration time.
     * @param maxAgeInSeconds the maximum age of the token in seconds
     * @return the generated token
     */
    public static String issueToken(int maxAgeInSeconds) {
        Parameters payload = new VariableParameters();
        payload.putValue(MAX_AGE_PARAM, maxAgeInSeconds);
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

}

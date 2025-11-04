package com.aspectran.appmon.common.auth;

import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.utils.security.TimeLimitedPBTokenIssuer;

/**
 * <p>Created: 2025-11-04</p>
 */
public class AppMonTokenIssuer {

    /**
     * Issues a time-limited token with a default expiration of 60 seconds.
     * @return the generated token
     */
    public static String issueToken() {
        return issueToken(60); // default 60 secs.
    }

    /**
     * Issues a time-limited token with a specified expiration time.
     * @param expirationTimeInSeconds the expiration time in seconds
     * @return the generated token
     */
    public static String issueToken(int expirationTimeInSeconds) {
        return TimeLimitedPBTokenIssuer.getToken(1000L * expirationTimeInSeconds);
    }

    /**
     * Validates the given time-limited token.
     * @param token the token to validate
     * @throws InvalidPBTokenException if the token is invalid or expired
     */
    public static void validateToken(String token) throws InvalidPBTokenException {
        TimeLimitedPBTokenIssuer.validate(token);
    }

}

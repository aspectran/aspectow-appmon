/*
 * Copyright (c) 2008-2024 The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aspectran.appmon.common.auth;

import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.support.util.WebUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;

/**
 * Manages the authentication token cookie for the Application Monitor.
 *
 * <p>Created: 2025-11-04</p>
 */
@Component
@Bean("appMonCookieIssuer")
public final class AppMonCookieIssuer {

    private static final String AUTH_TOKEN_NAME = "appmon-auth-token";

    /**
     * Issues a new authentication token cookie.
     * @param translet the current translet
     * @param contextPath the context path for the cookie
     * @param maxAgeInSeconds the maximum age of the cookie in seconds
     */
    public void issueCookie(@NonNull Translet translet, String contextPath, int maxAgeInSeconds) {
        String token = AppMonTokenIssuer.issueToken(maxAgeInSeconds);
        addCookie(translet, contextPath, token, maxAgeInSeconds);
    }

    /**
     * Refreshes the authentication token cookie.
     * @param translet the current translet
     * @param oldToken the old token to be refreshed
     * @throws InvalidPBTokenException if the token is invalid or expired
     */
    public void refreshCookie(@NonNull Translet translet, @NonNull String oldToken) throws InvalidPBTokenException {
        int maxAgeInSeconds = AppMonTokenIssuer.validateToken(oldToken);
        String newToken = AppMonTokenIssuer.issueToken(maxAgeInSeconds);
        addCookie(translet, translet.getContextPath(), newToken, maxAgeInSeconds);
    }

    /**
     * Removes the authentication token cookie.
     * @param translet the current translet
     */
    public void removeCookie(@NonNull Translet translet) {
        HttpServletRequest request = translet.getRequestAdaptee();
        HttpServletResponse response = translet.getResponseAdaptee();
        Cookie cookie = WebUtils.getCookie(request, AUTH_TOKEN_NAME);
        if (cookie != null) {
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
    }

    private void addCookie(@NonNull Translet translet, String contextPath, String token, int maxAgeInSeconds) {
        HttpServletResponse response = translet.getResponseAdaptee();
        Cookie cookie = new Cookie(AUTH_TOKEN_NAME, token);
        cookie.setPath(contextPath);
        cookie.setMaxAge(maxAgeInSeconds);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

}

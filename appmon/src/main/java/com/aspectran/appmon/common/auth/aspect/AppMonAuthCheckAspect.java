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
package com.aspectran.appmon.common.auth.aspect;

import com.aspectran.appmon.common.auth.AppMonCookieIssuer;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Aspect;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Before;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Joinpoint;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.support.http.MediaType;
import com.aspectran.web.support.util.WebUtils;
import jakarta.servlet.http.Cookie;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An aspect that checks for an authentication token before processing a request.
 * If the token is missing or invalid, it redirects the user to the main page.
 * If the token is valid, it refreshes the token's expiration time.
 *
 * <p>Created: 2024-07-27</p>
 */
@Component
@Aspect("appMonAuthCheckAspect")
@Joinpoint(pointcut = {
        "+: /**",
        "-: /auth-expired"
})
public class AppMonAuthCheckAspect {

    private final Logger logger = LoggerFactory.getLogger(AppMonAuthCheckAspect.class);

    private static final String AUTH_TOKEN_NAME = "appmon-auth-token";

    private final AppMonCookieIssuer appMonCookieIssuer;

    @Autowired
    public AppMonAuthCheckAspect(AppMonCookieIssuer appMonCookieIssuer) {
        this.appMonCookieIssuer = appMonCookieIssuer;
    }

    /**
     * Checks for a valid authentication token in the cookies.
     * If the token is not found or is invalid, redirects to the main page.
     * If the token is valid, the cookie is refreshed with a new expiration time.
     * @param translet the current translet
     */
    @Before
    public void before(@NonNull Translet translet) {
        Cookie cookie = WebUtils.getCookie(translet, AUTH_TOKEN_NAME);
        if (cookie == null) {
            reject(translet);
            return;
        }

        String token = cookie.getValue();
        try {
            appMonCookieIssuer.refreshCookie(translet, token);
        } catch (Exception e) {
            if (e instanceof InvalidPBTokenException) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unauthorized access to {}", translet.getActualRequestName());
                }
            } else {
                logger.error(e.getMessage(), e);
            }
            reject(translet);
        }
    }

    /**
     * Rejects the request due to an authentication failure.
     * For HTML requests, redirects to an authentication expired page.
     * For other requests (like AJAX), returns a 403 Forbidden status.
     * @param translet the current translet
     */
    private void reject(@NonNull Translet translet) {
        if (WebUtils.isAcceptContentTypes(translet, MediaType.TEXT_HTML)) {
            translet.redirect("/auth-expired");
            return;
        }
        translet.transform(new DefaultRestResponse().forbidden());
    }

}

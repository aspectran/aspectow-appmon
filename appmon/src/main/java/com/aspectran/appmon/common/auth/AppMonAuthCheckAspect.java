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

import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Aspect;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Before;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Joinpoint;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.support.util.WebUtils;
import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>Created: 2024-07-27</p>
 */
@Component
@Bean("appMonAuthCheckAspect")
@Aspect("appMonAuthCheckAspect")
@Joinpoint(pointcut = {
        "+: /**"
})
public class AppMonAuthCheckAspect {

    private final Logger logger = LoggerFactory.getLogger(AppMonAuthCheckAspect.class);

    private static final String AUTH_TOKEN_NAME = "appmon-auth-token";

    @Before
    public void before(@NonNull Translet translet) throws IOException {
        Cookie cookie = WebUtils.getCookie(translet, AUTH_TOKEN_NAME);
        if (cookie == null) {
            goToMain(translet);
            return;
        }

        String token = cookie.getValue();
        try {
            AppMonTokenIssuer.validateToken(token);
        } catch (Exception e) {
            if (e instanceof InvalidPBTokenException) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unauthorized access to {}", translet.getActualRequestName());
                }
            } else {
                logger.error(e.getMessage(), e);
            }
            goToMain(translet);
        }
    }

    private void goToMain(@NonNull Translet translet) {
        if (StringUtils.hasLength(translet.getContextPath())) {
            translet.redirect("/../");
        } else {
            translet.redirect("/");
        }
    }

}

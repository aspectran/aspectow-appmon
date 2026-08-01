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
package com.aspectran.aspectow.console.security;

import com.aspectran.core.activity.Translet;
import com.aspectran.core.adapter.ResponseAdapter;
import com.aspectran.core.component.bean.annotation.Aspect;
import com.aspectran.core.component.bean.annotation.Before;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Joinpoint;
import org.jspecify.annotations.NonNull;

/**
 * Aspect that injects security HTTP headers into every web response
 * to mitigate XSS, Clickjacking, MIME-sniffing, and other web vulnerabilities.
 */
@Component
@Aspect(
        id = "WebSecurityAspect",
        order = 1
)
@Joinpoint(
        pointcut = {
                "+: /**"
        }
)
public class WebSecurityAspect {

    private static final String CSP_POLICY =
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval' cdn.jsdelivr.net; " +
            "style-src 'self' 'unsafe-inline' cdn.jsdelivr.net; " +
            "img-src 'self' data: cdn.jsdelivr.net; " +
            "font-src 'self' cdn.jsdelivr.net data:;";

    @Before
    public void applySecurityHeaders(@NonNull Translet translet) {
        ResponseAdapter responseAdapter = translet.getResponseAdapter();
        if (responseAdapter != null) {
            responseAdapter.setHeader("X-Frame-Options", "SAMEORIGIN");
            responseAdapter.setHeader("X-Content-Type-Options", "nosniff");
            responseAdapter.setHeader("X-XSS-Protection", "1; mode=block");
            responseAdapter.setHeader("Content-Security-Policy", CSP_POLICY);
            responseAdapter.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        }
    }

}

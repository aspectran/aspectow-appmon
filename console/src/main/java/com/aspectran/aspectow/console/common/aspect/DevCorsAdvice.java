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
package com.aspectran.aspectow.console.common.aspect;

import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Aspect;
import com.aspectran.core.component.bean.annotation.Before;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Joinpoint;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.context.rule.type.MethodType;
import com.aspectran.web.support.http.HttpHeaders;
import org.jspecify.annotations.NonNull;

/**
 * Aspect to handle CORS requests during local development.
 */
@Component
@Profile("dev")
@Aspect(id = "devCorsAspect")
@Joinpoint(
        pointcut = {
                "+: /nodes/**",
                "+: /appmon/**"
        }
)
public class DevCorsAdvice {

    @Before
    public void process(@NonNull Translet translet) {
        String origin = translet.getRequestAdapter().getHeader(HttpHeaders.ORIGIN);
        if (origin != null) {
            translet.getResponseAdapter().setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            translet.getResponseAdapter().setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            translet.getResponseAdapter().setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
            translet.getResponseAdapter().setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization, X-Requested-With");
            translet.getResponseAdapter().setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800");
        }

        if (MethodType.OPTIONS.equals(translet.getRequestMethod())) {
            translet.getResponseAdapter().setStatus(200);
            translet.response();
        }
    }

}

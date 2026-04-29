/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.console.framework;

import com.aspectran.core.activity.InstantActivity;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.activity.request.ParameterMap;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles AsEL expression testing requests using an isolated InstantActivity.
 */
@Component("/framework/asel")
@Bean("aselActivity")
public class AselActivity extends InstantActivitySupport {

    @Request("/")
    @Dispatch("framework/asel/tester")
    @Action("page")
    public Map<String, Object> index() {
        return Map.of(
            "title", "AsEL Tester",
            "style", "asel-page",
            "group", "tools-menu"
        );
    }

    @RequestToPost("/test")
    public RestResponse test(String expression) {
        if (StringUtils.isEmpty(expression)) {
            return new FailureResponse().setError("required", "Expression is required.");
        }

        // Strict sandbox security check
        if (isUnsafe(expression)) {
            return new FailureResponse().setError("security", "Access to restricted system resources or properties is denied for security reasons.");
        }

        try {
            // Create a fresh InstantActivity bound to the current ActivityContext
            InstantActivity activity = new InstantActivity(getActivityContext());

            // Isolate test data (Sandbox environment)
            ParameterMap parameterMap = new ParameterMap();
            parameterMap.setParameter("id", "tester");
            parameterMap.setParameter("status", "active");
            activity.setParameterMap(parameterMap);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sampleAttr", "Sandbox mode enabled");
            attributes.put("sampleRole", "ADMIN");

            Map<String, Object> user = new HashMap<>();
            user.put("nickname", "Aspectow Developer");
            user.put("level", 99);
            attributes.put("sampleUser", user);
            activity.setAttributeMap(attributes);

            // Evaluate the expression within the perform block of the InstantActivity
            Object result = activity.perform(() -> activity.evaluate(expression));

            Map<String, Object> data = new HashMap<>();
            data.put("result", result);
            data.put("type", (result != null ? result.getClass().getName() : "null"));

            return new SuccessResponse(data).ok();
        } catch (Exception e) {
            String msg = (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return new FailureResponse().setError("error", msg);
        }
    }

    private boolean isUnsafe(@NonNull String expression) {
        String clean = expression.toLowerCase().replaceAll("\\s", "");
        // Block static class access and sensitive system objects
        if (clean.contains("system") || clean.contains("runtime") ||
                clean.contains("classloader") || clean.contains("activitycontext") ||
                clean.contains("beanfactory") || clean.contains("shutdown")) {
            return true;
        }
        // Block property tokens that access system/classpath/env or secrets
        if (clean.contains("%{")) {
            if (clean.contains("system:") || clean.contains("classpath:") ||
                    clean.contains("password") || clean.contains("secret") ||
                    clean.contains("key") || clean.contains("token")) {
                return true;
            }
        }
        return false;
    }

}

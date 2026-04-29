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

import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.wildcard.WildcardMasker;
import com.aspectran.utils.wildcard.WildcardMatcher;
import com.aspectran.utils.wildcard.WildcardPattern;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles wildcard pattern testing requests.
 */
@Component("/framework/wildcard")
@Bean("wildcardActivity")
public class WildcardActivity {

    @Request("/")
    @Dispatch("framework/wildcard/tester")
    @Action("page")
    public Map<String, Object> index() {
        return Map.of(
            "title", "Wildcard Tester",
            "style", "wildcard-page",
            "group", "tools-menu"
        );
    }

    @RequestToPost("/test")
    public RestResponse test(String pattern, String input, String separator) {
        pattern = StringUtils.trimWhitespace(pattern);
        input = StringUtils.trimWhitespace(input);

        if (StringUtils.isEmpty(pattern)) {
            return new FailureResponse().setError("required", "Pattern is required.");
        }
        if (input == null) {
            input = StringUtils.EMPTY;
        }

        try {
            WildcardPattern wp;
            if (StringUtils.hasText(separator)) {
                wp = WildcardPattern.compile(pattern, separator.trim().charAt(0));
            } else {
                wp = WildcardPattern.compile(pattern);
            }

            boolean matched = WildcardMatcher.matches(wp, input);
            String masked = (matched ? WildcardMasker.mask(wp, input) : null);

            Map<String, Object> result = new HashMap<>();
            result.put("matched", matched);
            result.put("masked", masked);
            result.put("weight", wp.getWeight());

            return new SuccessResponse(result).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

}

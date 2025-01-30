/*
 * Copyright (c) 2020-2025 The Aspectran Project
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
package com.aspectran.aspectow.appmon.front;

import com.aspectran.aspectow.appmon.AboutMe;
import com.aspectran.aspectow.appmon.manager.AppMonManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.security.InvalidPBTokenException;

import java.util.Map;

/**
 * <p>Created: 2020/02/23</p>
 */
@Component
public class FrontAction {

    @Request("/")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> home() {
        return Map.of(
            "include", "home/main",
            "style", "fluid compact"
        );
    }

    @Request("/front/${token}/${endpoint}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> front(@NonNull Translet translet, String token, String endpoint) {
        try {
            AppMonManager.validateToken(token);
        } catch (InvalidPBTokenException e) {
            if (StringUtils.hasLength(translet.getContextPath())) {
                translet.redirect("/../");
            } else {
                translet.redirect("/");
            }
            return null;
        }
        return Map.of(
            "version", AboutMe.getVersion(),
            "headinclude", "appmon/_endpoints",
            "include", "appmon/appmon",
            "style", "fluid compact",
            "token", AppMonManager.issueToken(),
            "endpoint", StringUtils.nullToEmpty(endpoint)
        );
    }

}

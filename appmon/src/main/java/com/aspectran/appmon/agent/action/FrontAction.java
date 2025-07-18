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
package com.aspectran.appmon.agent.action;

import com.aspectran.appmon.AboutMe;
import com.aspectran.appmon.manager.AppMonManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.security.InvalidPBTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <p>Created: 2020/02/23</p>
 */
@Component
public class FrontAction {

    private static final Logger logger = LoggerFactory.getLogger(FrontAction.class);

    @Request("/")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> home() {
        return Map.of(
            "include", "home/main",
            "style", "fluid compact",
            "version", AboutMe.getVersion()
            );
    }

    @Request("/${ignore}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> home2() {
        return home();
    }

    @Request("/front/${token}/${instances}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> front(@NonNull Translet translet, String token, String instances) {
        if (token == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Required token");
            }
            return home();
        }
        try {
            AppMonManager.validateToken(token);
            return Map.of(
                "headinclude", "appmon/_domains",
                "include", "appmon/appmon",
                "style", "fluid compact",
                "version", AboutMe.getVersion(),
                "token", AppMonManager.issueToken(),
                "instances", StringUtils.nullToEmpty(instances)
            );
        } catch (Exception e) {
            if (e instanceof InvalidPBTokenException) {
                logger.error("Invalid token: {}", token);
            } else {
                logger.error(e.getMessage(), e);
            }
            if (StringUtils.hasLength(translet.getContextPath())) {
                translet.redirect("/../");
            } else {
                translet.redirect("/");
            }
            return null;
        }
    }

}

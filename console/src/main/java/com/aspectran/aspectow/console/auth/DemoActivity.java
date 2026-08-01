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
package com.aspectran.aspectow.console.auth;

import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.service.UserService;
import com.aspectran.aspectow.console.common.util.ConsoleWebUtils;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Redirect;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.support.http.HttpHeaders;
import org.jspecify.annotations.NonNull;

/**
 * Handles authentication requests.
 */
@Component("/auth/demo")
@Profile("demo")
public class DemoActivity {

    private final UserService userService;

    private final LoginActivity loginActivity;

    @Autowired
    public DemoActivity(UserService userService, LoginActivity loginActivity) {
        this.userService = userService;
        this.loginActivity = loginActivity;
    }

    @Request("/login")
    @Redirect("/")
    public void demoLogin(@NonNull Translet translet, @NonNull String token, String redirect) {
        SessionAdapter sessionAdapter = translet.getSessionAdapter();
        UserInfo userInfo = sessionAdapter.getAttribute(UserInfo.USERINFO_KEY);
        if (userInfo != null) {
            if (StringUtils.hasText(redirect)) {
                translet.redirect(redirect);
            }
            return;
        }

        try {
            if (ConsoleTokenIssuer.isDemoToken(token)) {
                User user = userService.getUserByUsername("demo");

                if ("NORMAL".equals(user.getStatus())) {
                    loginActivity.doLogin(translet, user);

                    String remoteAddr = ConsoleWebUtils.getRemoteAddr(translet);
                    String userAgent = translet.getRequestAdapter().getHeader(HttpHeaders.USER_AGENT);

                    userService.recordLogin(user.getUsername(), remoteAddr, userAgent, true);

                    if (StringUtils.hasText(redirect)) {
                        translet.redirect(redirect);
                    }
                }
            }
        } catch (Exception e) {
            // ignore and show normal login page
        }
    }

}

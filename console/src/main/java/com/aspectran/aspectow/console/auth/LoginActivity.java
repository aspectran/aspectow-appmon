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
package com.aspectran.aspectow.console.auth;

import com.aspectran.aspectow.console.common.db.model.Permission;
import com.aspectran.aspectow.console.common.db.model.Role;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.service.UserService;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Redirect;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.http.HttpHeaders;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles authentication requests.
 */
@Component("/auth")
public class LoginActivity {

    private final UserService userService;

    @Autowired
    public LoginActivity(UserService userService) {
        this.userService = userService;
    }

    @Request("/login")
    @Dispatch("auth/login")
    @Action("page")
    public Map<String, String> loginPage() {
        return Map.of(
                "title", "Login",
                "layout", "popup"
        );
    }

    @RequestToPost("/login")
    public RestResponse login(Translet translet, String username, String password) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return new FailureResponse().setError("required", "Username and password are required.");
        }

        String remoteAddr = getRemoteAddr(translet);
        String userAgent = translet.getRequestAdapter().getHeader(HttpHeaders.USER_AGENT);

        User user = userService.getUserByUsername(username);
        // TODO: Use actual password hashing
        if (user != null && user.getPassword().equals(password)) {
            if (!"NORMAL".equals(user.getStatus())) {
                userService.recordLogin(username, remoteAddr, userAgent, false);
                return new FailureResponse().setError("locked", "Account is " + user.getStatus());
            }

            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(user.getUserId());
            userInfo.setUsername(user.getUsername());
            userInfo.setNickname(user.getNickname());

            Set<String> roles = new HashSet<>();
            Set<String> permissions = new HashSet<>();
            if (user.getRoles() != null) {
                for (Role role : user.getRoles()) {
                    roles.add(role.getRoleName());
                    if (role.getPermissions() != null) {
                        for (Permission perm : role.getPermissions()) {
                            permissions.add(perm.getPermCode());
                        }
                    }
                }
            }
            userInfo.setRoles(roles);
            userInfo.setPermissions(permissions);

            SessionAdapter sessionAdapter = translet.getSessionAdapter();
            sessionAdapter.setAttribute(UserInfo.USERINFO_KEY, userInfo);
            sessionAdapter.setMaxInactiveInterval(1800); // 30 min.

            userService.recordLogin(username, remoteAddr, userAgent, true);
            return new SuccessResponse("OK").ok();
        } else {
            userService.recordLogin(username, remoteAddr, userAgent, false);
            return new FailureResponse().setError("invalid", "Invalid username or password.");
        }
    }

    @Request("/logout")
    @Redirect("/")
    public void logout(Translet translet) {
        SessionAdapter sessionAdapter = translet.getSessionAdapter();
        sessionAdapter.removeAttribute(UserInfo.USERINFO_KEY);
    }

    /**
     * Gets the remote address from the translet, considering the X-Forwarded-For header.
     */
    private String getRemoteAddr(@NonNull Translet translet) {
        String remoteAddr = translet.getRequestAdapter().getHeader(HttpHeaders.X_FORWARDED_FOR);
        if (StringUtils.hasLength(remoteAddr)) {
            if (remoteAddr.contains(",")) {
                remoteAddr = StringUtils.tokenize(remoteAddr, ",", true)[0];
            }
        } else {
            remoteAddr = ((HttpServletRequest)translet.getRequestAdaptee()).getRemoteAddr();
        }
        return remoteAddr;
    }

}

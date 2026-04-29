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
package com.aspectran.aspectow.console.user;

import com.aspectran.aspectow.console.auth.UserInfo;
import com.aspectran.aspectow.console.common.db.model.LoginHistory;
import com.aspectran.aspectow.console.common.db.model.Role;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.service.UserService;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Handles user management requests.
 */
@Component("/user")
public class UserManagementActivity {

    private final UserService userService;

    @Autowired
    public UserManagementActivity(UserService userService) {
        this.userService = userService;
    }

    @Request("/")
    @Dispatch("user/list")
    @Action("page")
    public Map<String, Object> list() {
        List<User> userList = userService.getUserList();
        List<Role> roleList = userService.getRoleList();
        return Map.of(
            "title", "Users",
            "style", "user-page",
            "group", "accounts-menu",
            "userList", userList,
            "roleList", roleList
        );
    }

    @Request("/login-history")
    @Dispatch("user/login-history")
    @Action("page")
    public Map<String, Object> loginHistory(@NonNull Translet translet, String username) {
        UserInfo userInfo = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
        String targetUsername = username;

        // If not an admin, force to see only their own history
        if (userInfo != null && !userInfo.hasRole("SUPER_ADMIN")) {
            targetUsername = userInfo.getUsername();
        }

        List<LoginHistory> historyList = userService.getLoginHistoryList(targetUsername);
        return Map.of(
            "title", "Login History",
            "style", "user-page",
                "group", "accounts-menu",
            "historyList", historyList,
            "username", (targetUsername != null ? targetUsername : "")
        );
    }

    @RequestToPost("/save")
    public RestResponse save(@NonNull User user, Long[] roleIds) {
        if (StringUtils.isEmpty(user.getUsername())) {
            return new FailureResponse().setError("required", "Username is required.");
        }

        List<Long> roleIdList = (roleIds != null ? List.of(roleIds) : null);

        if (user.getUserId() != null) {
            // Update
            User existing = userService.getUserById(user.getUserId());
            if (existing == null) {
                 return new FailureResponse().setError("not_found", "User not found.");
            }
            userService.updateUser(user, roleIdList);
            return new SuccessResponse("Updated").ok();
        } else {
            // Insert
            if (userService.getUserByUsername(user.getUsername()) != null) {
                return new FailureResponse().setError("duplicate", "Username already exists.");
            }
            if (StringUtils.isEmpty(user.getPassword())) {
                return new FailureResponse().setError("required", "Password is required for a new user.");
            }
            userService.createUser(user, roleIdList);
            return new SuccessResponse("Created").ok();
        }
    }

    @RequestToPost("/delete")
    public RestResponse delete(Long userId) {
        if (userId == null) {
            return new FailureResponse().setError("required", "User ID is required.");
        }
        userService.deleteUser(userId);
        return new SuccessResponse("Deleted").ok();
    }

}

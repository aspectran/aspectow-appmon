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
package com.aspectran.aspectow.console.user;

import com.aspectran.aspectow.console.auth.UserInfo;
import com.aspectran.aspectow.console.common.db.model.AuditLog;
import com.aspectran.aspectow.console.common.db.model.LoginHistory;
import com.aspectran.aspectow.console.common.db.model.Permission;
import com.aspectran.aspectow.console.common.db.model.Role;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.pagination.PageInfo;
import com.aspectran.aspectow.console.common.service.UserService;
import com.aspectran.aspectow.console.common.util.ConsoleWebUtils;
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
 * Controller class that handles user management requests, including
 * listing users, managing login history, and creating, updating, or deleting users.
 */
@Component("/user")
public class UserManagementActivity {

    private final UserService userService;

    /**
     * Constructs a new {@code UserManagementActivity} with the specified user service.
     * @param userService the user service
     */
    @Autowired
    public UserManagementActivity(UserService userService) {
        this.userService = userService;
    }

    /**
     * Displays the user list page containing all users and available roles.
     * @param translet the current translet
     * @param searchKeyword the search keyword
     * @return a map of attributes for rendering the view
     */
    @Request("/")
    @Dispatch("user/list")
    @Action("page")
    public Map<String, Object> list(@NonNull Translet translet, String searchKeyword) {
        PageInfo pageInfo = PageInfo.of(translet, "user_list_page_size");
        List<User> userList = userService.getUserList(pageInfo, searchKeyword);
        List<Role> roleList = userService.getRoleList();
        List<Permission> permissionList = userService.getPermissionList();
        return Map.of(
            "title", "Users",
            "style", "user-management-page",
            "group", "accounts-menu",
            "userList", userList,
            "roleList", roleList,
            "permissionList", permissionList,
            "pageInfo", pageInfo,
            "searchKeyword", (searchKeyword != null ? searchKeyword : "")
        );
    }

    /**
     * Displays the login history page for a given user or the current user if not an admin.
     * @param translet the current translet
     * @param username the target username
     * @param searchKeyword the search keyword
     * @return a map of attributes for rendering the view
     */
    @Request("/login-history")
    @Dispatch("user/login-history")
    @Action("page")
    public Map<String, Object> loginHistory(@NonNull Translet translet, String username, String searchKeyword) {
        UserInfo userInfo = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
        String targetUsername = username;

        // If not an admin, force to see only their own history
        if (userInfo != null && !userInfo.hasRole("SUPER_ADMIN")) {
            targetUsername = userInfo.getUsername();
        }

        PageInfo pageInfo = PageInfo.of(translet, "login_history_page_size");
        List<LoginHistory> historyList = userService.getLoginHistoryList(pageInfo, targetUsername, searchKeyword);
        return Map.of(
            "title", "Login History",
            "style", "login-history-page",
            "group", "accounts-menu",
            "historyList", historyList,
            "pageInfo", pageInfo,
            "username", (targetUsername != null ? targetUsername : ""),
            "searchKeyword", (searchKeyword != null ? searchKeyword : "")
        );
    }

    /**
     * Displays the security audit log page.
     * @param translet the current translet
     * @param username the target username filter
     * @param searchKeyword the search keyword
     * @return a map of attributes for rendering the view
     */
    @Request("/audit-log")
    @Dispatch("user/audit-log")
    @Action("page")
    public Map<String, Object> auditLog(@NonNull Translet translet, String username, String searchKeyword) {
        UserInfo userInfo = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
        String targetUsername = username;

        if (userInfo != null && !userInfo.hasRole("SUPER_ADMIN")) {
            targetUsername = userInfo.getUsername();
        }

        PageInfo pageInfo = PageInfo.of(translet, "audit_log_page_size");
        List<AuditLog> auditList = userService.getAuditLogList(pageInfo, targetUsername, searchKeyword);
        return Map.of(
            "title", "Audit Log",
            "style", "audit-log-page",
            "group", "accounts-menu",
            "auditList", auditList,
            "pageInfo", pageInfo,
            "username", (targetUsername != null ? targetUsername : ""),
            "searchKeyword", (searchKeyword != null ? searchKeyword : "")
        );
    }

    /**
     * Saves user details, either creating a new user or updating an existing one,
     * along with their assigned roles.
     * @param user the user data
     * @param roleIds the array of role IDs to associate with the user
     * @return a {@link RestResponse} representing success or failure of the operation
     */
    @RequestToPost("/save")
    public RestResponse save(@NonNull Translet translet, @NonNull User user, Long[] roleIds) {
        if (StringUtils.isEmpty(user.getUsername())) {
            return new FailureResponse().setError("required", "Username is required.");
        }

        List<Long> roleIdList = (roleIds != null ? List.of(roleIds) : null);
        UserInfo actor = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
        String actorName = (actor != null ? actor.getUsername() : "system");
        String remoteAddr = ConsoleWebUtils.getRemoteAddr(translet);

        if (user.getUserId() != null) {
            // Update
            User existing = userService.getUserById(user.getUserId());
            if (existing == null) {
                 return new FailureResponse().setError("not_found", "User not found.");
            }
            userService.updateUser(user, roleIdList);
            userService.recordAuditLog(actorName, "USER_UPDATE", user.getUsername(), "Updated user status: " + user.getStatus(), remoteAddr);
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
            userService.recordAuditLog(actorName, "USER_CREATE", user.getUsername(), "Created new user", remoteAddr);
            return new SuccessResponse("Created").ok();
        }
    }

    @RequestToPost("/delete")
    public RestResponse delete(@NonNull Translet translet, Long userId) {
        if (userId == null) {
            return new FailureResponse().setError("required", "User ID is required.");
        }
        User targetUser = userService.getUserById(userId);
        String targetName = (targetUser != null ? targetUser.getUsername() : String.valueOf(userId));
        userService.deleteUser(userId);

        UserInfo actor = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
        String actorName = (actor != null ? actor.getUsername() : "system");
        userService.recordAuditLog(actorName, "USER_DELETE", targetName, "Deleted user ID: " + userId, ConsoleWebUtils.getRemoteAddr(translet));

        return new SuccessResponse("Deleted").ok();
    }

    @RequestToPost("/role/save-permissions")
    public RestResponse saveRolePermissions(@NonNull Translet translet, Long roleId, Long[] permIds) {
        if (roleId == null) {
            return new FailureResponse().setError("required", "Role ID is required.");
        }
        List<Long> permIdList = (permIds != null ? List.of(permIds) : null);
        userService.updateRolePermissions(roleId, permIdList);

        UserInfo actor = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
        String actorName = (actor != null ? actor.getUsername() : "system");
        userService.recordAuditLog(actorName, "ROLE_PERM_UPDATE", "RoleID:" + roleId, "Updated permissions for role ID: " + roleId, ConsoleWebUtils.getRemoteAddr(translet));

        return new SuccessResponse("Role permissions updated").ok();
    }

}

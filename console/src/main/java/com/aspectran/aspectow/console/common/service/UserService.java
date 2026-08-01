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
package com.aspectran.aspectow.console.common.service;

import com.aspectran.aspectow.console.common.db.model.AuditLog;
import com.aspectran.aspectow.console.common.db.model.LoginHistory;
import com.aspectran.aspectow.console.common.db.model.Permission;
import com.aspectran.aspectow.console.common.db.model.Role;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.pagination.PageInfo;

import java.util.List;

/**
 * Service interface for user management.
 */
public interface UserService {

    /**
     * Retrieves a user by their ID.
     * @param userId the user ID
     * @return the user, or null if not found
     */
    User getUserById(Long userId);

    /**
     * Retrieves a user by their username.
     * @param username the username of the user
     * @return the user, or null if not found
     */
    User getUserByUsername(String username);

    /**
     * Checks if the given password matches the password stored in the database.
     * @param user the user object from the database
     * @param password the password entered by the user
     * @return true if the passwords match, false otherwise
     */
    boolean checkPassword(User user, String password);

    /**
     * Checks if the user is required to change their password (e.g. initial setup).
     * @param user the user object
     * @param password the password entered by the user
     * @return true if password change is required, false otherwise
     */
    boolean isPasswordChangeRequired(User user, String password);

    /**
     * Retrieves users with pagination and search keyword.
     * @param pageInfo the pagination info
     * @param searchKeyword the search keyword
     * @return the list of users
     */
    List<User> getUserList(PageInfo pageInfo, String searchKeyword);

    /**
     * Retrieves all users.
     * @param pageInfo the pagination info
     * @return the list of users
     */
    default List<User> getUserList(PageInfo pageInfo) {
        return getUserList(pageInfo, null);
    }

    /**
     * Retrieves all roles.
     * @return the list of roles
     */
    List<Role> getRoleList();

    /**
     * Retrieves all permissions.
     * @return the list of permissions
     */
    List<Permission> getPermissionList();

    /**
     * Retrieves permissions associated with a specific role.
     * @param roleId the role ID
     * @return the list of permissions
     */
    List<Permission> getPermissionsByRoleId(Long roleId);

    /**
     * Updates the permissions assigned to a role.
     * @param roleId the role ID
     * @param permIds the list of permission IDs to assign
     */
    void updateRolePermissions(Long roleId, List<Long> permIds);

    /**
     * Creates a new user with assigned roles.
     * @param user the user details to create
     * @param roleIds the IDs of roles to assign to the user
     */
    void createUser(User user, List<Long> roleIds);

    /**
     * Updates an existing user and their assigned roles.
     * @param user the user details to update
     * @param roleIds the updated list of role IDs to assign
     */
    void updateUser(User user, List<Long> roleIds);

    /**
     * Deletes a user by their ID.
     * @param userId the user ID to delete
     */
    void deleteUser(Long userId);

    /**
     * Records a user login attempt.
     * @param username the username of the user attempting to log in
     * @param ipAddress the IP address from which the login attempt originated
     * @param userAgent the User-Agent header of the browser or client
     * @param success true if the login attempt was successful, false otherwise
     */
    void recordLogin(String username, String ipAddress, String userAgent, boolean success);

    /**
     * Retrieves the login history for a specific user with search keyword.
     * @param pageInfo the pagination info
     * @param username the username of the user
     * @param searchKeyword the search keyword
     * @return the list of login history records
     */
    List<LoginHistory> getLoginHistoryList(PageInfo pageInfo, String username, String searchKeyword);

    /**
     * Retrieves the login history for a specific user.
     * @param pageInfo the pagination info
     * @param username the username of the user
     * @return the list of login history records
     */
    default List<LoginHistory> getLoginHistoryList(PageInfo pageInfo, String username) {
        return getLoginHistoryList(pageInfo, username, null);
    }

    /**
     * Records a security audit log entry.
     * @param username the username performing the action
     * @param eventType the event type
     * @param target the target resource/user
     * @param details the details
     * @param ipAddress the IP address
     */
    void recordAuditLog(String username, String eventType, String target, String details, String ipAddress);

    /**
     * Retrieves audit logs with pagination and search keyword.
     * @param pageInfo the pagination info
     * @param username the username filter
     * @param searchKeyword the search keyword
     * @return the list of audit log records
     */
    List<AuditLog> getAuditLogList(PageInfo pageInfo, String username, String searchKeyword);

    default List<AuditLog> getAuditLogList(PageInfo pageInfo) {
        return getAuditLogList(pageInfo, null, null);
    }

}

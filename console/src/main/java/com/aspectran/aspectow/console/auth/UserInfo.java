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

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * Object to be stored in the session representing the authenticated user.
 */
public class UserInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = -4563821094857234L;

    public static final String USERINFO_KEY = "USER_INFO";

    private Long userId;
    private String username;
    private String nickname;
    private String loginIp;
    private Set<String> roles;
    private Set<String> permissions;

    /**
     * Gets the user ID.
     * @return the user ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     * @param userId the user ID to set
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Gets the username.
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the nickname.
     * @return the nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Sets the nickname.
     * @param nickname the nickname to set
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Gets the login IP address.
     * @return the login IP address
     */
    public String getLoginIp() {
        return loginIp;
    }

    /**
     * Sets the login IP address.
     * @param loginIp the login IP address to set
     */
    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    /**
     * Gets the set of roles assigned to the user.
     * @return the set of roles
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Sets the set of roles assigned to the user.
     * @param roles the set of roles to set
     */
    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    /**
     * Gets the set of permissions granted to the user.
     * @return the set of permissions
     */
    public Set<String> getPermissions() {
        return permissions;
    }

    /**
     * Sets the set of permissions granted to the user.
     * @param permissions the set of permissions to set
     */
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * Checks if the user has the specified role.
     * @param role the role name
     * @return {@code true} if the user has the role, {@code false} otherwise
     */
    public boolean hasRole(String role) {
        return (roles != null && roles.contains(role));
    }

    /**
     * Checks if the user has the specified permission.
     * @param permission the permission name
     * @return {@code true} if the user has the permission, {@code false} otherwise
     */
    public boolean hasPermission(String permission) {
        return (permissions != null && permissions.contains(permission));
    }

}

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
    private Set<String> roles;
    private Set<String> permissions;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public boolean hasRole(String role) {
        return (roles != null && roles.contains(role));
    }

    public boolean hasPermission(String permission) {
        return (permissions != null && permissions.contains(permission));
    }

}

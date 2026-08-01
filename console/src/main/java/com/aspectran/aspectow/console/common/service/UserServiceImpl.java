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

import com.aspectran.aspectow.console.common.db.mapper.AccountMapper;
import com.aspectran.aspectow.console.common.db.model.AuditLog;
import com.aspectran.aspectow.console.common.db.model.LoginHistory;
import com.aspectran.aspectow.console.common.db.model.Permission;
import com.aspectran.aspectow.console.common.db.model.Role;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.pagination.PageInfo;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.aware.EnvironmentAware;
import com.aspectran.core.context.env.Environment;
import com.aspectran.utils.StringUtils;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the UserService.
 */
@Component
public class UserServiceImpl implements UserService, EnvironmentAware {

    private final StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

    private final AccountMapper accountMapper;

    private final Map<String, Integer> failedAttemptsMap = new ConcurrentHashMap<>();

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private Environment environment;

    @Autowired
    public UserServiceImpl(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public User getUserById(Long userId) {
        return accountMapper.getUserById(userId);
    }

    @Override
    public User getUserByUsername(String username) {
        return accountMapper.getUserByUsername(username);
    }

    @Override
    public boolean checkPassword(User user, String password) {
        if (user == null || password == null) {
            return false;
        }
        String dbPassword = user.getPassword();
        if (dbPassword == null) {
            return false;
        }
        try {
            if (passwordEncryptor.checkPassword(password, dbPassword)) {
                return true;
            }
        } catch (Exception e) {
            // Ignore exception and fallback to plain text checks
        }

        // 1. check if the user is a SUPER_ADMIN and created within 1 hour
        boolean isSuperAdmin = user.getRoles() != null &&
                user.getRoles().stream().anyMatch(role -> "SUPER_ADMIN".equals(role.getRoleName()));
        if (isSuperAdmin) {
            LocalDateTime createdAt = user.getCreatedAt();
            if (createdAt != null && createdAt.plusHours(1).isAfter(LocalDateTime.now())) {
                return dbPassword.equals(password);
            }
        }

        // 2. fallback to dev profile check
        if (environment != null && environment.acceptsProfiles("dev")) {
            return dbPassword.equals(password);
        }
        return false;
    }

    @Override
    public boolean isPasswordChangeRequired(User user, String password) {
        if (user == null || password == null) {
            return false;
        }
        String dbPassword = user.getPassword();
        if (dbPassword == null) {
            return false;
        }

        // If the password matches using the one-way hash, it is already hashed
        try {
            if (passwordEncryptor.checkPassword(password, dbPassword)) {
                return false;
            }
        } catch (Exception e) {
            // Ignore
        }

        // If it is plain text, SUPER_ADMIN, and within 1 hour, it requires a change
        boolean isSuperAdmin = user.getRoles() != null &&
                user.getRoles().stream().anyMatch(role -> "SUPER_ADMIN".equals(role.getRoleName()));
        if (isSuperAdmin && dbPassword.equals(password)) {
            LocalDateTime createdAt = user.getCreatedAt();
            if (createdAt != null && createdAt.plusHours(1).isAfter(LocalDateTime.now())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<User> getUserList(PageInfo pageInfo, String searchKeyword) {
        List<User> userList = accountMapper.getUserList(pageInfo, searchKeyword);
        if (pageInfo != null) {
            pageInfo.setTotalElements(userList.size(), () -> accountMapper.getUserCount(searchKeyword));
        }
        return userList;
    }

    @Override
    public List<Role> getRoleList() {
        return accountMapper.getRoleList();
    }

    @Override
    public List<Permission> getPermissionList() {
        return accountMapper.getPermissionList();
    }

    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return accountMapper.getPermissionsByRoleId(roleId);
    }

    @Override
    public void updateRolePermissions(Long roleId, List<Long> permIds) {
        if (roleId != null) {
            accountMapper.deleteRolePermissions(roleId);
            if (permIds != null) {
                for (Long permId : permIds) {
                    accountMapper.insertRolePermission(roleId, permId);
                }
            }
        }
    }

    @Override
    public void createUser(@NonNull User user, List<Long> roleIds) {
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncryptor.encryptPassword(user.getPassword()));
        }
        accountMapper.insertUser(user);
        if (roleIds != null && user.getUserId() != null) {
            for (Long roleId : roleIds) {
                accountMapper.insertUserRole(user.getUserId(), roleId);
            }
        }
    }

    @Override
    public void updateUser(@NonNull User user, List<Long> roleIds) {
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncryptor.encryptPassword(user.getPassword()));
        }
        accountMapper.updateUser(user);
        if ("NORMAL".equals(user.getStatus()) && StringUtils.hasText(user.getUsername())) {
            failedAttemptsMap.remove(user.getUsername());
        }
        if (roleIds != null) {
            accountMapper.deleteUserRoles(user.getUserId());
            for (Long roleId : roleIds) {
                accountMapper.insertUserRole(user.getUserId(), roleId);
            }
        }
    }

    @Override
    public void deleteUser(Long userId) {
        accountMapper.deleteUser(userId);
    }

    @Override
    public void recordLogin(String username, String ipAddress, String userAgent, boolean success) {
        LoginHistory history = new LoginHistory();
        history.setUsername(username);
        history.setIpAddress(ipAddress);
        history.setUserAgent(userAgent);
        history.setSuccessYn(success ? "Y" : "N");
        accountMapper.insertLoginHistory(history);
        if (success) {
            accountMapper.updateLastLogin(username);
            if (StringUtils.hasText(username)) {
                failedAttemptsMap.remove(username);
            }
        } else {
            if (StringUtils.hasText(username)) {
                int failedCount = failedAttemptsMap.compute(username, (k, v) -> (v == null ? 1 : v + 1));
                if (failedCount >= MAX_FAILED_ATTEMPTS) {
                    User user = accountMapper.getUserByUsername(username);
                    if (user != null && "NORMAL".equals(user.getStatus())) {
                        user.setStatus("LOCKED");
                        accountMapper.updateUser(user);
                        failedAttemptsMap.remove(username);
                    }
                }
            }
        }
    }

    @Override
    public List<LoginHistory> getLoginHistoryList(PageInfo pageInfo, String username, String searchKeyword) {
        List<LoginHistory> historyList = accountMapper.getLoginHistoryList(pageInfo, username, searchKeyword);
        if (pageInfo != null) {
            pageInfo.setTotalElements(historyList.size(), () -> accountMapper.getLoginHistoryCount(username, searchKeyword));
        }
        return historyList;
    }

    @Override
    public void recordAuditLog(String username, String eventType, String target, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(username);
        auditLog.setEventType(eventType);
        auditLog.setTarget(target);
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);
        accountMapper.insertAuditLog(auditLog);
    }

    @Override
    public List<AuditLog> getAuditLogList(PageInfo pageInfo, String username, String searchKeyword) {
        List<AuditLog> auditList = accountMapper.getAuditLogList(pageInfo, username, searchKeyword);
        if (pageInfo != null) {
            pageInfo.setTotalElements(auditList.size(), () -> accountMapper.getAuditLogCount(username, searchKeyword));
        }
        return auditList;
    }
}

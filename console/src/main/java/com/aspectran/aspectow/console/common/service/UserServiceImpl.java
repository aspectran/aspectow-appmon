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
package com.aspectran.aspectow.console.common.service;

import com.aspectran.aspectow.console.common.db.mapper.AccountMapper;
import com.aspectran.aspectow.console.common.db.model.LoginHistory;
import com.aspectran.aspectow.console.common.db.model.Role;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;

import java.util.List;

/**
 * Implementation of the UserService.
 */
@Component
public class UserServiceImpl implements UserService {

    private final AccountMapper accountMapper;

    @Autowired
    public UserServiceImpl(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
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
    public List<User> getUserList() {
        return accountMapper.getUserList();
    }

    @Override
    public List<Role> getRoleList() {
        return accountMapper.getRoleList();
    }

    @Override
    public void createUser(User user, List<Long> roleIds) {
        accountMapper.insertUser(user);
        if (roleIds != null && user.getUserId() != null) {
            for (Long roleId : roleIds) {
                accountMapper.insertUserRole(user.getUserId(), roleId);
            }
        }
    }

    @Override
    public void updateUser(User user, List<Long> roleIds) {
        accountMapper.updateUser(user);
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
        }
    }

    @Override
    public List<LoginHistory> getLoginHistoryList(String username) {
        return accountMapper.getLoginHistoryList(username);
    }

}

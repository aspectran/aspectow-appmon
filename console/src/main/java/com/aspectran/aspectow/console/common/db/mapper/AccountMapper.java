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
package com.aspectran.aspectow.console.common.db.mapper;

import com.aspectran.aspectow.console.common.db.model.LoginHistory;
import com.aspectran.aspectow.console.common.db.model.Permission;
import com.aspectran.aspectow.console.common.db.model.Role;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.db.tx.ConsoleSqlMapperProvider;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlMapperAccess;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * The MyBatis mapper interface for user accounts and RBAC data.
 */
@Mapper
public interface AccountMapper {

    /**
     * Retrieves a user by their ID.
     * @param userId the user ID
     * @return the user, or null if not found
     */
    User getUserById(Long userId);

    /**
     * Retrieves a user by their username.
     * @param username the username
     * @return the user, or null if not found
     */
    User getUserByUsername(String username);

    /**
     * Retrieves a list of all users.
     * @return the list of users
     */
    List<User> getUserList();

    /**
     * Inserts a new user record into the database.
     * @param user the user to insert
     * @return the number of affected rows
     */
    int insertUser(User user);

    /**
     * Updates an existing user's information.
     * @param user the user with updated info
     * @return the number of affected rows
     */
    int updateUser(User user);

    /**
     * Deletes a user by their ID.
     * @param userId the user ID
     * @return the number of affected rows
     */
    int deleteUser(Long userId);

    /**
     * Updates the last login timestamp for a user.
     * @param username the username
     * @return the number of affected rows
     */
    int updateLastLogin(String username);

    /**
     * Retrieves all roles assigned to a specific user.
     * @param userId the user ID
     * @return the list of roles
     */
    List<Role> getRolesByUserId(Long userId);

    /**
     * Retrieves a list of all defined roles.
     * @return the list of roles
     */
    List<Role> getRoleList();

    /**
     * Maps a role to a specific user.
     * @param userId the user ID
     * @param roleId the role ID
     * @return the number of affected rows
     */
    int insertUserRole(Long userId, Long roleId);

    /**
     * Removes all role assignments for a specific user.
     * @param userId the user ID
     * @return the number of affected rows
     */
    int deleteUserRoles(Long userId);

    /**
     * Retrieves all permissions associated with a specific role.
     * @param roleId the role ID
     * @return the list of permissions
     */
    List<Permission> getPermissionsByRoleId(Long roleId);

    /**
     * Retrieves a list of all defined permissions.
     * @return the list of permissions
     */
    List<Permission> getPermissionList();

    /**
     * Records a login attempt in the history log.
     * @param history the login history record
     * @return the number of affected rows
     */
    int insertLoginHistory(LoginHistory history);

    /**
     * Retrieves the login history for a specific user.
     * @param username the username
     * @return the list of login history records
     */
    List<LoginHistory> getLoginHistoryList(String username);

    /**
     * Data Access Object (DAO) for {@link AccountMapper}.
     * Provides a convenient way to access the mapper methods using Aspectran's bean container.
     */
    @Component
    @Bean("console.accountDao")
    class Dao extends SqlMapperAccess<AccountMapper> implements AccountMapper {

        /**
         * Constructs a new Dao.
         * @param sqlMapperProvider the SQL mapper provider
         */
        @Autowired
        public Dao(ConsoleSqlMapperProvider sqlMapperProvider) {
            super(sqlMapperProvider);
        }

        @Override
        public User getUserById(Long userId) {
            return mapper().getUserById(userId);
        }

        @Override
        public User getUserByUsername(String username) {
            return mapper().getUserByUsername(username);
        }

        @Override
        public List<User> getUserList() {
            return mapper().getUserList();
        }

        @Override
        public int insertUser(User user) {
            return mapper().insertUser(user);
        }

        @Override
        public int updateUser(User user) {
            return mapper().updateUser(user);
        }

        @Override
        public int deleteUser(Long userId) {
            return mapper().deleteUser(userId);
        }

        @Override
        public int updateLastLogin(String username) {
            return mapper().updateLastLogin(username);
        }

        @Override
        public List<Role> getRolesByUserId(Long userId) {
            return mapper().getRolesByUserId(userId);
        }

        @Override
        public List<Role> getRoleList() {
            return mapper().getRoleList();
        }

        @Override
        public int insertUserRole(Long userId, Long roleId) {
            return mapper().insertUserRole(userId, roleId);
        }

        @Override
        public int deleteUserRoles(Long userId) {
            return mapper().deleteUserRoles(userId);
        }

        @Override
        public List<Permission> getPermissionsByRoleId(Long roleId) {
            return mapper().getPermissionsByRoleId(roleId);
        }

        @Override
        public List<Permission> getPermissionList() {
            return mapper().getPermissionList();
        }

        @Override
        public int insertLoginHistory(LoginHistory history) {
            return mapper().insertLoginHistory(history);
        }

        @Override
        public List<LoginHistory> getLoginHistoryList(String username) {
            return mapper().getLoginHistoryList(username);
        }
    }

}

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

import com.aspectran.aspectow.console.common.db.model.Vault;
import com.aspectran.aspectow.console.common.db.tx.ConsoleSqlMapperProvider;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlMapperAccess;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * The MyBatis mapper interface for vault data.
 */
@Mapper
public interface VaultMapper {

    Vault getVaultById(Long vaultId);

    List<Vault> getVaultList();

    int insertVault(Vault vault);

    int updateVault(Vault vault);

    int deleteVault(Long vaultId);

    /**
     * Data Access Object (DAO) for {@link VaultMapper}.
     */
    @Component
    @Bean("console.vaultDao")
    class Dao extends SqlMapperAccess<VaultMapper> implements VaultMapper {

        @Autowired
        public Dao(ConsoleSqlMapperProvider sqlMapperProvider) {
            super(sqlMapperProvider);
        }

        @Override
        public Vault getVaultById(Long vaultId) {
            return mapper().getVaultById(vaultId);
        }

        @Override
        public List<Vault> getVaultList() {
            return mapper().getVaultList();
        }

        @Override
        public int insertVault(Vault vault) {
            return mapper().insertVault(vault);
        }

        @Override
        public int updateVault(Vault vault) {
            return mapper().updateVault(vault);
        }

        @Override
        public int deleteVault(Long vaultId) {
            return mapper().deleteVault(vaultId);
        }
    }

}

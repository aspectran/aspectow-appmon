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

import com.aspectran.aspectow.console.common.db.mapper.VaultMapper;
import com.aspectran.aspectow.console.common.db.model.Vault;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.utils.PBEncryptionUtils;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.apon.AponParseException;
import com.aspectran.utils.apon.AponReader;
import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.security.PBTokenIssuer;
import com.aspectran.utils.security.TimeLimitedPBTokenIssuer;
import org.jasypt.properties.PropertyValueEncryptionUtils;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.aspectran.utils.PBEncryptionUtils.getDefaultEncryptor;

/**
 * Implementation of the VaultService.
 */
@Component
public class VaultServiceImpl implements VaultService {

    private final VaultMapper vaultMapper;

    @Autowired
    public VaultServiceImpl(VaultMapper vaultMapper) {
        this.vaultMapper = vaultMapper;
    }

    @Override
    public Vault getVaultById(Long vaultId) {
        return vaultMapper.getVaultById(vaultId);
    }

    @Override
    public List<Vault> getVaultList() {
        return vaultMapper.getVaultList();
    }

    @Override
    public void createVault(Vault vault, String plainText) {
        if (StringUtils.hasText(plainText)) {
            vault.setEncryptedValue(encrypt(vault, plainText));
        }
        vaultMapper.insertVault(vault);
    }

    @Override
    public void updateVault(Vault vault, String plainText, String existingEncryptedValue) {
        if (StringUtils.hasText(plainText)) {
            String oldPlainText = decrypt(existingEncryptedValue, vault.getTokenType());
            if (!plainText.equals(oldPlainText)) {
                vault.setEncryptedValue(encrypt(vault, plainText));
            }
        }
        vaultMapper.updateVault(vault);
    }

    private String encrypt(@NonNull Vault vault, String plainText) {
        String type = vault.getTokenType();
        if ("SIMPLE".equals(type) || type == null) {
            return PBEncryptionUtils.encrypt(plainText);
        } else {
            // PERSISTENT or TIME_LIMITED (Requires APON)
            try {
                Parameters payload = AponReader.read(plainText);
                if ("TIME_LIMITED".equals(type) && vault.getValidUntil() != null) {
                    long expirationTime = Duration.between(LocalDateTime.now(), vault.getValidUntil()).toMillis();
                    return TimeLimitedPBTokenIssuer.createToken(payload, Math.max(expirationTime, 0));
                } else {
                    return PBTokenIssuer.createToken(payload);
                }
            } catch (AponParseException e) {
                throw new IllegalArgumentException("Invalid APON format: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void deleteVault(Long vaultId) {
        vaultMapper.deleteVault(vaultId);
    }

    @Override
    public String decrypt(String encryptedValue, String tokenType) {
        if (StringUtils.isEmpty(encryptedValue)) {
            return encryptedValue;
        }
        
        try {
            if ("TIME_LIMITED".equals(tokenType)) {
                Parameters payload = TimeLimitedPBTokenIssuer.parseToken(encryptedValue);
                return payload.toString();
            } else if ("PERSISTENT".equals(tokenType)) {
                Parameters payload = PBTokenIssuer.parseToken(encryptedValue);
                return payload.toString();
            } else {
                // SIMPLE
                if (PropertyValueEncryptionUtils.isEncryptedValue(encryptedValue)) {
                    return PropertyValueEncryptionUtils.decrypt(encryptedValue, getDefaultEncryptor());
                } else {
                    return getDefaultEncryptor().decrypt(encryptedValue);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Decryption failed: " + e.getMessage(), e);
        }
    }

}

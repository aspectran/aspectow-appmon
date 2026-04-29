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
package com.aspectran.aspectow.console.vault;

import com.aspectran.aspectow.console.common.db.model.Vault;
import com.aspectran.aspectow.console.common.service.VaultService;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.PBEncryptionUtils;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Handles vault management requests.
 */
@Component("/vault")
public class VaultActivity {

    private final VaultService vaultService;

    @Autowired
    public VaultActivity(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @Request("/")
    @Dispatch("vault/list")
    @Action("page")
    public Map<String, Object> list() {
        List<Vault> vaultList = vaultService.getVaultList();
        return Map.of(
            "title", "Vault",
            "style", "vault-page",
            "group", "security-menu",
            "vaultList", vaultList,
            "encryptionAlgorithm", PBEncryptionUtils.getAlgorithm(),
            "encryptionSalt", StringUtils.nullToEmpty(PBEncryptionUtils.getSalt()),
            "now", LocalDateTime.now()
        );
    }

    @RequestToPost("/encryption-password")
    public RestResponse getEncryptionPassword() {
        try {
            String password = PBEncryptionUtils.getPassword();
            return new SuccessResponse(password).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("failed", e.getMessage());
        }
    }

    @Request("/tool")
    @Dispatch("vault/tool")
    @Action("page")
    public Map<String, Object> tool() {
        List<String> algorithms = List.of(
            "PBEWITHHMACSHA256ANDAES_128",
            "PBEWITHHMACSHA512ANDAES_256",
            "PBEWithMD5AndDES",
            "PBEWithMD5AndTripleDES",
            "PBEWithSHA1AndDESede",
            "PBEWithSHA1AndRC2_40"
        );
        return Map.of(
            "title", "Vault Tool",
            "style", "vault-tool-page",
            "group", "tools-menu",
            "algorithms", algorithms,
            "defaultAlgorithm", PBEncryptionUtils.DEFAULT_ALGORITHM
        );
    }

    @RequestToPost("/tool/execute")
    public RestResponse executeTool(String algorithm, String password, String salt, String mode, String text) {
        if (StringUtils.isEmpty(algorithm) || StringUtils.isEmpty(password) || StringUtils.isEmpty(text)) {
            return new FailureResponse().setError("required", "Algorithm, Password, and Text are required.");
        }
        try {
            String result;
            if ("encrypt".equals(mode)) {
                result = PBEncryptionUtils.encrypt(text, password, salt);
            } else {
                result = PBEncryptionUtils.decrypt(text, password, salt);
            }
            return new SuccessResponse(result).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("failed", e.getMessage());
        }
    }

    @RequestToPost("/save")
    public RestResponse save(@NonNull Vault vault, String plainText, Integer expirationMinutes) {
        if (StringUtils.isEmpty(vault.getLabel())) {
            return new FailureResponse().setError("required", "Label is required.");
        }

        if (expirationMinutes != null && expirationMinutes > 0) {
            vault.setValidUntil(LocalDateTime.now().plusMinutes(expirationMinutes));
        }

        try {
            if (vault.getVaultId() != null) {
                // Update
                Vault existing = vaultService.getVaultById(vault.getVaultId());
                if (existing == null) {
                    return new FailureResponse().setError("not_found", "Token not found.");
                }
                vaultService.updateVault(vault, plainText, existing.getEncryptedValue());
                return new SuccessResponse("Updated").ok();
            } else {
                // Create
                if (StringUtils.isEmpty(plainText)) {
                    return new FailureResponse().setError("required", "Value to encrypt is required.");
                }
                vaultService.createVault(vault, plainText);
                return new SuccessResponse("Created").ok();
            }
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    @RequestToPost("/delete")
    public RestResponse delete(Long vaultId) {
        if (vaultId == null) {
            return new FailureResponse().setError("required", "Vault ID is required.");
        }
        vaultService.deleteVault(vaultId);
        return new SuccessResponse("Deleted").ok();
    }

    @RequestToPost("/decrypt")
    public RestResponse decrypt(String encryptedValue, String tokenType) {
        if (StringUtils.isEmpty(encryptedValue)) {
            return new FailureResponse().setError("required", "Encrypted value is required.");
        }
        try {
            String decrypted = vaultService.decrypt(encryptedValue, tokenType);
            return new SuccessResponse(decrypted).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("failed", e.getMessage());
        }
    }

}

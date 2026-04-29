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
package com.aspectran.aspectow.node.config;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Defines the security settings for encrypted communication within the cluster,
 * including the shared password, encryption algorithm, and salt for
 * Password-Based Encryption (PBE).
 *
 * <p>Created: 2026-04-18</p>
 */
public class SecretConfig extends DefaultParameters {

    private static final ParameterKey password;
    private static final ParameterKey algorithm;
    private static final ParameterKey salt;

    private static final ParameterKey[] parameterKeys;

    static {
        password = new ParameterKey("password", ValueType.STRING);
        algorithm = new ParameterKey("algorithm", ValueType.STRING);
        salt = new ParameterKey("salt", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                password,
                algorithm,
                salt
        };
    }

    public SecretConfig() {
        super(parameterKeys);
    }

    /**
     * Returns the shared password used for encryption.
     * @return the encryption password
     */
    public String getPassword() {
        return getString(password);
    }

    /**
     * Sets the shared password to be used for encryption.
     * @param password the encryption password
     */
    public void setPassword(String password) {
        putValue(SecretConfig.password, password);
    }

    /**
     * Returns the algorithm used for Password-Based Encryption (PBE).
     * @return the encryption algorithm
     */
    public String getAlgorithm() {
        return getString(algorithm);
    }

    /**
     * Sets the algorithm to be used for Password-Based Encryption (PBE).
     * @param algorithm the encryption algorithm
     */
    public void setAlgorithm(String algorithm) {
        putValue(SecretConfig.algorithm, algorithm);
    }

    /**
     * Returns the salt used for Password-Based Encryption (PBE).
     * @return the encryption salt
     */
    public String getSalt() {
        return getString(salt);
    }

    /**
     * Sets the salt to be used for Password-Based Encryption (PBE).
     * @param salt the encryption salt
     */
    public void setSalt(String salt) {
        putValue(SecretConfig.salt, salt);
    }

}

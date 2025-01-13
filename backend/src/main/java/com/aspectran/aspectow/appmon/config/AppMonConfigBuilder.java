/*
 * Copyright (c) 2020-2025 The Aspectran Project
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
package com.aspectran.aspectow.appmon.config;

import com.aspectran.utils.ResourceUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;

import java.io.IOException;
import java.io.Reader;

public abstract class AppMonConfigBuilder {

    private static final String APPMON_CONFIG_FILE = "com/aspectran/aspectow/appmon/config/appmon-config.apon";

    private static final String APPMON_CONFIG_FILE_PROD = "com/aspectran/aspectow/appmon/config/appmon-config-prod.apon";

    @NonNull
    public static AppMonConfig build(boolean forProd) throws IOException {
        if (forProd) {
            try {
                Reader reader = ResourceUtils.getResourceAsReader(APPMON_CONFIG_FILE_PROD);
                return new AppMonConfig(reader);
            } catch (IOException e) {
                // ignored
            }
        }
        Reader reader = ResourceUtils.getResourceAsReader(APPMON_CONFIG_FILE);
        return new AppMonConfig(reader);
    }

}

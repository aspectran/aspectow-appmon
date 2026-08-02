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
package com.aspectran.aspectow.console.framework;

import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Profile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * A sample bean for testing AsEL expressions.
 */
@Component
@Profile("[console.ui, console.custom-ui]")
@Bean("aselTestBean")
public class AselTestBean {

    /**
     * Gets the name of the test bean.
     * @return the name of the test bean
     */
    public String getName() {
        return "AsEL Expression Tester";
    }

    /**
     * Gets a test value.
     * @return the test value
     */
    public int getValue() {
        return 12345;
    }

    /**
     * Checks if the service or bean is available.
     * @return {@code true} if available, {@code false} otherwise
     */
    public boolean isAvailable() {
        return true;
    }

    /**
     * Gets the current server time.
     * @return the current local date-time
     */
    public LocalDateTime getServerTime() {
        return LocalDateTime.now();
    }

    /**
     * Gets a list of tags associated with the test bean.
     * @return a list of tags
     */
    public List<String> getTags() {
        return List.of("aspectran", "asel", "expression", "language");
    }

    /**
     * Gets metadata details as a map.
     * @return a map containing metadata
     */
    public Map<String, Object> getMetadata() {
        return Map.of(
            "version", "1.0.0",
            "environment", "console",
            "active", true
        );
    }

    /**
     * Returns a greeting message for the specified user.
     * @param user the user name
     * @return the greeting message
     */
    public String greet(String user) {
        return "Hello, " + user + "! Welcome to AsEL.";
    }

}

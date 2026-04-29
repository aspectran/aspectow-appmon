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
package com.aspectran.aspectow.console.framework;

import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * A sample bean for testing AsEL expressions.
 */
@Component
@Bean("aselTestBean")
public class AselTestBean {

    public String getName() {
        return "AsEL Expression Tester";
    }

    public int getValue() {
        return 12345;
    }

    public boolean isAvailable() {
        return true;
    }

    public LocalDateTime getServerTime() {
        return LocalDateTime.now();
    }

    public List<String> getTags() {
        return List.of("aspectran", "asel", "expression", "language");
    }

    public Map<String, Object> getMetadata() {
        return Map.of(
            "version", "1.0.0",
            "environment", "console",
            "active", true
        );
    }

    public String greet(String user) {
        return "Hello, " + user + "! Welcome to AsEL.";
    }

}

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
package com.aspectran.appmon.exporter;

import com.aspectran.appmon.service.CommandOptions;
import com.aspectran.utils.lifecycle.LifeCycle;

import java.util.List;

/**
 * <p>Created: 2024-12-18</p>
 */
public interface Exporter extends LifeCycle {

    ExporterType getType();

    String getName();

    void read(List<String> messages, CommandOptions commandOptions);

    void readIfChanged(List<String> messages, CommandOptions commandOptions);

    void broadcast(String message);

}

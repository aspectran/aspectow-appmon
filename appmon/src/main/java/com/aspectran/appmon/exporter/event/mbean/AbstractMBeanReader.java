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
package com.aspectran.appmon.exporter.event.mbean;

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.appmon.exporter.ExporterType;
import com.aspectran.appmon.exporter.event.AbstractEventReader;
import com.aspectran.utils.annotation.jsr305.NonNull;

/**
 * <p>Created: 2025-06-02</p>
 */
public abstract class AbstractMBeanReader extends AbstractEventReader {

    public AbstractMBeanReader(
            @NonNull ExporterManager exporterManager,
            @NonNull EventInfo eventInfo) {
        super(exporterManager, eventInfo);
    }

    @Override
    public ExporterType getType() {
        return ExporterType.STATUS;
    }

}

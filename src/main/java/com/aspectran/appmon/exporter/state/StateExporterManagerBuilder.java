/*
 * Copyright (c) 2018-2025 The Aspectran Project
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
package com.aspectran.appmon.exporter.state;

import com.aspectran.appmon.config.StateInfo;
import com.aspectran.utils.ClassUtils;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.logging.Logger;
import com.aspectran.utils.logging.LoggerFactory;

import java.util.List;

public abstract class StateExporterManagerBuilder {

    private static final Logger logger = LoggerFactory.getLogger(StateExporterManagerBuilder.class);

    @NonNull
    public static void build(@NonNull StateExporterManager stateExporterManager,
                             @NonNull List<StateInfo> stateInfoList) throws Exception {
        for (StateInfo stateInfo : stateInfoList) {
            if (logger.isDebugEnabled()) {
                logger.debug(ToStringBuilder.toString("Create StateExporter", stateInfo));
            }

            stateInfo.validateRequiredParameters();

            StateReader stateReader = createStateReader(stateExporterManager, stateInfo);
            StateExporter stateExporter = new StateExporter(stateExporterManager, stateInfo, stateReader);
            stateExporterManager.addExporter(stateExporter);
        }
    }

    @NonNull
    private static StateReader createStateReader(
            @NonNull StateExporterManager stateExporterManager, @NonNull StateInfo stateInfo) throws Exception {
        try {
            Class<StateReader> readerType = ClassUtils.classForName(stateInfo.getReader());
            Object[] args = { stateExporterManager, stateInfo };
            Class<?>[] argTypes = { StateExporterManager.class, StateInfo.class };
            return ClassUtils.createInstance(readerType, args, argTypes);
        } catch (Exception e) {
            throw new Exception(ToStringBuilder.toString("Failed to create state reader", stateInfo), e);
        }
    }

}
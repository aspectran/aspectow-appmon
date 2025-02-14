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
package com.aspectran.appmon.config;

import com.aspectran.utils.annotation.jsr305.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DomainInfoHolder {

    private final Map<String, DomainInfo> domainInfoMap = new LinkedHashMap<>();

    public DomainInfoHolder(@NonNull List<DomainInfo> domainInfoList) {
        for (DomainInfo info : domainInfoList) {
            domainInfoMap.put(info.getName(), info);
        }
    }

    public List<DomainInfo> getDomainInfoList() {
        return new ArrayList<>(domainInfoMap.values());
    }

}

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
package com.aspectran.appmon.engine.config;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A holder for managing a collection of {@link DomainInfo} objects.
 * Provides convenient access to domain information.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class DomainInfoHolder {

    private final Map<String, DomainInfo> domainInfoMap = new LinkedHashMap<>();

    /**
     * Instantiates a new DomainInfoHolder.
     * @param domainInfoList the list of domain information to hold
     */
    public DomainInfoHolder(@NonNull List<DomainInfo> domainInfoList) {
        for (DomainInfo info : domainInfoList) {
            domainInfoMap.put(info.getName(), info);
        }
    }

    /**
     * Gets the list of all held domain information.
     * @return a list of {@link DomainInfo} objects
     */
    public List<DomainInfo> getDomainInfoList() {
        return new ArrayList<>(domainInfoMap.values());
    }

    /**
     * Checks if a domain with the specified name exists.
     * @param domain the name of the domain
     * @return {@code true} if the domain exists, {@code false} otherwise
     */
    public boolean hasDomain(String domain) {
        return domainInfoMap.containsKey(domain);
    }

}

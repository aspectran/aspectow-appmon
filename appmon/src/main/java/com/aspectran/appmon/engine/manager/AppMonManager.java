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
package com.aspectran.appmon.engine.manager;

import com.aspectran.appmon.engine.config.DomainInfo;
import com.aspectran.appmon.engine.config.DomainInfoHolder;
import com.aspectran.appmon.engine.config.InstanceInfo;
import com.aspectran.appmon.engine.config.InstanceInfoHolder;
import com.aspectran.appmon.engine.config.PollingConfig;
import com.aspectran.appmon.engine.persist.PersistManager;
import com.aspectran.appmon.engine.service.ExportServiceManager;
import com.aspectran.core.activity.InstantAction;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.utils.security.TimeLimitedPBTokenIssuer;

import java.util.List;

/**
 * The main manager for Aspectow AppMon.
 * This class orchestrates the entire monitoring application, including configuration,
 * exporters, persistence, and lifecycle management.
 * It also provides access to the core components of Aspectran's ActivityContext.
 *
 * <p>Created: 4/3/2024</p>
 */
public class AppMonManager extends InstantActivitySupport {

    private final String currentDomain;

    private final PollingConfig pollingConfig;

    private final int counterPersistInterval;

    private final DomainInfoHolder domainInfoHolder;

    private final InstanceInfoHolder instanceInfoHolder;

    private final ExportServiceManager exportServiceManager;

    private final PersistManager persistManager;

    /**
     * Instantiates a new AppMonManager.
     * @param currentDomain the name of the current domain
     * @param pollingConfig the polling configuration
     * @param counterPersistInterval the counter persistence interval in minutes
     * @param domainInfoHolder the holder for domain information
     * @param instanceInfoHolder the holder for instance information
     */
    public AppMonManager(String currentDomain,
                         PollingConfig pollingConfig,
                         int counterPersistInterval,
                         DomainInfoHolder domainInfoHolder,
                         InstanceInfoHolder instanceInfoHolder) {
        this.currentDomain = currentDomain;
        this.pollingConfig = pollingConfig;
        this.counterPersistInterval = counterPersistInterval;
        this.domainInfoHolder = domainInfoHolder;
        this.instanceInfoHolder = instanceInfoHolder;
        this.exportServiceManager = new ExportServiceManager(instanceInfoHolder);
        this.persistManager = new PersistManager();
    }

    @Override
    @NonNull
    public ActivityContext getActivityContext() {
        return super.getActivityContext();
    }

    @Override
    @NonNull
    public ApplicationAdapter getApplicationAdapter() {
        return super.getApplicationAdapter();
    }

    /**
     * Gets the name of the current domain.
     * @return the current domain name
     */
    public String getCurrentDomain() {
        return currentDomain;
    }

    /**
     * Gets the polling configuration.
     * @return the polling configuration
     */
    public PollingConfig getPollingConfig() {
        return pollingConfig;
    }

    /**
     * Gets the counter persistence interval in minutes.
     * @return the interval in minutes
     */
    public int getCounterPersistInterval() {
        return counterPersistInterval;
    }

    /**
     * Gets the list of all domain information.
     * @return the list of domain information
     */
    public List<DomainInfo> getDomainInfoList() {
        return domainInfoHolder.getDomainInfoList();
    }

    /**
     * Gets the list of all instance information.
     * @return the list of instance information
     */
    public List<InstanceInfo> getInstanceInfoList() {
        return instanceInfoHolder.getInstanceInfoList();
    }

    /**
     * Gets the list of instance information for the specified instance names.
     * @param instanceNames an array of instance names
     * @return a list of matching instance information
     */
    public List<InstanceInfo> getInstanceInfoList(String[] instanceNames) {
        return instanceInfoHolder.getInstanceInfoList(instanceNames);
    }

    /**
     * Verifies the given instance names against the configured instances and returns the valid ones.
     * @param instanceNames an array of instance names to verify
     * @return an array of verified instance names
     */
    public String[] getVerifiedInstanceNames(String[] instanceNames) {
        List<InstanceInfo> infoList = getInstanceInfoList(instanceNames);
        if (!infoList.isEmpty()) {
            return InstanceInfoHolder.extractInstanceNames(infoList);
        } else {
            return new String[0];
        }
    }

    /**
     * Gets the manager for export services.
     * @return the export service manager
     */
    public ExportServiceManager getExportServiceManager() {
        return exportServiceManager;
    }

    /**
     * Gets the manager for persistence.
     * @return the persist manager
     */
    public PersistManager getPersistManager() {
        return persistManager;
    }

    @Override
    public <V> V instantActivity(InstantAction<V> instantAction) {
        return super.instantActivity(instantAction);
    }

    /**
     * Gets a bean from the ActivityContext's bean registry by its ID.
     * @param id the ID of the bean
     * @param <V> the type of the bean
     * @return the bean instance
     */
    public <V> V getBean(@NonNull String id) {
        return getActivityContext().getBeanRegistry().getBean(id);
    }

    /**
     * Gets a bean from the ActivityContext's bean registry by its type.
     * @param type the type of the bean
     * @param <V> the type of the bean
     * @return the bean instance
     */
    public <V> V getBean(Class<V> type) {
        return getActivityContext().getBeanRegistry().getBean(type);
    }

    /**
     * Checks if a bean of the given type exists in the ActivityContext's bean registry.
     * @param type the type of the bean
     * @return {@code true} if the bean exists, {@code false} otherwise
     */
    public boolean containsBean(Class<?> type) {
        return getActivityContext().getBeanRegistry().containsBean(type);
    }

    /**
     * Issues a time-limited token with a default expiration of 60 seconds.
     * @return the generated token
     */
    public static String issueToken() {
        return issueToken(60); // default 60 secs.
    }

    /**
     * Issues a time-limited token with a specified expiration time.
     * @param expirationTimeInSeconds the expiration time in seconds
     * @return the generated token
     */
    public static String issueToken(int expirationTimeInSeconds) {
        return TimeLimitedPBTokenIssuer.getToken(1000L * expirationTimeInSeconds);
    }

    /**
     * Validates the given time-limited token.
     * @param token the token to validate
     * @throws InvalidPBTokenException if the token is invalid or expired
     */
    public static void validateToken(String token) throws InvalidPBTokenException {
        TimeLimitedPBTokenIssuer.validate(token);
    }

}

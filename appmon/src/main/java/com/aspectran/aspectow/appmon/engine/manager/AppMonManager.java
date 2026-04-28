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
package com.aspectran.aspectow.appmon.engine.manager;

import com.aspectran.aspectow.appmon.engine.config.AppInfo;
import com.aspectran.aspectow.appmon.engine.config.AppInfoHolder;
import com.aspectran.aspectow.appmon.engine.config.PollingConfig;
import com.aspectran.aspectow.appmon.engine.persist.PersistManager;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.config.NodeInfoHolder;
import com.aspectran.core.activity.InstantAction;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.core.context.ActivityContext;
import org.jspecify.annotations.NonNull;

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

    private final String nodeId;

    private final PollingConfig pollingConfig;

    private final int counterPersistInterval;

    private final NodeInfoHolder nodeInfoHolder;

    private final AppInfoHolder appInfoHolder;

    private final MessageRelayManager messageRelayManager;

    private final PersistManager persistManager;

    /**
     * Instantiates a new AppMonManager.
     * @param nodeId the name of the current domain
     * @param pollingConfig the polling configuration
     * @param counterPersistInterval the counter persistence interval in minutes
     * @param nodeInfoHolder the holder for domain information
     * @param appInfoHolder the holder for instance information
     */
    public AppMonManager(
            String nodeId,
            PollingConfig pollingConfig,
            int counterPersistInterval,
            NodeInfoHolder nodeInfoHolder,
            AppInfoHolder appInfoHolder,
            MessageRelayManager messageRelayManager) {
        this.nodeId = nodeId;
        this.pollingConfig = pollingConfig;
        this.counterPersistInterval = counterPersistInterval;
        this.nodeInfoHolder = nodeInfoHolder;
        this.appInfoHolder = appInfoHolder;
        this.messageRelayManager = messageRelayManager;
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
    public String getNodeId() {
        return nodeId;
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
     * Gets the list of all node information.
     * @return the list of node information
     */
    public List<NodeInfo> getNodeInfoList() {
        return nodeInfoHolder.getNodeInfoList();
    }

    /**
     * Gets the list of all instance information.
     * @return the list of instance information
     */
    public List<AppInfo> getAppInfoList() {
        return appInfoHolder.getAppInfoList();
    }

    /**
     * Gets the list of instance information for the specified instance IDs.
     * @param appIds an array of instance IDs
     * @return a list of matching instance information
     */
    public List<AppInfo> getAppInfoList(String[] appIds) {
        return appInfoHolder.getAppInfoList(appIds);
    }

    /**
     * Verifies the given instance IDs against the configured instances and returns the valid ones.
     * @param appIds an array of instance IDs to verify
     * @return an array of verified instance IDs
     */
    public String[] getVerifiedAppIds(String[] appIds) {
        List<AppInfo> infoList = getAppInfoList(appIds);
        if (!infoList.isEmpty()) {
            return AppInfoHolder.extractAppIds(infoList);
        } else {
            return new String[0];
        }
    }

    /**
     * Gets the manager for message relayers.
     * @return the message relay manager
     */
    public MessageRelayManager getMessageRelayManager() {
        return messageRelayManager;
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

}

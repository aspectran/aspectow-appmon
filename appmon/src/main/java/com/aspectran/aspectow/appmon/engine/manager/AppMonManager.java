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
import com.aspectran.aspectow.appmon.engine.relay.remote.NodeMessageRelayHandler;
import com.aspectran.aspectow.node.config.GroupInfo;
import com.aspectran.aspectow.node.config.GroupInfoHolder;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.config.NodeInfoHolder;
import com.aspectran.aspectow.node.manager.ClusterEventListener;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeMessageProtocol;
import com.aspectran.aspectow.node.manager.NodeRegistry;
import com.aspectran.aspectow.node.manager.NodeRegistryListener;
import com.aspectran.core.activity.InstantAction;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.core.context.ActivityContext;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The main manager for Aspectow AppMon.
 * This class orchestrates the entire monitoring application, including configuration,
 * exporters, persistence, and lifecycle management.
 * It also provides access to the core components of Aspectran's ActivityContext.
 *
 * <p>Created: 4/3/2024</p>
 */
public class AppMonManager extends InstantActivitySupport {

    private static final Logger logger = LoggerFactory.getLogger(AppMonManager.class);

    private final NodeManager nodeManager;

    private final NodeInfoHolder nodeInfoHolder;

    private final GroupInfoHolder groupInfoHolder;

    private final AppInfoHolder appInfoHolder;

    private final PollingConfig pollingConfig;

    private final int counterPersistInterval;

    private final MessageRelayManager messageRelayManager;

    private final PersistManager persistManager;

    private NodeMessageRelayHandler nodeMessageRelayHandler;

    private ClusterEventListener clusterEventListener;

    private NodeRegistryListener nodeRegistryListener;

    /**
     * Instantiates a new AppMonManager.
     * @param nodeManager the node manager
     * @param appInfoHolder the holder for app information
     * @param pollingConfig the polling configuration
     * @param counterPersistInterval the counter persist interval in minutes
     */
    public AppMonManager(
            @NonNull NodeManager nodeManager,
            AppInfoHolder appInfoHolder,
            PollingConfig pollingConfig,
            int counterPersistInterval) {
        this.nodeManager = nodeManager;
        this.nodeInfoHolder = nodeManager.getNodeInfoHolder();
        this.groupInfoHolder = nodeManager.getGroupInfoHolder();
        this.appInfoHolder = appInfoHolder;
        this.pollingConfig = pollingConfig;
        this.counterPersistInterval = counterPersistInterval;
        this.messageRelayManager = new MessageRelayManager(nodeManager);
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
     * Gets the cluster mode.
     * @return the cluster mode
     */
    public String getClusterMode() {
        return nodeManager.getClusterConfig().getMode();
    }

    /**
     * Checks if the cluster is in gateway mode.
     * @return {@code true} if in gateway mode, {@code false} otherwise
     */
    public boolean isGatewayMode() {
        return nodeManager.getClusterConfig().isGatewayMode();
    }

    /**
     * Gets the name of the current node.
     * @return the current node ID
     */
    public String getNodeId() {
        return nodeManager.getNodeId();
    }

    /**
     * Gets the name of the current node group.
     * @return the current node group name
     */
    public String getGroupId() {
        return nodeManager.getGroupId();
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
     * Gets the list of instance information.
     * @return the list of instance information
     */
    public List<AppInfo> getAppInfoList() {
        return appInfoHolder.getAppInfoList();
    }

    /**
     * Gets the IDs of all instances.
     * @return an array of instance IDs
     */
    public String[] getAppIds() {
        return AppInfoHolder.extractAppIds(getAppInfoList());
    }

    /**
     * Gets the list of all application definitions in the cluster.
     * In gateway mode, this retrieves information from the Redis registry.
     * @return the list of all application definitions
     */
    public List<AppInfo> getClusterAppInfoList() {
        if (isGatewayMode()) {
            NodeRegistry nodeRegistry = nodeManager.getNodeRegistry();
            List<AppInfo> apps = new ArrayList<>();
            Map<String, String> allGroups = nodeRegistry.getAllGroups();
            for (String groupId : allGroups.keySet()) {
                Map<String, String> allApps = nodeRegistry.getAllApps(groupId);
                for (String aponData : allApps.values()) {
                    try {
                        AppInfo appInfo = new AppInfo();
                        appInfo.readFrom(aponData);
                        apps.add(appInfo);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            return apps;
        } else {
            return appInfoHolder.getAppInfoList();
        }
    }

    /**
     * Gets the list of all group information.
     * @return the list of group information
     */
    public List<GroupInfo> getGroupInfoList() {
        if (isGatewayMode()) {
            NodeRegistry nodeRegistry = nodeManager.getNodeRegistry();
            List<GroupInfo> groups = new ArrayList<>();
            Map<String, String> allGroups = nodeRegistry.getAllGroups();
            for (String aponData : allGroups.values()) {
                try {
                    GroupInfo groupInfo = new GroupInfo();
                    groupInfo.readFrom(aponData);
                    groups.add(groupInfo);
                } catch (Exception e) {
                    // ignore
                }
            }
            return groups;
        } else {
            return List.copyOf(groupInfoHolder.getGroupInfos());
        }
    }

    /**
     * Verifies the given instance IDs against the configured instances and returns the valid ones.
     * @param appIds an array of instance IDs to verify
     * @param allAppInfoList a list of all available application definitions
     * @return an array of verified instance IDs
     */
    public String[] getVerifiedAppIds(String[] appIds, List<AppInfo> allAppInfoList) {
        return getVerifiedAppIds(appIds, allAppInfoList, false);
    }

    /**
     * Verifies the given instance IDs against the configured instances and returns the valid ones.
     * @param appIds an array of instance IDs to verify
     * @param allAppInfoList a list of all available application definitions
     * @param includeHidden whether to include hidden apps when appIds is not specified
     * @return an array of verified instance IDs
     */
    public String[] getVerifiedAppIds(String[] appIds, List<AppInfo> allAppInfoList, boolean includeHidden) {
        if (allAppInfoList == null || allAppInfoList.isEmpty()) {
            return new String[0];
        }
        List<AppInfo> infoList = new ArrayList<>(allAppInfoList.size());
        if (appIds != null && appIds.length > 0) {
            for (String id : appIds) {
                for (AppInfo info : allAppInfoList) {
                    if (info.getAppId().equals(id)) {
                        infoList.add(info);
                    }
                }
            }
        } else {
            for (AppInfo info : allAppInfoList) {
                if (includeHidden || !info.isHidden()) {
                    infoList.add(info);
                }
            }
        }
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

    protected void setNodeMessageRelayHandler(NodeMessageRelayHandler nodeMessageRelayHandler) {
        this.nodeMessageRelayHandler = nodeMessageRelayHandler;
    }

    protected void setClusterEventListener(ClusterEventListener clusterEventListener) {
        this.clusterEventListener = clusterEventListener;
    }

    protected void setNodeRegistryListener(NodeRegistryListener nodeRegistryListener) {
        this.nodeRegistryListener = nodeRegistryListener;
    }

    /**
     * Checks if the registered applications in Redis for this group exactly match the local app info list.
     * @param nodeRegistry the node registry
     * @return true if Redis contains the exact same apps in the same order and content, false otherwise
     */
    private boolean isAppsUpToDate(NodeRegistry nodeRegistry) {
        if (nodeRegistry == null) {
            return false;
        }
        Map<String, String> registeredApps = nodeRegistry.getAllApps(getGroupId());
        List<AppInfo> myAppList = getAppInfoList();
        if (registeredApps.size() != myAppList.size()) {
            return false;
        }
        int index = 0;
        for (Map.Entry<String, String> entry : registeredApps.entrySet()) {
            AppInfo current = myAppList.get(index++);
            if (!entry.getKey().equals(current.getAppId())) {
                return false;
            }
            if (!entry.getValue().equals(current.toString())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Registers application metadata to the Redis cluster registry.
     */
    protected void registerAppInfoToRedis() {
        if (isGatewayMode() && nodeManager.getRedisConnectionPool() != null && nodeManager.getRedisConnectionPool().isAvailable()) {
            NodeRegistry nodeRegistry = nodeManager.getNodeRegistry();
            if (isAppsUpToDate(nodeRegistry)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("App info in Redis for group '{}' is already up to date. Skipping registration.", getGroupId());
                }
                return;
            }

            String clusterId = nodeManager.getClusterConfig().getId();
            String appsKey = NodeMessageProtocol.getAppsHashKey(clusterId, getGroupId());
            String appsOrderKey = NodeMessageProtocol.getAppsOrderKey(clusterId, getGroupId());
            try (StatefulRedisConnection<String, String> connection = nodeManager.getRedisConnectionPool().getConnection()) {
                RedisCommands<String, String> sync = connection.sync();
                sync.multi();
                sync.del(appsKey);
                sync.del(appsOrderKey);
                for (AppInfo appInfo : getAppInfoList()) {
                    sync.hset(appsKey, appInfo.getAppId(), appInfo.toString());
                    sync.rpush(appsOrderKey, appInfo.getAppId());
                }
                sync.exec();
                logger.info("Synchronized app info to Redis: {} (Apps: {})", appsKey, getAppIds());
            } catch (Exception e) {
                if (nodeManager.getRedisConnectionPool().isAvailable()) {
                    logger.error("Failed to register app info to Redis", e);
                }
            }
        }
    }

    /**
     * Unregisters application metadata from the Redis cluster registry.
     */
    protected void unregisterAppInfoFromRedis() {
        if (isGatewayMode() && nodeManager.getRedisConnectionPool() != null && nodeManager.getRedisConnectionPool().isAvailable()) {
            NodeRegistry nodeRegistry = nodeManager.getNodeRegistry();
            if (nodeRegistry != null && nodeRegistry.hasOtherNodesInGroup(getGroupId(), getNodeId())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping unregister of app info from Redis because other nodes exist in group '{}'", getGroupId());
                }
                return;
            }

            String clusterId = nodeManager.getClusterConfig().getId();
            String appsKey = NodeMessageProtocol.getAppsHashKey(clusterId, getGroupId());
            String appsOrderKey = NodeMessageProtocol.getAppsOrderKey(clusterId, getGroupId());
            try (StatefulRedisConnection<String, String> connection = nodeManager.getRedisConnectionPool().getConnection()) {
                RedisCommands<String, String> sync = connection.sync();
                sync.del(appsKey);
                sync.del(appsOrderKey);
                logger.info("Unregistered app info from Redis: {}", appsKey);
            } catch (Exception e) {
                if (nodeManager.getRedisConnectionPool().isAvailable()) {
                    logger.error("Failed to unregister app info from Redis", e);
                }
            }
        }
    }

    /**
     * Closes and releases all resources managed by this AppMonManager.
     */
    public void destroy() {
        unregisterAppInfoFromRedis();
        if (messageRelayManager != null) {
            messageRelayManager.destroy();
        }
        if (nodeMessageRelayHandler != null && nodeManager.getNodeMessageSubscriber() != null) {
            nodeManager.getNodeMessageSubscriber().removeListener(nodeMessageRelayHandler);
        }
        if (clusterEventListener != null && nodeManager.getClusterEventSubscriber() != null) {
            nodeManager.getClusterEventSubscriber().removeListener(clusterEventListener);
        }
        if (nodeRegistryListener != null && nodeManager.getNodeRegistry() != null) {
            nodeManager.getNodeRegistry().removeListener(nodeRegistryListener);
        }
    }

}

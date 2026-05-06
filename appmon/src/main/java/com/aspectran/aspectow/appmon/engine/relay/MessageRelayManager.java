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
package com.aspectran.aspectow.appmon.engine.relay;

import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeRegistry;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_REFRESH;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_SUBSCRIBE;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_UNSUBSCRIBE;

/**
 * Manages all {@link MessageRelayer} and {@link ExporterManager} apps.
 * This class is a central hub for handling client sessions (join/release),
 * collecting messages from exporters, and relaying them to clients.
 *
 * <p>Created: 2025-02-12</p>
 */
public class MessageRelayManager {

    private static final Logger logger = LoggerFactory.getLogger(MessageRelayManager.class);

    public static final String CATEGORY_APPMON = "appmon";

    private final Set<MessageRelayer> messageRelayers = new CopyOnWriteArraySet<>();

    private final List<ExporterManager> exporterManagers = new CopyOnWriteArrayList<>();

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    private final String nodeId;

    private final NodeRegistry nodeRegistry;

    private final RedisMessagePublisher messagePublisher;

    /**
     * Instantiates a new MessageRelayManager.
     * @param messagePublisher the Redis message publisher
     */
    public MessageRelayManager(String nodeId, NodeRegistry nodeRegistry, RedisMessagePublisher messagePublisher) {
        this.nodeId = nodeId;
        this.nodeRegistry = nodeRegistry;
        this.messagePublisher = messagePublisher;
    }

    public SubscriptionRegistry getSubscriptionRegistry() {
        return subscriptionRegistry;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isSameNode(String nodeId) {
        return (nodeId != null && nodeId.equals(this.nodeId));
    }

    /**
     * Checks if the manager is running in gateway mode.
     * @return {@code true} if in gateway mode, {@code false} otherwise
     */
    public boolean isGatewayMode() {
        return (messagePublisher != null);
    }

    /**
     * Adds a message relayer to the manager.
     * @param messageRelayer the message relayer to add
     */
    public void addRelayer(MessageRelayer messageRelayer) {
        messageRelayers.add(messageRelayer);
    }

    /**
     * Removes a message relayer from the manager.
     * @param messageRelayer the message relayer to remove
     */
    public void removeRelayer(MessageRelayer messageRelayer) {
        messageRelayers.remove(messageRelayer);
    }

    /**
     * Adds an exporter manager to this manager.
     * @param exporterManager the exporter manager to add
     */
    public void addExporterManager(ExporterManager exporterManager) {
        exporterManagers.add(exporterManager);
    }

    private void startExporters(String appId) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (appId == null || exporterManager.getAppId().equals(appId)) {
                exporterManager.start();
            }
        }
    }

    private void stopExporters(String appId) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (appId == null || exporterManager.getAppId().equals(appId)) {
                exporterManager.stop();
            }
        }
    }

    /**
     * Publishes a local message to Redis and relays it to all registered relayers.
     * @param message the message to publish
     */
    public void broadcast(String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishRelay(CATEGORY_APPMON, message);
            } catch (Exception e) {
                logger.error("Failed to publish relay message to Redis", e);
            }
        } else {
            relayLocally(message);
        }
    }

    /**
     * Publishes a management control message for this node.
     * @param message the control message to publish
     */
    private void publishControl(String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishControl(message);
            } catch (Exception e) {
                logger.error("Failed to publish control message to Redis", e);
            }
        }
    }

    private void publishRelay(String targetNodeId, String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishRelay(CATEGORY_APPMON, targetNodeId, message);
            } catch (Exception e) {
                logger.error("Failed to publish control message to Redis", e);
            }
        }
    }

    private void publishControl(String targetNodeId, @NonNull CommandOptions commandOptions) {
        if (messagePublisher != null) {
            publishControl(targetNodeId, commandOptions.toString(false));
        }
    }

    private void publishControl(String targetNodeId, String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishControl(CATEGORY_APPMON, targetNodeId, message);
            } catch (Exception e) {
                logger.error("Failed to publish control message to node {}", targetNodeId, e);
            }
        }
    }

    /**
     * Relays a message to all registered relayers.
     * This method does not publish the message to Redis.
     * @param message the message to relay
     */
    public void relayLocally(@NonNull String message) {
        String appId = null;
        boolean isLog = false;

        int idx1 = message.indexOf(':');
        if (idx1 != -1) {
            appId = message.substring(0, idx1);
            int idx2 = message.indexOf(':', idx1 + 1);
            if (idx2 != -1) {
                String type = message.substring(idx1 + 1, idx2);
                if (type.startsWith("log")) {
                    isLog = true;
                }
            }
        }

        if (isGatewayMode()) {
            // In gateway mode, we frame with nodeId\n and filter logs by focus
            Set<String> allSessionIds = subscriptionRegistry.getAllSessionIds();
            for (MessageRelayer relayer : messageRelayers) {
                for (String sessionId : allSessionIds) {
                    RelaySession session = relayer.getLocalRelaySession(sessionId);
                    if (session != null && session.isValid()) {
                        if (isLog) {
                            //String targetFocus = nodeId + "/" + appId;
                            String targetFocus = appId;
                            if (targetFocus.equals(session.getFocusedAppId())) {
                                relayer.relay(session, message);
                            }
                        } else {
                            relayer.relay(session, message);
                        }
                    }
                }
            }
        } else {
            for (MessageRelayer relayer : messageRelayers) {
                relayer.relay(message);
            }
        }
    }

    @Nullable
    public RelaySession findRelaySession(String sessionId) {
        for (MessageRelayer relayer : messageRelayers) {
            RelaySession session = relayer.getLocalRelaySession(sessionId);
            if (session != null) {
                return session;
            }
        }
        return null;
    }

    /**
     * Relays a message to a specific session via all registered relayers.
     * @param session the target relay session
     * @param message the message to relay
     */
    public void relay(RelaySession session, String message) {
        for (MessageRelayer relayer : messageRelayers) {
            relayer.relay(session, message);
        }
    }

    /**
     * Handles a client joining to monitor apps.
     * Starts the necessary exporters for the joined apps.
     * @param session the client session that is joining
     * @return {@code true} if the join was successful, {@code false} otherwise
     */
    public synchronized boolean subscribe(@NonNull RelaySession session, String targetNodeId) {
        if (session.isValid()) {
            String[] appIds = session.getJoinedApps();
            subscriptionRegistry.addLocalSubscription(session.getId(), appIds);
            if (appIds != null && appIds.length > 0) {
                for (String appId : appIds) {
                    if (targetNodeId == null || isSameNode(targetNodeId)) {
                        startExporters(appId);
                    }
                    CommandOptions options = new CommandOptions();
                    options.setCommand(COMMAND_SUBSCRIBE);
                    options.setNodeId(targetNodeId);
                    options.setAppId(appId);
                    options.setTimeZone(session.getTimeZone());
                    for (NodeInfo nodeInfo : nodeRegistry.getNodes()) {
                        if (!isSameNode(nodeInfo.getNodeId())) {
                            publishControl(nodeInfo.getNodeId(), options);
                        }
                    }
                }
            } else {
                startExporters(null);
                CommandOptions options = new CommandOptions();
                options.setCommand(COMMAND_SUBSCRIBE);
                options.setNodeId(targetNodeId);
                options.setTimeZone(session.getTimeZone());
                for (NodeInfo nodeInfo : nodeRegistry.getNodes()) {
                    if (!isSameNode(nodeInfo.getNodeId())) {
                        publishControl(nodeInfo.getNodeId(), options);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public void subscribeRemotely(String nodeId, @NonNull CommandOptions commandOptions) {
        String appId = commandOptions.getAppId();
        subscriptionRegistry.addRemoteSubscription(nodeId, appId);
        startExporters(appId);
        List<String> messages = getLastMessages(commandOptions);
        for (String message : messages) {
            publishRelay(nodeId, message);
        }
    }

    /**
     * Handles a client releasing its monitoring session.
     * Stops exporters that are no longer being monitored by any client.
     * @param session the client session that is being released
     */
    public synchronized void unsubscribe(@NonNull RelaySession session) {
        String[] appIds = session.getJoinedApps();
        subscriptionRegistry.removeLocalSubscription(session.getId());
        if (appIds != null && appIds.length > 0) {
            for (String appId : appIds) {
                if (!subscriptionRegistry.isAppInUseLocally(appId)) {
                    stopExporters(appId);
                }
                if (isGatewayMode()) {
                    CommandOptions options = new CommandOptions();
                    options.setCommand(COMMAND_UNSUBSCRIBE);
                    options.setAppId(appId);
                    String message = options.toString(false);
                    Set<String> remoteNodeIds = subscriptionRegistry.getNodeIdsRemotelySubscribedToApp(appId);
                    if (remoteNodeIds != null) {
                        for (String remoteNodeId : remoteNodeIds) {
                            publishControl(remoteNodeId, message);
                        }
                    }
                }
            }
        } else {
            if (!subscriptionRegistry.isAppInUseLocally(null)) {
                stopExporters(null);
            }
            if (isGatewayMode()) {
                CommandOptions options = new CommandOptions();
                options.setCommand(COMMAND_UNSUBSCRIBE);
                String message = options.toString(false);
                Set<String> remoteNodeIds = subscriptionRegistry.getNodeIdsRemotelySubscribedToApp(null);
                if (remoteNodeIds != null) {
                    for (String remoteNodeId : remoteNodeIds) {
                        publishControl(remoteNodeId, message);
                    }
                }
            }
        }
        session.removeJoinedApps();
    }

    public void unsubscribeRemotely(String nodeId, String appId) {
        subscriptionRegistry.removeRemoteSubscription(nodeId, appId);
        if (!subscriptionRegistry.isAppInUse(appId)) {
            stopExporters(appId);
        }
    }

    public void refreshDataRemotely(String nodeId, @NonNull CommandOptions commandOptions) {
        if (messagePublisher != null) {
            String appId = commandOptions.getAppId();
            List<String> messages = new ArrayList<>();
            collectNewMessages(appId, messages, commandOptions);
            for (String message : messages) {
                publishRelay(nodeId, message);
            }
        }
    }

    @Nullable
    public List<String> refreshData(@NonNull RelaySession session, @NonNull CommandOptions commandOptions) {
        if (!commandOptions.hasTimeZone()) {
            commandOptions.setTimeZone(session.getTimeZone());
        }
        String targetNodeId = commandOptions.getNodeId();
        if (targetNodeId == null || isSameNode(targetNodeId)) {
            return getNewMessages(session, commandOptions);
        }
        if (messagePublisher != null) {
            commandOptions.setCommand(COMMAND_REFRESH);
            publishControl(targetNodeId, commandOptions);
        }
        return null;
    }

    /**
     * Gets the last known messages for the apps joined by the session.
     * @param session the client session
     * @return a list of messages
     */
    public List<String> getLastMessages(@NonNull RelaySession session) {
        if (session.isValid()) {
            List<String> messages = new ArrayList<>();
            CommandOptions commandOptions = new CommandOptions();
            commandOptions.setTimeZone(session.getTimeZone());
            String[] appIds = session.getJoinedApps();
            if (appIds != null && appIds.length > 0) {
                for (String id : appIds) {
                    commandOptions.setAppId(id);
                    collectLastMessages(messages, commandOptions);
                }
            } else {
                collectLastMessages(messages, commandOptions);
            }
        }
        return List.of();
    }

    public List<String> getLastMessages(@NonNull CommandOptions commandOptions) {
        List<String> messages = new ArrayList<>();
        collectLastMessages(messages, commandOptions);
        return messages;
    }

    private void collectLastMessages(List<String> messages, @NonNull CommandOptions commandOptions) {
        String appId = commandOptions.getAppId();
        for (ExporterManager exporterManager : exporterManagers) {
            if (appId == null || exporterManager.getAppId().equals(appId)) {
                exporterManager.collectMessages(messages, commandOptions);
            }
        }
    }

    /**
     * Gets new or changed messages based on the provided command options.
     * @param session the client session
     * @param commandOptions the command options specifying what to refresh
     * @return a list of new messages
     */
    @NonNull
    public List<String> getNewMessages(RelaySession session, @NonNull CommandOptions commandOptions) {
        String appId = commandOptions.getAppId();
        List<String> messages = new ArrayList<>();
        if (session == null || session.isValid()) {
            String[] appIds = (session != null ? session.getJoinedApps() : null);
            if (appIds != null) {
                for (String id : appIds) {
                    if (appId == null || id.equals(appId)) {
                        collectNewMessages(id, messages, commandOptions);
                    }
                }
            } else {
                collectNewMessages(appId, messages, commandOptions);
            }
        }
        return messages;
    }

    private void collectNewMessages(String appId, List<String> messages, CommandOptions commandOptions) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (appId == null || exporterManager.getAppId().equals(appId)) {
                exporterManager.collectNewMessages(messages, commandOptions);
            }
        }
    }

    /**
     * Destroys the manager, stopping all exporters.
     */
    public void destroy() {
        for (ExporterManager exporterManager : exporterManagers) {
            exporterManager.stop();
        }
        exporterManagers.clear();
    }

}

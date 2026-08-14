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
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeMessagePublisher;
import com.aspectran.aspectow.node.manager.NodeRegistry;
import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.json.JsonBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_SUBSCRIBE;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_UNSUBSCRIBE;

/**
 * Manages all {@link MessageRelayer} and {@link ExporterManager} apps.
 * This class is a central hub for handling client sessions (subscribe/release),
 * collecting messages from exporters, and relaying them to clients.
 *
 * <p>Created: 2025-02-12</p>
 */
public class MessageRelayManager {

    private static final Logger logger = LoggerFactory.getLogger(MessageRelayManager.class);

    public static final String CATEGORY_APPMON = "appmon";

    private static final char DELIMITER = ':';

    private final Map<String, MessageRelayer> sessionRelayerMap = new ConcurrentHashMap<>();

    private final List<ExporterManager> exporterManagers = new CopyOnWriteArrayList<>();

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    private final String nodeId;

    private final String groupId;

    private final NodeRegistry nodeRegistry;

    private final NodeMessagePublisher messagePublisher;

    /**
     * Instantiates a new MessageRelayManager.
     * @param nodeManager the node manager
     */
    public MessageRelayManager(@NonNull NodeManager nodeManager) {
        this.nodeId = nodeManager.getNodeId();
        this.groupId = nodeManager.getGroupId();
        this.nodeRegistry = nodeManager.getNodeRegistry();
        this.messagePublisher = nodeManager.getNodeMessagePublisher();
    }

    /**
     * Returns the node registry used for tracking active nodes in the cluster.
     * @return the node registry
     */
    public NodeRegistry getNodeRegistry() {
        return nodeRegistry;
    }

    /**
     * Returns the current node's identifier.
     * @return the node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Returns the group identifier that this node belongs to.
     * @return the group ID
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Checks if the given node ID matches this node's ID.
     * @param targetNodeId the node ID to compare
     * @return true if targetNodeId is equal to this node's ID, false otherwise
     */
    public boolean isSameNode(String targetNodeId) {
        return (targetNodeId != null && targetNodeId.equals(nodeId));
    }

    /**
     * Checks if the manager is running in gateway mode.
     * @return {@code true} if in gateway mode, {@code false} otherwise
     */
    public boolean isGatewayMode() {
        return (messagePublisher != null);
    }

    /**
     * Registers a new client session with its corresponding message relayer.
     * @param sessionId the session identifier
     * @param messageRelayer the message relayer handling the session
     */
    public void registerSession(String sessionId, MessageRelayer messageRelayer) {
        sessionRelayerMap.put(sessionId, messageRelayer);
    }

    /**
     * Unregisters a client session.
     * @param sessionId the session identifier to unregister
     */
    public void unregisterSession(String sessionId) {
        sessionRelayerMap.remove(sessionId);
    }

    /**
     * Adds an exporter manager to this manager.
     * @param exporterManager the exporter manager to add
     */
    public void addExporterManager(ExporterManager exporterManager) {
        exporterManagers.add(exporterManager);
    }

    private void startExporters(String appId) {
        Assert.hasText(appId, "appId must not be null or empty");
        for (ExporterManager exporterManager : exporterManagers) {
            if (exporterManager.getAppId().equals(appId)) {
                exporterManager.start();
            }
        }
    }

    private void stopExporters(String appId) {
        Assert.hasText(appId, "appId must not be null or empty");
        for (ExporterManager exporterManager : exporterManagers) {
            if (exporterManager.getAppId().equals(appId)) {
                exporterManager.stop();
            }
        }
    }

    /**
     * Handles the event when a new node joins the cluster.
     * If the joining node is not the current node, it serializes its information and relays it locally.
     * @param info the metadata of the joined node
     */
    public void nodeJoined(@NonNull NodeInfo info) {
        if (!isSameNode(info.getId())) {
            JsonBuilder jsonBuilder = new JsonBuilder()
                    .nullWritable(false)
                    .prettyPrint(false)
                    .put(info);
            relayLocally(info.getId() + "::node:joined:" + jsonBuilder.toString());
        }
    }

    /**
     * Handles the event when a node's status changes in the cluster.
     * If the node is not the current node, it serializes its updated information and relays it locally.
     * @param info the updated metadata of the node
     */
    public void nodeStatusChanged(@NonNull NodeInfo info) {
        if (!isSameNode(info.getId())) {
            JsonBuilder jsonBuilder = new JsonBuilder()
                    .nullWritable(false)
                    .prettyPrint(false)
                    .put(info);
            relayLocally(info.getId() + "::node:statusChanged:" + jsonBuilder.toString());
        }
    }

    /**
     * Handles the event when a node leaves the cluster.
     * If the leaving node is not the current node, it relays the event locally.
     * @param nodeId the ID of the node that left
     */
    public void nodeLeft(String nodeId) {
        if (!isSameNode(nodeId)) {
            relayLocally(nodeId + "::node:left");
        }
    }

    /**
     * Publishes a local message to Redis and relays it to all registered relayers.
     * @param message the message to publish
     */
    public void broadcast(String message) {
        relayLocally(message);
        if (isGatewayMode()) {
            String appId = extractAppId(message);
            if (appId != null) {
                Set<String> remoteNodeIds = subscriptionRegistry.getNodeIdsRemotelySubscribedToApp(appId);
                if (remoteNodeIds != null) {
                    for (String remoteNodeId : remoteNodeIds) {
                        publishRelay(remoteNodeId, message);
                    }
                }
            } else {
                for (NodeInfo nodeInfo : nodeRegistry.getNodes()) {
                    if (!isSameNode(nodeInfo.getId())) {
                        publishRelay(nodeInfo.getId(), message);
                    }
                }
            }
        }
    }

    private void publishRelay(String targetNodeId, String message) {
        publishRelay(targetNodeId, null, message);
    }

    private void publishRelay(String targetNodeId, String sessionId, String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishRelay(CATEGORY_APPMON, targetNodeId, sessionId, message);
            } catch (Exception e) {
                logger.error("Failed to publish relay message to node {}", targetNodeId, e);
            }
        }
    }

    private void publishControl(String targetNodeId, @NonNull CommandOptions commandOptions) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishControl(CATEGORY_APPMON, targetNodeId, commandOptions.toString());
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
    public void relayLocally(String message) {
        Assert.notNull(message, "message must not be null");
        String messageNodeId = extractNodeId(message);
        String messageAppId = extractAppId(message);
        boolean isLog = isLogMessage(message);
        Set<String> sessionIds;
        if (messageAppId != null) {
            sessionIds = subscriptionRegistry.getSessionsSubscribedToApp(messageAppId);
        } else {
            sessionIds = subscriptionRegistry.getAllSessionIds();
        }
        for (String sid : sessionIds) {
            MessageRelayer relayer = sessionRelayerMap.get(sid);
            if (relayer != null) {
                RelaySession session = relayer.findRelaySession(sid);
                if (session != null) {
                    relayLocally(relayer, session, message, messageNodeId, messageAppId, isLog);
                }
            }
        }
    }

    /**
     * Relays a message to all registered relayers.
     * This method does not publish the message to Redis.
     * @param sessionId the target session ID
     * @param message the message to relay
     */
    public void relayLocally(String sessionId, String message) {
        Assert.notNull(sessionId, "sessionId must not be null");
        Assert.notNull(message, "message must not be null");
        MessageRelayer relayer = sessionRelayerMap.get(sessionId);
        if (relayer != null) {
            RelaySession session = relayer.findRelaySession(sessionId);
            if (session != null) {
                String messageNodeId = extractNodeId(message);
                String messageAppId = extractAppId(message);
                boolean isLog = isLogMessage(message);
                relayLocally(relayer, session, message, messageNodeId, messageAppId, isLog);
            }
        }
    }

    private void relayLocally(
            @NonNull MessageRelayer relayer, @NonNull RelaySession session, @NonNull String message,
            @Nullable String messageNodeId, @Nullable String messageAppId, boolean isLog) {
        String subscribedNodeId = session.getSubscribedNodeId();
        if (StringUtils.hasText(subscribedNodeId) && messageNodeId != null) {
            if (!subscribedNodeId.equals(messageNodeId)) {
                return;
            }
        }
        if (messageAppId != null && isLog) {
            String focusedAppId = session.getFocusedAppId();
            if (focusedAppId != null && !focusedAppId.equals(messageAppId)) {
                return;
            }
        }
        if (session.isValid()) {
            relayer.relay(session, message);
        }
    }

    @Nullable
    private String extractNodeId(@NonNull String message) {
        int idx = message.indexOf(DELIMITER);
        if (idx != -1) {
            String nodeId = message.substring(0, idx);
            return (!nodeId.isEmpty() ? nodeId : null);
        }
        return null;
    }

    @Nullable
    private String extractAppId(@NonNull String message) {
        int idx1 = message.indexOf(DELIMITER);
        if (idx1 != -1) {
            int idx2 = message.indexOf(DELIMITER, idx1 + 1);
            if (idx2 != -1) {
                String appId = message.substring(idx1 + 1, idx2);
                return (!appId.isEmpty() ? appId : null);
            }
        }
        return null;
    }

    @Nullable
    private String extractType(@NonNull String message) {
        int idx1 = message.indexOf(DELIMITER);
        if (idx1 != -1) {
            int idx2 = message.indexOf(DELIMITER, idx1 + 1);
            if (idx2 != -1) {
                int idx3 = message.indexOf(DELIMITER, idx2 + 1);
                if (idx3 != -1) {
                    return message.substring(idx2 + 1, idx3);
                }
            }
        }
        return null;
    }

    private boolean isLogMessage(@NonNull String message) {
        return "log".equals(extractType(message));
    }

    /**
     * Handles a client subscribing to monitor apps.
     * Starts the necessary exporters for the subscribed apps.
     * @param session the client session that is subscribing
     * @param nodeId the node id of the client session
     * @param singleNodeDesignated whether a specific single node is designated
     * @return {@code true} if the subscription was successful, {@code false} otherwise
     */
    public synchronized boolean subscribe(@NonNull RelaySession session, String nodeId, boolean singleNodeDesignated) {
        if (!session.isValid()) {
            return false;
        }
        String[] subscribedApps = session.getSubscribedApps();
        if (subscribedApps == null || subscribedApps.length == 0) {
            return false;
        }
        CommandOptions commandOptions = new CommandOptions();
        commandOptions.setCommand(COMMAND_SUBSCRIBE);
        commandOptions.setNodeId(getNodeId());
        commandOptions.setSessionId(session.getId());
        commandOptions.setTimeZone(session.getTimeZone());
        for (String appId : subscribedApps) {
            if (isSameNode(nodeId) && !subscriptionRegistry.isAppInUse(appId)) {
                startExporters(appId);
            }
            if (isGatewayMode()) {
                commandOptions.setAppId(appId);
                if (singleNodeDesignated) {
                    if (!isSameNode(nodeId)) {
                        publishControl(nodeId, commandOptions);
                    }
                } else {
                    for (NodeInfo nodeInfo : nodeRegistry.getNodes()) {
                        if (!isSameNode(nodeInfo.getId())) {
                            publishControl(nodeInfo.getId(), commandOptions);
                        }
                    }
                }
            }
        }
        subscriptionRegistry.addLocalSubscription(session.getId(), subscribedApps);
        return true;
    }

    /**
     * Subscribes remotely to monitor an application based on the provided command options.
     * This method ensures that the necessary exporters are started for the specified app
     * and handles relaying any last known messages to the specified session, if applicable.
     * @param commandOptions the command options containing details for the remote subscription,
     *                       such as node ID, app ID, and session ID; must not be null
     */
    public synchronized void subscribeRemotely(CommandOptions commandOptions) {
        Assert.notNull(commandOptions, "Command options must not be null");
        String nodeId = commandOptions.getNodeId();
        String appId = commandOptions.getAppId();
        String sessionId = commandOptions.getSessionId();

        boolean matched = false;
        for (ExporterManager exporterManager : exporterManagers) {
            if (exporterManager.getAppId().equals(appId)) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return;
        }

        if (!subscriptionRegistry.isAppInUse(appId)) {
            startExporters(appId);
        }
        subscriptionRegistry.addRemoteSubscription(nodeId, appId);
        List<String> messages = getLastMessages(commandOptions);
        if (sessionId != null) {
            for (String message : messages) {
                publishRelay(nodeId, sessionId, message);
            }
        }
    }

    /**
     * Handles a client releasing its monitoring session.
     * Stops exporters that are no longer being monitored by any client.
     * @param session the client session that is being released
     */
    public synchronized void unsubscribe(@NonNull RelaySession session) {
        subscriptionRegistry.removeLocalSubscription(session.getId());
        String[] subscribedApps = session.getSubscribedApps();
        if (subscribedApps != null) {
            for (String appId : subscribedApps) {
                if (!subscriptionRegistry.isAppInUse(appId)) {
                    stopExporters(appId);
                }
                if (isGatewayMode()) {
                    CommandOptions commandOptions = new CommandOptions();
                    commandOptions.setCommand(COMMAND_UNSUBSCRIBE);
                    commandOptions.setNodeId(getNodeId());
                    commandOptions.setAppId(appId);
                    for (NodeInfo nodeInfo : nodeRegistry.getNodes()) {
                        if (!isSameNode(nodeInfo.getId())) {
                            publishControl(nodeInfo.getId(), commandOptions);
                        }
                    }
                }
            }
        }
        session.removeSubscribedApps();
    }

    /**
     * Removes a remote subscription based on the provided command options and stops
     * exporters if the associated application is no longer in use.
     * @param commandOptions the command options containing the details of the
     *                       remote subscription to be removed; must not be null
     */
    public synchronized void unsubscribeRemotely(CommandOptions commandOptions) {
        Assert.notNull(commandOptions, "Command options must not be null");
        String nodeId = commandOptions.getNodeId();
        String appId = commandOptions.getAppId();
        subscriptionRegistry.removeRemoteSubscription(nodeId, appId);
        if (!subscriptionRegistry.isAppInUse(appId)) {
            stopExporters(appId);
        }
    }

    /**
     * Refreshes the data for a given session based on the command options.
     * If the target is the current node, it retrieves new messages locally.
     * Otherwise, in gateway mode, it forwards the refresh command to the target remote node.
     * @param session the client session requesting the refresh
     * @param commandOptions the command options indicating what data to refresh
     * @return a list of new messages if processed locally; {@code null} otherwise
     */
    @Nullable
    public List<String> refreshData(@NonNull RelaySession session, CommandOptions commandOptions) {
        Assert.notNull(commandOptions, "Command options must not be null");
        if (!commandOptions.hasTimeZone()) {
            commandOptions.setTimeZone(session.getTimeZone());
        }
        String targetNodeId = commandOptions.getNodeId();
        if (isSameNode(targetNodeId)) {
            return getNewMessages(session, commandOptions);
        }
        if (isGatewayMode()) {
            commandOptions.setNodeId(getNodeId());
            commandOptions.setSessionId(session.getId());
            publishControl(targetNodeId, commandOptions);
        }
        return null;
    }

    /**
     * Processes a data refresh request received from a remote node.
     * It collects new messages locally and publishes them back to the requesting node and session.
     * @param commandOptions the command options carrying the refresh request details
     */
    public void refreshDataRemotely(CommandOptions commandOptions) {
        Assert.notNull(commandOptions, "Command options must not be null");
        if (isGatewayMode()) {
            String fromNodeId = commandOptions.getNodeId();
            String appId = commandOptions.getAppId();
            String sessionId = commandOptions.getSessionId();
            List<String> messages = new ArrayList<>();
            collectNewMessages(appId, messages, commandOptions);
            for (String message : messages) {
                publishRelay(fromNodeId, sessionId, message);
            }
        }
    }

    /**
     * Gets the last known messages for the apps subscribed by the session.
     * @param session the client session
     * @return a list of messages
     */
    public List<String> getLastMessages(@NonNull RelaySession session) {
        if (!session.isValid()) {
            return List.of();
        }
        List<String> messages = new ArrayList<>();
        CommandOptions commandOptions = new CommandOptions();
        commandOptions.setTimeZone(session.getTimeZone());
        String[] subscribedApps = session.getSubscribedApps();
        if (subscribedApps != null && subscribedApps.length > 0) {
            for (String appId : subscribedApps) {
                commandOptions.setAppId(appId);
                collectLastMessages(messages, commandOptions);
            }
        } else {
            collectLastMessages(messages, commandOptions);
        }
        return messages;
    }

    /**
     * Retrieves the last known messages based on the provided command options.
     * This method collects messages relevant to the specified criteria
     * and returns them as a list of strings.
     * @param commandOptions the command options specifying the criteria
     *                       for retrieving the last messages
     * @return a list of strings containing the last known messages
     */
    public List<String> getLastMessages(@NonNull CommandOptions commandOptions) {
        List<String> messages = new ArrayList<>();
        collectLastMessages(messages, commandOptions);
        return messages;
    }

    private void collectLastMessages(List<String> messages, @NonNull CommandOptions commandOptions) {
        String appId = commandOptions.getAppId();
        for (ExporterManager exporterManager : exporterManagers) {
            if (appId == null || appId.equals(exporterManager.getAppId())) {
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
            String[] appIds = (session != null ? session.getSubscribedApps() : null);
            if (appIds != null) {
                for (String id : appIds) {
                    if (appId == null || appId.equals(id)) {
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
            if (appId == null || appId.equals(exporterManager.getAppId())) {
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

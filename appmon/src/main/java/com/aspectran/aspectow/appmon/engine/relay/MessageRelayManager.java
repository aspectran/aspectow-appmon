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

import com.aspectran.aspectow.appmon.engine.config.AppInfoHolder;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
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
import java.util.concurrent.CopyOnWriteArraySet;

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

    private final String nodeId;

    private final AppInfoHolder appInfoHolder;

    private final RedisMessagePublisher messagePublisher;

    private final Map<String, Set<String>> joinedAppsByNode = new ConcurrentHashMap<>();

    /**
     * Instantiates a new MessageRelayManager.
     * @param nodeId the unique identifier of the current node
     * @param appInfoHolder the holder for app information
     * @param messagePublisher the Redis message publisher
     */
    public MessageRelayManager(String nodeId, AppInfoHolder appInfoHolder, RedisMessagePublisher messagePublisher) {
        this.nodeId = nodeId;
        this.appInfoHolder = appInfoHolder;
        this.messagePublisher = messagePublisher;
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

    /**
     * Publishes a local message to Redis and relays it to all registered relayers.
     * @param message the message to publish
     */
    public void publish(String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishRelay(CATEGORY_APPMON, message);
            } catch (Exception e) {
                logger.error("Failed to publish relay message to Redis", e);
            }
        }
        relay(message);
    }

    /**
     * Publishes a management control message for this node.
     * @param message the control message to publish
     */
    public void publishControl(String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishControl(message);
            } catch (Exception e) {
                logger.error("Failed to publish control message to Redis", e);
            }
        }
    }

    /**
     * Relays a message to all registered relayers.
     * This method does not publish the message to Redis.
     * @param message the message to relay
     */
    public void relay(String message) {
        for (MessageRelayer relayer : messageRelayers) {
            relayer.relay(message);
        }
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
    public synchronized boolean join(@NonNull RelaySession session) {
        if (session.isValid()) {
            String[] appIds = session.getJoinedApps();
            if (appIds != null && appIds.length > 0) {
                for (String id : appIds) {
                    startExporters(id);
                    publishControl("appmon:join:" + id);
                }
            } else {
                startExporters(null);
                publishControl("appmon:join");
            }
            return true;
        } else {
            return false;
        }
    }

    private void startExporters(String appId) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (appId == null || exporterManager.getAppId().equals(appId)) {
                exporterManager.start();
            }
        }
    }

    /**
     * Handles a client releasing its monitoring session.
     * Stops exporters that are no longer being monitored by any client.
     * @param session the client session that is being released
     */
    public synchronized void release(@NonNull RelaySession session) {
        String[] appIds = session.getJoinedApps();
        if (appIds != null && appIds.length > 0) {
            for (String id : appIds) {
                if (messagePublisher != null) {
                    if (!isUsingAppLocally(id)) {
                        publishControl("appmon:release:" + id);
                    }
                } else {
                    if (!isUsingAppLocally(id)) {
                        stopExporters(id);
                    }
                }
            }
        } else {
            if (messagePublisher != null) {
                if (!isUsingAppLocally(null)) {
                    publishControl("appmon:release");
                }
            } else {
                if (!isUsingAppLocally(null)) {
                    stopExporters(null);
                }
            }
        }
        session.removeJoinedApps();
    }

    private void stopExporters(String appId) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (appId == null || exporterManager.getAppId().equals(appId)) {
                exporterManager.stop();
            }
        }
    }

    /**
     * Handles control messages from the cluster.
     * @param nodeId the ID of the node that sent the message
     * @param message the control message
     */
    public void handleControlMessage(String nodeId, @NonNull String message) {
        if (message.startsWith("appmon:join")) {
            String appId = (message.length() > 12 ? message.substring(12) : null);
            addNodeJoinedApp(nodeId, appId);
            startExporters(appId);
        } else if (message.startsWith("appmon:release")) {
            String appId = (message.length() > 15 ? message.substring(15) : null);
            removeNodeJoinedApp(nodeId, appId);
            if (!isAppInUse(appId)) {
                stopExporters(appId);
            }
        }
    }

    private void addNodeJoinedApp(String nodeId, String appId) {
        if (appId == null) {
            appId = "";
        }
        joinedAppsByNode.computeIfAbsent(appId, k -> ConcurrentHashMap.newKeySet()).add(nodeId);
    }

    private void removeNodeJoinedApp(String nodeId, String appId) {
        if (appId == null) {
            appId = "";
        }
        Set<String> nodes = joinedAppsByNode.get(appId);
        if (nodes != null) {
            nodes.remove(nodeId);
            if (nodes.isEmpty()) {
                joinedAppsByNode.remove(appId);
            }
        }
    }


    private boolean isAppInUse(String appId) {
        if (isUsingAppLocally(appId)) {
            return true;
        }
        if (appId == null) {
            appId = "";
        }
        Set<String> nodes = joinedAppsByNode.get(appId);
        return (nodes != null && !nodes.isEmpty());
    }

    private boolean isUsingAppLocally(String appId) {
        for (MessageRelayer messageRelayer : messageRelayers) {
            if (messageRelayer.isUsingApp(appId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the last known messages for the apps joined by the session.
     * @param session the client session
     * @return a list of messages
     */
    public List<String> getLastMessages(@NonNull RelaySession session) {
        CommandOptions commandOptions = new CommandOptions();
        commandOptions.setTimeZone(session.getTimeZone());
        List<String> messages = new ArrayList<>();
        if (session.isValid()) {
            String[] appIds = session.getJoinedApps();
            if (appIds != null && appIds.length > 0) {
                for (String id : appIds) {
                    collectLastMessages(id, messages, commandOptions);
                }
            } else {
                collectLastMessages(null, messages, commandOptions);
            }
        }
        return messages;
    }

    private void collectLastMessages(String appId, List<String> messages, CommandOptions commandOptions) {
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
    public List<String> getNewMessages(@NonNull RelaySession session, @NonNull CommandOptions commandOptions) {
        String appId = commandOptions.getApp();
        List<String> messages = new ArrayList<>();
        if (session.isValid()) {
            String[] appIds = session.getJoinedApps();
            if (appIds != null && appIds.length > 0) {
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

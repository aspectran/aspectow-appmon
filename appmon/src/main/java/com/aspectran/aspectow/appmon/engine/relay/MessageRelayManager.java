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
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

    private static final String CONTROL_JOIN = "appmon:join";

    private static final String CONTROL_RELEASE = "appmon:release";

    private final Set<MessageRelayer> messageRelayers = new CopyOnWriteArraySet<>();

    private final List<ExporterManager> exporterManagers = new CopyOnWriteArrayList<>();

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    private final RedisMessagePublisher messagePublisher;

    /**
     * Instantiates a new MessageRelayManager.
     * @param messagePublisher the Redis message publisher
     */
    public MessageRelayManager(RedisMessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    public SubscriptionRegistry getSubscriptionRegistry() {
        return subscriptionRegistry;
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
    public void broadcast(String message) {
//        if (messagePublisher != null) {
//            try {
//                messagePublisher.publishRelay(CATEGORY_APPMON, message);
//            } catch (Exception e) {
//                logger.error("Failed to publish relay message to Redis", e);
//            }
//        }
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
            subscriptionRegistry.addLocalSubscription(session.getId(), appIds);
            if (appIds != null && appIds.length > 0) {
                for (String id : appIds) {
                    startExporters(id);
                    publishControl(CONTROL_JOIN + ":" + id);
                }
            } else {
                startExporters(null);
                publishControl(CONTROL_JOIN);
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
        subscriptionRegistry.removeLocalSubscription(session.getId());
        if (appIds != null && appIds.length > 0) {
            for (String id : appIds) {
                if (messagePublisher != null) {
                    if (!subscriptionRegistry.isAppInUseLocally(id)) {
                        publishControl(CONTROL_RELEASE + ":" + id);
                    }
                } else {
                    if (!subscriptionRegistry.isAppInUseLocally(id)) {
                        stopExporters(id);
                    }
                }
            }
        } else {
            if (messagePublisher != null) {
                if (!subscriptionRegistry.isAppInUseLocally(null)) {
                    publishControl(CONTROL_RELEASE);
                }
            } else {
                if (!subscriptionRegistry.isAppInUseLocally(null)) {
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
        if (message.startsWith(CONTROL_JOIN)) {
            String appId = (message.length() > CONTROL_JOIN.length() + 1 ?
                    message.substring(CONTROL_JOIN.length() + 1) : null);
            subscriptionRegistry.addRemoteSubscription(nodeId, appId);
            startExporters(appId);
        } else if (message.startsWith(CONTROL_RELEASE)) {
            String appId = (message.length() > CONTROL_RELEASE.length() + 1 ?
                    message.substring(CONTROL_RELEASE.length() + 1) : null);
            subscriptionRegistry.removeRemoteSubscription(nodeId, appId);
            if (!subscriptionRegistry.isAppInUse(appId)) {
                stopExporters(appId);
            }
        }
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

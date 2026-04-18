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

import com.aspectran.aspectow.appmon.engine.config.InstanceInfoHolder;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages all {@link MessageRelayer} and {@link ExporterManager} instances.
 * This class is a central hub for handling client sessions (join/release),
 * collecting messages from exporters, and relaying them to clients.
 *
 * <p>Created: 2025-02-12</p>
 */
public class MessageRelayManager {

    private static final Logger logger = LoggerFactory.getLogger(MessageRelayManager.class);

    private final Set<MessageRelayer> messageRelayers = new CopyOnWriteArraySet<>();

    private final List<ExporterManager> exporterManagers = new CopyOnWriteArrayList<>();

    private final InstanceInfoHolder instanceInfoHolder;

    private final RedisMessagePublisher messagePublisher;

    /**
     * Instantiates a new MessageRelayManager.
     * @param instanceInfoHolder the holder for instance information
     */
    public MessageRelayManager(InstanceInfoHolder instanceInfoHolder, RedisMessagePublisher messagePublisher) {
        this.instanceInfoHolder = instanceInfoHolder;
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
     * Relays a local message from an exporter.
     * @param message the message to relay
     */
    public void relay(String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishRelay(message);
            } catch (Exception e) {
                logger.error("Failed to publish relay message to Redis", e);
            }
        }
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
     * Handles a client joining to monitor instances.
     * Starts the necessary exporters for the joined instances.
     * @param session the client session that is joining
     * @return {@code true} if the join was successful, {@code false} otherwise
     */
    public synchronized boolean join(@NonNull RelaySession session) {
        if (session.isValid()) {
            String[] instanceIds = session.getJoinedInstances();
            if (instanceIds != null && instanceIds.length > 0) {
                for (String id : instanceIds) {
                    startExporters(id);
                }
            } else {
                startExporters(null);
            }
            return true;
        } else {
            return false;
        }
    }

    private void startExporters(String instanceName) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (instanceName == null || exporterManager.getInstanceId().equals(instanceName)) {
                exporterManager.start();
            }
        }
    }

    /**
     * Handles a client releasing its monitoring session.
     * Stops exporters that are no longer being monitored by any client.
     * @param session the client session that is being released
     */
    public synchronized void release(RelaySession session) {
        String[] instanceIds = getUnusedInstances(session);
        if (instanceIds != null) {
            for (String id : instanceIds) {
                stopExporters(id);
            }
        }
        session.removeJoinedInstances();
    }

    private void stopExporters(String instanceId) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (instanceId == null || exporterManager.getInstanceId().equals(instanceId)) {
                exporterManager.stop();
            }
        }
    }

    /**
     * Gets the last known messages for the instances joined by the session.
     * @param session the client session
     * @return a list of messages
     */
    public List<String> getLastMessages(@NonNull RelaySession session) {
        CommandOptions commandOptions = new CommandOptions();
        commandOptions.setTimeZone(session.getTimeZone());
        List<String> messages = new ArrayList<>();
        if (session.isValid()) {
            String[] instanceIds = session.getJoinedInstances();
            if (instanceIds != null && instanceIds.length > 0) {
                for (String id : instanceIds) {
                    collectLastMessages(id, messages, commandOptions);
                }
            } else {
                collectLastMessages(null, messages, commandOptions);
            }
        }
        return messages;
    }

    private void collectLastMessages(String instanceId, List<String> messages, CommandOptions commandOptions) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (instanceId == null || exporterManager.getInstanceId().equals(instanceId)) {
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
        String instanceId = commandOptions.getInstance();
        List<String> messages = new ArrayList<>();
        if (session.isValid()) {
            String[] instanceIds = session.getJoinedInstances();
            if (instanceIds != null && instanceIds.length > 0) {
                for (String id : instanceIds) {
                    if (instanceId == null || id.equals(instanceId)) {
                        collectNewMessages(id, messages, commandOptions);
                    }
                }
            } else {
                collectNewMessages(instanceId, messages, commandOptions);
            }
        }
        return messages;
    }

    private void collectNewMessages(String instanceId, List<String> messages, CommandOptions commandOptions) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (instanceId == null || exporterManager.getInstanceId().equals(instanceId)) {
                exporterManager.collectNewMessages(messages, commandOptions);
            }
        }
    }

    private String @Nullable [] getUnusedInstances(RelaySession session) {
        String[] instanceIds = getJoinedInstances(session);
        if (instanceIds == null || instanceIds.length == 0) {
            return null;
        }
        List<String> unusedInstances = new ArrayList<>(instanceIds.length);
        for (String id : instanceIds) {
            boolean using = false;
            for (MessageRelayer messageRelayer : messageRelayers) {
                if (messageRelayer.isUsingInstance(id)) {
                    using = true;
                    break;
                }
            }
            if (!using) {
                unusedInstances.add(id);
            }
        }
        if (!unusedInstances.isEmpty()) {
            return unusedInstances.toArray(new String[0]);
        } else {
            return null;
        }
    }

    private String @Nullable [] getJoinedInstances(@NonNull RelaySession session) {
        String[] instanceIds = session.getJoinedInstances();
        if (instanceIds == null) {
            return null;
        }
        Set<String> validJoinedInstances = new HashSet<>();
        for (String id : instanceIds) {
            if (instanceInfoHolder.containsInstance(id)) {
                validJoinedInstances.add(id);
            }
        }
        if (!validJoinedInstances.isEmpty()) {
            return validJoinedInstances.toArray(new String[0]);
        } else {
            return null;
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

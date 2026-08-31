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

/**
 * HTTP Polling implementation of the AppMon client.
 *
 * @version 4.1
 * @last-modified 2026-08-29
 */
class PollingClient extends BaseClient {
    constructor(node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode = false) {
        super(node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode);
        this.pendingCommands = [];
        this.pollingTimer = null;
        this.stopped = false;

        if (!this.isGatewayMode && this.node.port && (location.hostname === "localhost" || location.hostname === "127.0.0.1")) {
            const url = new URL(this.node.endpoint.path, location.href);
            url.port = this.node.port;
            this.node.endpoint.path = url.origin + url.pathname;
        }
    }

    start(appsToSubscribe, nodeToSubscribe) {
        this.stopped = false;
        this.nodeToSubscribe = nodeToSubscribe;
        this.appsToSubscribe = appsToSubscribe;
        this.connect(this.node.id);
    }

    stop() {
        this.stopped = true;
        this.primary = false;
        this.primaryNodeId = null;
        if (this.pollingTimer) {
            clearTimeout(this.pollingTimer);
            this.pollingTimer = null;
        }
    }

    connect(nodeId) {
        $.ajax({
            url: this.node.endpoint.path + "/appmon/polling/subscribe",
            type: "post",
            dataType: "json",
            data: {
                nodeId: nodeId,
                timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                nodeToSubscribe: this.nodeToSubscribe,
                appsToSubscribe: this.appsToSubscribe
            },
            success: (data) => {
                if (data) {
                    if (data.primary && !data.appsToSubscribe) {
                        console.warn("No verified apps found. Please check the configuration of the backend.");
                        return;
                    }

                    this.everConnected = true;
                    if (data.primary) {
                        this.retryCount = 0;
                        this.node.endpoint['mode'] = "polling";
                        this.node.endpoint['pollingInterval'] = data.pollingInterval;
                    }

                    this.establish(data.nodeId, data.primary, data.alive);

                    if (this.primary && !this.stopped) {
                        this.appsToSubscribe = data.appsToSubscribe;
                        this.poll();
                    }
                } else {
                    console.log(this.node.id, "connection failed");
                    this.printErrorMessage("Connection failed.");
                    this.reconnect();
                }
            },
            error: (xhr, status, error) => {
                console.log(this.node.id, "connection failed", error);
                this.printErrorMessage("Connection failed.");
                this.reconnect();
            }
        });
    }

    poll() {
        if (this.stopped) return;
        let commands = null;
        if (this.pendingCommands.length) {
            commands = this.pendingCommands.slice();
            this.pendingCommands.length = 0;
        }
        $.ajax({
            url: this.node.endpoint.path + "/appmon/polling/pull",
            type: "post",
            cache: false,
            data: commands ? {
                "commands[]": commands
            } : null,
            success: (data) => {
                if (this.stopped) return;
                if (data && data.messages) {
                    this.processMessages(data.messages);
                    const interval = this.node.endpoint.pollingInterval || 3000;
                    this.pollingTimer = setTimeout(() => {
                        this.poll();
                    }, interval);
                } else {
                    console.log(this.node.id, "connection lost");
                    this.printErrorMessage("Connection lost.");
                    this.notifyClosed();
                    this.reconnect();
                }
            },
            error: (xhr, status, error) => {
                if (this.stopped) return;
                if (commands && commands.length) {
                    this.pendingCommands.unshift(...commands);
                }
                console.log(this.node.id, "connection lost", error);
                this.printErrorMessage("Connection lost.");
                this.notifyClosed();
                this.reconnect();
            }
        });
    }

    changePollingInterval(speed) {
        $.ajax({
            url: this.node.endpoint.path + "/appmon/polling/interval",
            type: "post",
            dataType: "json",
            data: { speed: speed },
            success: (data) => {
                if (data && data.pollingInterval) {
                    this.node.endpoint.pollingInterval = data.pollingInterval;
                    console.log(this.node.id, "pollingInterval", data.pollingInterval);
                    this.viewer.printMessage("Polling every " + data.pollingInterval + " milliseconds.");
                    if (this.pollingTimer) {
                        clearTimeout(this.pollingTimer);
                        this.pollingTimer = setTimeout(() => this.poll(), data.pollingInterval);
                    }
                } else {
                    console.log(this.node.id, "failed to change polling interval");
                    this.viewer.printMessage("Failed to change polling interval.");
                }
            },
            error: (xhr, status, error) => {
                console.log(this.node.id, "failed to change polling interval", error);
                this.viewer.printMessage("Failed to change polling interval.");
            }
        });
    }

    processMessages(messages) {
        if (messages) {
            messages.forEach(msg => {
                const idx = msg.indexOf(':');
                if (idx === -1) return;

                const nodeId = msg.substring(0, idx);
                const message = msg.substring(idx + 1);

                if (this.primary) {
                    if (this.isGatewayMode) {
                        if (message.startsWith(":subscribed:")) {
                            const alive = (message === ":subscribed:alive");
                            this.establish(nodeId, false, alive);
                            return;
                        }
                        if (message.startsWith(":node:joined:")) {
                            try {
                                const nodeInfo = JSON.parse(message.substring(13));
                                if (this.onNodeJoined) this.onNodeJoined(nodeInfo);
                            } catch (e) {
                                console.error("Failed to parse node:joined message:", message, e);
                            }
                            return;
                        }
                        if (message.startsWith(":node:statusChanged:")) {
                            try {
                                const nodeInfo = JSON.parse(message.substring(20));
                                if (this.onNodeStatusChanged) this.onNodeStatusChanged(nodeInfo);
                            } catch (e) {
                                console.error("Failed to parse node:statusChanged message:", message, e);
                            }
                            return;
                        }
                        if (message === ":node:left") {
                            if (this.onNodeLeft) this.onNodeLeft(nodeId);
                            return;
                        }
                    }

                    // Data messages
                    const viewer = this.getViewer(nodeId);
                    if (viewer) {
                        viewer.processMessage(message);
                    } else {
                        console.warn("No viewer registered for nodeId:", nodeId, "Message:", message);
                    }
                } else {
                    console.error("Unexpected message received before primary connection established:", message);
                }
            });
        }
    }

    establish(nodeId, primary, alive) {
        if (this.reconnecting && (!primary || !alive)) {
            console.log("Reconnect attempt failed, node is not primary or alive");
            if (this.onRequireRebuild) {
                this.onRequireRebuild();
            }
            return;
        }

        if (primary) {
            // If an unknown node becomes primary in Gateway mode
            // (e.g., topology change or gateway node restart with a new ID),
            // request a full dashboard rebuild to refresh cluster node configurations.
            if (this.isGatewayMode && !this.getNodeConfig(nodeId)) {
                this.stop();
                if (this.onRequireRebuild) {
                    console.log(nodeId, "unknown primary node detected, requesting full rebuild");
                    this.onRequireRebuild();
                }
                return;
            }
            this.primary = true;
            this.primaryNodeId = nodeId;
        }

        const config = this.getNodeConfig(nodeId);
        if (config) {
            config.node.alive = !!alive;
            if (config.onSubscribed && !config.node.subscribed) {
                config.onSubscribed(config.node, primary);
            }
        }

        const viewer = this.getViewer(nodeId);
        if (!alive) {
            viewer.printErrorMessage("Node " + nodeId + " not alive");
        } else {
            viewer.printMessage("Polling every " + this.node.endpoint.pollingInterval + " milliseconds.");
        }
        if (primary) {
            if (this.isGatewayMode && this.reconnecting) {
                for (let id in this.clusterNodes) {
                    if (id !== nodeId) {
                        this.connect(id);
                    }
                }
            }
            this.reconnecting = false;
            this.sendCommand(["command:established"], nodeId);
        }
    }

    sendCommand(options, nodeId) {
        if (options) {
            let arr = options.slice();
            arr.push("nodeId:" + (nodeId || this.primaryNodeId));
            const cmd = arr.join(";");
            console.log("send", cmd);
            if (!this.pendingCommands.includes(cmd)) {
                this.pendingCommands.push(cmd);
            }
        }
    }
}

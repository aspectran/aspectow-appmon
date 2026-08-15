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
 * WebSocket implementation of the AppMon client.
 * In Gateway Mode, it manages a single physical connection for the entire cluster.
 *
 * @version 4.0
 * @last-modified 2026-08-15
 */
class WebsocketClient extends BaseClient {
    constructor(node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode) {
        super(node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode);
        this.heartbeatInterval = 50000;
        this.heartbeatTimer = null;
        this.socket = null;
        this.handshakeSuccessful = false;
    }

    start(appsToSubscribe, nodeToSubscribe) {
        this.nodeToSubscribe = nodeToSubscribe;
        this.appsToSubscribe = appsToSubscribe;
        this.openSocket();
    }

    stop() {
        this.closeSocket();
        this.handshakeSuccessful = false;
    }

    openSocket() {
        this.closeSocket(false);
        const url = new URL(this.node.endpoint.path + "/appmon/websocket/" + this.node.endpoint.token, location.href);
        if (!this.isGatewayMode && this.node.port && (location.hostname === "localhost" || location.hostname === "127.0.0.1")) {
            url.port = this.node.port;
        }
        url.protocol = url.protocol.replace("https:", "wss:").replace("http:", "ws:");

        console.log("connecting to websocket:", url.href);
        this.socket = new WebSocket(url.href);

        this.socket.onopen = () => {
            this.handshakeSuccessful = true;
            this.everConnected = true;
            console.log(this.node.id, "websocket connected");

            // Connect to the current node
            this.connect(this.node.id);
            this.sendPing();
            this.retryCount = 0;
        };

        this.socket.onmessage = (event) => {
            if (typeof event.data !== "string") return;
            const msg = event.data;
            const idx = msg.indexOf(':');
            if (idx === -1) {
                console.warn("Invalid message format received:", msg);
                return;
            }

            const nodeId = msg.substring(0, idx);
            const message = msg.substring(idx + 1);

            if (this.primary) {
                // Standard control messages
                if (message.startsWith(":pong:")) {
                    this.node.endpoint.token = message.substring(6);
                    this.sendPing();
                    return;
                }

                if (this.isGatewayMode) {
                    if (message.startsWith(":subscribed:")) {
                        const alive = (message === ":subscribed:alive");
                        this.establish(nodeId, false, alive);
                        return;
                    }
                    if (message.startsWith(":node:joined:")) {
                        const nodeInfo = JSON.parse(message.substring(13));
                        if (this.onNodeJoined) this.onNodeJoined(nodeInfo);
                        return;
                    }
                    if (message.startsWith(":node:statusChanged:")) {
                        const nodeInfo = JSON.parse(message.substring(20));
                        if (this.onNodeStatusChanged) this.onNodeStatusChanged(nodeInfo);
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
            } else if (message.startsWith(":subscribed:")) {
                const primary = message.startsWith(":subscribed:primary:");
                const alive = message.endsWith(":alive");
                this.establish(nodeId, primary, alive);
            } else {
                console.error("Unexpected message received before primary connection established:", message);
            }
        };

        this.socket.onclose = (event) => {
            const wasHandshakeSuccessful = this.handshakeSuccessful;
            this.closeSocket(true);

            if (!wasHandshakeSuccessful && !this.everConnected) {
                console.warn("WebSocket handshake failed. Code:", event.code);
                return;
            }

            this.notifyClosed();
            if (event.code === 1003) {
                console.warn("Websocket connection refused: ", event.code);
                this.printErrorMessage("Socket connection refused by server.");
                if (this.onRequireRebuild) {
                    setTimeout(() => this.onRequireRebuild(), 1000);
                }
                return;
            }
            if (event.code === 1011) {
                console.log("Websocket connection closed: ", event.code);
                this.printErrorMessage("Websocket connection closed due to server error.");
                return;
            }
            if (event.code === 1000 || this.retryCount === 0) {
                console.log("Websocket connection closed: ", event.code);
                this.printMessage("Websocket connection closed.");
            }
            if (event.code !== 1000) {
                setTimeout(() => this.reconnect(), 1000);
            }
        };

        this.socket.onerror = (event) => {
            console.error(this.node.id, "websocket error:", event);
            if (!this.everConnected && this.node.endpoint.mode !== "polling") {
                this.node.endpoint.mode = "polling";
                this.printErrorMessage("WebSocket is not supported. Switching to polling mode.");
                this.notifyFailed();
            } else {
                this.printErrorMessage("Could not connect to the WebSocket server.");
            }
        };
    }

    closeSocket(afterClosing) {
        this.primary = false;
        this.primaryNodeId = null;
        if (this.socket) {
            if (!afterClosing) {
                this.socket.close();
            }
            this.socket = null;
        }
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }

    connect(nodeId) {
        const options = ["command:subscribe"];
        options.push("timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone);
        if (this.nodeToSubscribe) {
            options.push("nodeToSubscribe:" + this.nodeToSubscribe);
        }
        if (this.appsToSubscribe) {
            options.push("appsToSubscribe:" + this.appsToSubscribe);
        }
        this.sendCommand(options, nodeId);
    }

    establish(nodeId, primary, alive) {
        if (this.reconnecting && (!primary || !alive)) {
            console.log("Reconnect attempt failed, node is not primary or alive");
            if (this.onRequireRebuild) this.onRequireRebuild();
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
        if (primary) {
            if (this.isGatewayMode && this.reconnecting) {
                for (let id in this.clusterNodes) {
                    if (id !== nodeId) {
                        this.connect(id);
                    }
                }
            }
            this.reconnecting = false;
            const options = ["command:established"];
            if (this.nodeToSubscribe) options.push("nodeToSubscribe:" + this.nodeToSubscribe);
            if (this.appsToSubscribe) options.push("appsToSubscribe:" + this.appsToSubscribe);
            this.sendCommand(options, nodeId);
        }
        if (!alive) {
            viewer.printErrorMessage("Node " + nodeId + " not alive");
        }
    }

    sendCommand(options, nodeId) {
        if (options && this.socket && this.socket.readyState === WebSocket.OPEN) {
            const arr = options.slice();
            arr.push("nodeId:" + (nodeId || this.primaryNodeId));
            const cmd = arr.join(";");
            console.log("send", cmd);
            this.socket.send(cmd);
        }
    }

    sendPing() {
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
        }
        this.heartbeatTimer = setTimeout(() => {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                this.socket.send("command:ping");
            }
        }, this.heartbeatInterval);
    }
}

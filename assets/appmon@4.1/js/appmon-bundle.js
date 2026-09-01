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
 * The base class for AppMon communication clients.
 * Provides common functionality for connection management and retries.
 *
 * @version 4.1
 * @last-modified 2026-08-29
 */
class BaseClient {
    constructor(node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode) {
        this.node = node;
        this.viewer = viewer;
        this.clusterViewers = {};
        this.clusterNodes = {};
        this.onSubscribed = onSubscribed;
        this.onClosed = onClosed;
        this.onFailed = onFailed;
        this.onNodeJoined = null;
        this.onNodeStatusChanged = null;
        this.onNodeLeft = null;
        this.onRequireRebuild = null;
        this.isGatewayMode = isGatewayMode;
        this.nodeToSubscribe = null;
        this.appsToSubscribe = null;
        this.primary = false;
        this.primaryNodeId = node.id
        this.retryCount = 0;
        this.reconnecting = false;
        this.maxRetries = 10;
        this.retryInterval = 5000;
        this.everConnected = false;
    }

    addClusterViewer(nodeId, viewer) {
        this.clusterViewers[nodeId] = viewer;
    }

    addClusterNode(node, onSubscribed) {
        this.clusterNodes[node.id] = {node, onSubscribed};
    }

    getViewer(nodeId) {
        if (this.isGatewayMode && this.node.id !== nodeId) {
            return this.clusterViewers[nodeId];
        }
        return this.viewer;
    }

    getNodeConfig (nodeId) {
        return this.isGatewayMode ? this.clusterNodes[nodeId] : this;
    }

    notifyClosed() {
        if (this.isGatewayMode) {
            for (let id in this.clusterNodes) {
                const config = this.clusterNodes[id];
                if (this.onClosed) this.onClosed(config.node);
            }
        } else {
            if (this.onClosed) this.onClosed(this.node);
        }
    }

    notifyFailed() {
        if (this.isGatewayMode) {
            for (let id in this.clusterNodes) {
                const config = this.clusterNodes[id];
                if (this.onFailed) this.onFailed(config.node);
            }
        } else {
            if (this.onFailed) this.onFailed(this.node);
        }
    }

    printMessage(message) {
        if (this.isGatewayMode) {
            for (let id in this.clusterViewers) {
                const viewer = this.clusterViewers[id];
                if (viewer) {
                    viewer.printMessage(message);
                }
            }
        } else {
            this.viewer.printMessage(message);
        }
    }

    printErrorMessage(message) {
        if (this.isGatewayMode) {
            for (let id in this.clusterViewers) {
                const viewer = this.clusterViewers[id];
                if (viewer) {
                    viewer.printErrorMessage(message);
                }
            }
        } else {
            this.viewer.printErrorMessage(message);
        }
    }

    /**
     * Starts the client connection.
     * @param {string} [appsToSubscribe] - Names of apps to subscribe.
     * @param {string} [nodeToSubscribe] - Node ID to subscribe.
     */
    start(appsToSubscribe, nodeToSubscribe) {
        throw new Error("Method 'start()' must be implemented.");
    }

    /**
     * Stops the client connection.
     */
    stop() {
        // Default implementation does nothing
    }

    /**
     * Refreshes the monitoring data with the specified options.
     * @param {string[]} [options] - Refresh options.
     * @param {string} [nodeId] - Target node ID.
     */
    refresh(options, nodeId) {
        let cmdOptions = ["command:refresh"];
        if (options) cmdOptions.push(...options);
        this.sendCommand(cmdOptions, nodeId);
    }

    focus(appId, nodeId) {
        this.sendCommand([
            "command:focus",
            "appId:" + appId
        ], nodeId);
    }

    loadPrevious(appId, logId, loadedLines, nodeId) {
        this.sendCommand([
            "command:loadPrevious",
            "appId:" + appId,
            "logId:" + logId,
            "loadedLines:" + loadedLines
        ], nodeId);
    }

    /**
     * Sends a command with the specified options.
     * @param {string[]} [options] - Command options.
     * @param {string} [nodeId] - Target node ID.
     */
    sendCommand(options, nodeId) {
        throw new Error("Method 'sendCommand()' must be implemented.");
    }

    /**
     * Handles reconnection logic when a connection is lost or fails.
     */
    reconnect() {
        if (this.retryCount++ < this.maxRetries) {
            this.reconnecting = true;
            const nodeIndex = (this.node && typeof this.node.index === 'number') ? this.node.index : 0;
            const jitter = Math.floor(Math.random() * 1000);
            const retryInterval = (this.retryInterval * this.retryCount) + (nodeIndex * 200) + jitter;
            const status = "(" + this.retryCount + "/" + this.maxRetries + ", interval=" + retryInterval + "ms)";
            console.log(this.node.id, "trying to reconnect", status);
            this.printMessage("Trying to reconnect... " + status);
            setTimeout(() => {
                this.start(this.appsToSubscribe, this.nodeToSubscribe);
            }, retryInterval);
        } else {
            console.log(this.node.id, "max connection attempts exceeded");
            this.printMessage("Max connection attempts exceeded.");
            this.notifyFailed();
            if (this.everConnected) {
                console.log(this.node.id, "will keep retrying connection in background...");
                setTimeout(() => {
                    this.retryCount = Math.max(0, this.maxRetries - 2);
                    this.start(this.appsToSubscribe, this.nodeToSubscribe);
                }, this.retryInterval * 2);
            }
        }
    }
}

/**
 * WebSocket implementation of the AppMon client.
 * In Gateway Mode, it manages a single physical connection for the entire cluster.
 *
 * @version 4.1
 * @last-modified 2026-08-29
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

/**
 * Advanced Canvas-based particle engine for AppMon traffic visualization.
 * Handles tab visibility to prevent "bullet bursts" when returning to the tab.
 *
 * @version 4.1
 * @last-modified 2026-08-29
 */
class TrafficPainter {
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.bullets = [];
        this.animationId = null;
        this.isRunning = false;
        this.finishLineOffset = 180; // track-stack width

        this.resize();
        this.resizeObserver = new ResizeObserver(() => this.resize());
        this.resizeObserver.observe(this.canvas.parentElement);
    }

    resize() {
        const rect = this.canvas.parentElement.getBoundingClientRect();
        this.canvas.width = rect.width;
        this.canvas.height = rect.height;
    }

    /**
     * Adds a new bullet to the painter.
     * @param {Object} data - Bullet data (error, elapsedTime, activityCount).
     * @param {Function} onArriving - Callback when bullet reaches the finish line.
     */
    addBullet(data, onArriving) {
        const elapsedTime = data.elapsedTime || 0;
        const activityCount = data.activityCount || 0;
        const hasError = !!(data.error);
        const timeIntensity = Math.min(elapsedTime / 5000, 1);
        const targetMax = 1000;
        const activityIntensity = activityCount > 0 
            ? Math.min(Math.log10(activityCount + 1) / Math.log10(targetMax + 1), 1)
            : 0;
        
        const size = 3.0 + (timeIntensity * 4) + (activityIntensity * 4);
        const baseSpeed = (this.canvas.width - this.finishLineOffset) / (900 / 16.6);
        const speed = baseSpeed * (1 - (timeIntensity * 0.6));

        const bullet = {
            x: -(Math.random() * 150),
            y: Math.random() * (this.canvas.height - 20) + 10,
            speed: speed,
            size: size,
            timeIntensity: timeIntensity,
            activityIntensity: activityIntensity,
            color: hasError ? '#ff0000' : (timeIntensity > 0.5 ? '#f1c40f' : '#11d539'),
            elapsedTime: Math.max(elapsedTime, 500),
            arrived: false,
            arrivedTime: 0,
            impactPulse: 0,
            alpha: 1.0,
            onArriving: onArriving
        };
        this.bullets.push(bullet);

        if (!this.isRunning) {
            this.start();
        }
    }

    start() {
        this.isRunning = true;
        this.lastTime = performance.now();
        const loop = (currentTime) => {
            if (document.hidden) {
                this.isRunning = false;
                return;
            }
            
            const deltaTime = Math.min((currentTime - this.lastTime) / 16.66, 3.0);
            this.lastTime = currentTime;

            this.update(deltaTime);
            this.draw();

            if (this.bullets.length > 0) {
                this.animationId = requestAnimationFrame(loop);
            } else {
                this.isRunning = false;
                this.clear();
            }
        };
        this.animationId = requestAnimationFrame(loop);
    }

    update(deltaTime) {
        const finishLine = this.canvas.width - this.finishLineOffset;
        const now = Date.now();

        for (let i = this.bullets.length - 1; i >= 0; i--) {
            const b = this.bullets[i];

            if (!b.arrived) {
                b.x += b.speed * deltaTime;
                if (b.x >= finishLine) {
                    b.x = finishLine;
                    b.arrived = true;
                    b.arrivedTime = now;
                    b.impactPulse = 1.0;
                }
            } else {
                if (b.impactPulse > 0) {
                    b.impactPulse -= 0.05 * deltaTime;
                }

                const stayElapsed = now - b.arrivedTime;
                if (stayElapsed > b.elapsedTime + 200) {
                    b.alpha -= 0.04 * deltaTime;
                    if (b.alpha <= 0) {
                        if (b.onArriving) b.onArriving();
                        this.bullets.splice(i, 1);
                    }
                }
            }
        }
    }

    draw() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        for (let i = 0; i < this.bullets.length; i++) {
            const b = this.bullets[i];
            this.ctx.globalAlpha = b.alpha;
            
            const drawSize = b.arrived ? b.size * (1 + b.impactPulse * 0.3) : b.size;

            // 1. Aura (Glow) - high performance alternative to shadowBlur
            this.ctx.fillStyle = b.color;
            this.ctx.beginPath();
            let auraSize = drawSize * (1.2 + b.timeIntensity + (b.arrived ? b.impactPulse : 0));
            this.ctx.globalAlpha = b.alpha * 0.3;
            this.ctx.arc(b.x, b.y, auraSize, 0, Math.PI * 2);
            this.ctx.fill();

            // 2. Main Bullet Body
            this.ctx.globalAlpha = b.alpha;
            this.ctx.beginPath();
            this.ctx.arc(b.x, b.y, drawSize, 0, Math.PI * 2);
            this.ctx.fill();

            // 3. Activity Glow (Hot Core) - high performance alternative to radial gradient
            if (b.activityIntensity > 0.3) {
                const coreSize = drawSize * (0.3 + b.activityIntensity * 0.4);
                this.ctx.fillStyle = '#fff';
                this.ctx.beginPath();
                this.ctx.arc(b.x, b.y, coreSize, 0, Math.PI * 2);
                this.ctx.fill();
            }
        }
        this.ctx.globalAlpha = 1.0;
    }

    clear() {
        this.bullets = [];
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    }

    destroy() {
        if (this.animationId) cancelAnimationFrame(this.animationId);
        this.resizeObserver.disconnect();
    }
}

/**
 * The chart component for the AppMon dashboard.
 * Responsible for rendering and updating individual charts using Chart.js.
 *
 * @version 4.1
 * @last-modified 2026-08-29
 */
class DashboardChart {
    constructor($container, eventId) {
        this.$container = $container;
        this.eventId = eventId;
        this.chart = null;
        this.dateUnit = null;
        this.dateOffset = null;
    }

    isDrawn() {
        return (this.chart !== null);
    }

    getLabels() {
        return (this.chart ? this.chart.data.labels : []);
    }

    getDataset(index) {
        return (this.chart ? this.chart.data.datasets[index].data : []);
    }

    ensureCanvas() {
        let $canvas = this.$container.find("canvas");
        if (!$canvas.length) {
            $canvas = $("<canvas/>").appendTo(this.$container);
        }
        return $canvas;
    }

    draw(dateUnit, labels, data1, data2) {
        this.destroy();
        const $canvas = this.ensureCanvas();

        let dataLabel1;
        let borderColor1;
        let backgroundColor1;
        switch (this.eventId) {
            case "activity":
                dataLabel1 = "Activities";
                borderColor1 = "#4493c8";
                backgroundColor1 = "#cce0fa";
                break;
            case "session":
                dataLabel1 = "Sessions";
                borderColor1 = "#44c577";
                backgroundColor1 = "#bcefd0";
                break;
            default:
                dataLabel1 = "";
        }

        const chartType = (!dateUnit ? "line" : "bar");
        this.chart = new Chart($canvas[0], {
            type: chartType,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        enabled: true,
                        reverse: true,
                        mode: 'x',
                        intersect: false,
                        callbacks: {
                            title: (tooltip) => {
                                const datetime = dayjs(labels[tooltip[0].dataIndex]);
                                switch (dateUnit) {
                                    case "hour": return datetime.format("LL HH:00");
                                    case "day": return datetime.format("LL");
                                    case "month": return datetime.date(1).format("LL");
                                    case "year": return datetime.format("YYYY");
                                    default: return datetime.format("LLL");
                                }
                            }
                        }
                    },
                    zoom: {
                        zoom: {
                            wheel: { enabled: false },
                            pinch: { enabled: true },
                            drag: {
                                enabled: true,
                                threshold: 21,
                                backgroundColor: "rgba(225,225,225,0.35)",
                                borderColor: "rgba(225,225,225)",
                                borderWidth: 1
                            },
                            mode: "x",
                            onZoomComplete: () => {
                                const $resetZoom = this.$container.find(".reset-zoom");
                                if (this.isZoomedOrPanned()) {
                                    $resetZoom.off("click").on("click", () => this.resetZoom()).show();
                                } else {
                                    $resetZoom.hide();
                                }
                            }
                        },
                        pan: { enabled: true, mode: "x", modifierKey: "ctrl" }
                    }
                },
                scales: {
                    x: {
                        display: true,
                        ticks: {
                            autoSkip: false,
                            includeBounds: false,
                            callback: (value, index) => {
                                const datetime = dayjs(labels[value]);
                                const datetime2 = (value > 0 ? dayjs(labels[value - 1]) : null);
                                switch (dateUnit) {
                                    case "hour":
                                        return (index === 0 || (datetime2 && !datetime.isSame(datetime2, "day")))
                                            ? datetime.format("M/D HH:00")
                                            : datetime.format("HH:00");
                                    case "day":
                                        return (index === 0 || (datetime2 && !datetime.isSame(datetime2, "year")))
                                            ? datetime.format("YYYY M/D")
                                            : datetime.format("M/D");
                                    case "month":
                                        return datetime.format("YYYY/M");
                                    case "year":
                                        return datetime.format("YYYY");
                                    default: // 5m.
                                        return (index === 0 || (datetime2 && !datetime.isSame(datetime2, "day")))
                                            ? datetime.format("M/D HH:mm")
                                            : datetime.format("HH:mm");
                                }
                            }
                        },
                        stacked: true,
                        grid: chartType === "line" ? {
                            color: (ctx) => (data2[ctx.tick.value] > 0 ? "#ff6384" : "#e4e4e4")
                        } : {}
                    },
                    y: {
                        display: true,
                        title: { display: true, text: dataLabel1 },
                        suggestedMin: 0,
                        suggestedMax: 5,
                        stacked: true,
                        grid: { color: "#e4e4e4" }
                    }
                }
            },
            data: {
                labels: labels,
                datasets: [
                    chartType === "line" ? {
                        label: dataLabel1,
                        data: data1,
                        fill: true,
                        borderColor: borderColor1,
                        backgroundColor: backgroundColor1,
                        borderWidth: 1.4,
                        tension: 0.1,
                        pointStyle: false,
                        order: 2
                    } : {
                        label: dataLabel1,
                        data: data1,
                        minBarLength: 2,
                        fill: true,
                        borderWidth: 1,
                        borderColor: borderColor1,
                        backgroundColor: borderColor1,
                        order: 2
                    },
                    {
                        label: "Errors",
                        data: data2,
                        type: chartType,
                        fill: true,
                        borderWidth: 1,
                        borderColor: "#ff6384",
                        backgroundColor: "#ff6384",
                        showLine: false,
                        pointStyle: false,
                        order: 1
                    }
                ]
            }
        });

        this.dateUnit = dateUnit;
        const $resetZoom = this.$container.find(".reset-zoom");
        if (this.isZoomedOrPanned()) {
            $resetZoom.show();
        } else {
            $resetZoom.hide();
        }
    }

    setData(labels, data1, data2) {
        if (this.chart) {
            this.chart.data.labels = labels;
            this.chart.data.datasets[0].data = data1;
            this.chart.data.datasets[1].data = data2;
            this.update();
        }
    }

    rollup(labels, data1, data2) {
        if (this.chart) {
            const chartLabels = this.chart.data.labels;
            const chartData1 = this.chart.data.datasets[0].data;
            const chartData2 = this.chart.data.datasets[1].data;
            if (chartLabels.length > 0) {
                const lastIndex = chartLabels.length - 1;
                if (chartLabels[lastIndex] >= labels[0]) {
                    chartLabels.splice(lastIndex, 1);
                    chartData1.splice(lastIndex, 1);
                    chartData2.splice(lastIndex, 1);
                }
            }
            chartLabels.push(...labels);
            chartData1.push(...data1);
            chartData2.push(...data2);
        }
    }

    isZoomedOrPanned() {
        return (this.chart && typeof this.chart.isZoomedOrPanned === "function" && this.chart.isZoomedOrPanned());
    }

    resetZoom() {
        if (this.chart && typeof this.chart.resetZoom === "function") {
            this.chart.resetZoom();
        }
    }

    update() {
        if (this.chart) {
            this.chart.update();
        }
    }

    destroy() {
        if (this.chart) {
            this.chart.destroy();
            this.chart = null;
        }
    }
}

/**
 * The viewer component for the AppMon dashboard.
 * Responsible for rendering monitoring data, including logs, metrics, and charts.
 *
 * @version 4.1
 * @last-modified 2026-08-29
 */
class DashboardViewer {
    constructor(sampleInterval, options = {}) {
        this.flagsUrl = options.flagsUrl || "https://cdn.jsdelivr.net/gh/aspectran/aspectran-assets@main/assets/countries/flags/";
        this.tempResidentInactiveSecs = 30;
        this.sampleInterval = sampleInterval;

        this.client = null;
        this.enable = false;
        this.visible = false;
        this.displays = {};
        this.metrics = {};
        this.charts = {};
        this.consoles = {};
        this.indicators = {};
        this.currentActivityCounts = {};
        this.cachedCanvasWidth = 0;
        this.activeBulletCount = 0;
        this.maxBullets = 500;
        this.painters = {};
    }

    setClient(client) {
        this.client = client;
    }

    setEnable(flag) {
        this.enable = !!flag;
        if (this.enable) {
            this.resetAllInterimTimers();
        }
    }

    setVisible(flag) {
        this.visible = !!flag;
        if (!this.visible) {
            this.clearBullets();
        }
    }

    putDisplay$(appId, eventId, $display) {
        const key = appId + ":event:" + eventId;
        this.displays[key] = $display;
        if ($display.hasClass("track-box")) {
            const canvas = $display.find(".traffic-canvas")[0];
            if (canvas) {
                this.painters[key] = new TrafficPainter(canvas);
            }
        }
    }

    putMetric$(appId, metricId, $metric) {
        this.metrics[appId + ":metric:" + metricId] = $metric;
    }

    putChart$(appId, eventId, $chart) {
        const key = appId + ":data:" + eventId;
        this.charts[key] = new DashboardChart($chart, eventId);
    }

    putConsole$(appId, logId, $console) {
        this.consoles[appId + ":log:" + logId] = $console;
    }

    putIndicator$(appId, exporterType, exporterName, $indicator) {
        this.indicators[appId + ":" + exporterType + ":" + exporterName] = $indicator;
    }

    getDisplay$(key) {
        return this.displays[key] || null;
    }

    getMetric$(key) {
        return this.metrics[key] || null;
    }

    getChart$(key) {
        return this.charts[key] || null;
    }

    getConsole$(key) {
        return this.consoles[key] || null;
    }

    getIndicator$(key) {
        return this.indicators[key] || null;
    }

    updateCanvasWidth() {
        this.cachedCanvasWidth = 0;
    }

    resetCurrentActivityCounts() {
        this.currentActivityCounts = {};
        for (let key in this.indicators) {
            if (key.includes(":event:activity")) {
                this.printCurrentActivityCount(key, 0);
            }
        }
        this.clearBullets();
    }

    clearAllSessions() {
        for (let key in this.displays) {
            if (key.includes(":event:session")) {
                const $sessions = this.displays[key].find("ul.sessions");
                $sessions.find("li").each(function () {
                    const timer = $(this).data("timer");
                    if (timer) clearTimeout(timer);
                });
                $sessions.empty();
            }
        }
    }

    setLoading(appId, isLoading) {
        for (let key in this.charts) {
            if (key.startsWith(appId + ":")) {
                const dashboardChart = this.charts[key];
                const $chartBox = dashboardChart.$container.closest(".chart-box");
                const $overlay = $chartBox.find(".loading-overlay");
                if (isLoading) {
                    $overlay.css("display", "flex");
                } else {
                    $overlay.hide();
                }
            }
        }
    }

    refreshConsole($console) {
        if ($console) {
            this.scrollToBottom($console);
        } else {
            for (let key in this.consoles) {
                if (!this.consoles[key].data("pause")) {
                    this.scrollToBottom(this.consoles[key]);
                }
            }
        }
    }

    clearConsole($console) {
        if ($console) {
            $console.empty();
        }
    }

    scrollToBottom($console) {
        if (!$console) return;
        let timer = $console.data("timer");
        if (timer) {
            clearTimeout(timer);
        }
        timer = setTimeout(() => {
            const el = $console[0];
            if (!el) return;

            // Process Buffered Messages
            const buffer = $console.data("log-buffer");
            if (buffer && buffer.length > 0) {
                const fragment = document.createDocumentFragment();
                while (buffer.length > 0) {
                    const item = buffer.shift();
                    const p = document.createElement("p");
                    if (typeof item === "string") {
                        p.textContent = item;
                    } else {
                        if (item.html) p.innerHTML = item.html;
                        else p.textContent = item.text;
                        if (item.className) p.className = item.className;
                    }
                    fragment.appendChild(p);
                }
                el.appendChild(fragment);
            }

            // Scroll to bottom if tailing
            if ($console.data("tailing")) {
                el.scrollTop = el.scrollHeight;
            }

            // Truncate old messages
            const pList = el.getElementsByTagName("p");
            if (pList.length > 11000) {
                const removeCount = pList.length - 10000;
                for (let i = 0; i < removeCount; i++) {
                    el.removeChild(pList[0]);
                }
            }
        }, 300);
        $console.data("timer", timer);
    }

    prependToConsole($console, noAnchoring) {
        if (!$console) return;
        let timer = $console.data("prev-timer");
        if (timer) {
            clearTimeout(timer);
        }
        timer = setTimeout(() => {
            const el = $console[0];
            if (!el) return;

            const buffer = $console.data("log-prev-buffer");
            if (buffer && buffer.length > 0) {
                const oldScrollHeight = el.scrollHeight;
                const oldScrollTop = el.scrollTop;

                const fragment = document.createDocumentFragment();
                while (buffer.length > 0) {
                    const item = buffer.shift();
                    const p = document.createElement("p");
                    if (typeof item === "string") {
                        p.textContent = item;
                    } else {
                        if (item.html) p.innerHTML = item.html;
                        else p.textContent = item.text;
                        if (item.className) p.className = item.className;
                    }
                    fragment.appendChild(p);
                }
                el.prepend(fragment);

                // Maintain scroll position (anchoring)
                if (noAnchoring) {
                    el.scrollTop = 0;
                } else {
                    el.scrollTop = oldScrollTop + (el.scrollHeight - oldScrollHeight);
                }
            }
        }, 100);
        $console.data("prev-timer", timer);
    }

    printMessage(message, consoleName) {
        if (consoleName) {
            const $console = this.getConsole$(consoleName);
            if ($console) {
                let buffer = $console.data("log-buffer");
                if (!buffer) {
                    buffer = [];
                    $console.data("log-buffer", buffer);
                }
                buffer.push({ html: message, className: "event ellipses" });
                this.scrollToBottom($console);
            }
        } else {
            for (let key in this.consoles) {
                this.printMessage(message, key);
            }
        }
    }

    printErrorMessage(message, consoleName) {
        if (consoleName || !Object.keys(this.consoles).length) {
            const $console = this.getConsole$(consoleName);
            if ($console) {
                let buffer = $console.data("log-buffer");
                if (!buffer) {
                    buffer = [];
                    $console.data("log-buffer", buffer);
                }
                buffer.push({ html: message, className: "event error" });
                this.scrollToBottom($console);
            }
        } else {
            for (let key in this.consoles) {
                this.printErrorMessage(message, key);
            }
        }
    }

    processMessage(message) {
        const idx1 = message.indexOf(":");
        const idx2 = (idx1 !== -1 ? message.indexOf(":", idx1 + 1) : -1);
        const idx3 = (idx2 !== -1 ? message.indexOf(":", idx2 + 1) : -1);
        if (idx3 === -1) {
            return;
        }

        const appId = message.substring(0, idx1);
        let exporterType = message.substring(idx1 + 1, idx2);
        const exporterName = message.substring(idx2 + 1, idx3);

        let subType = "";
        if (exporterType.includes("/")) {
            const parts = exporterType.split("/");
            exporterType = parts[0];
            subType = parts[1];
        }

        const exporterKey = appId + ":" + exporterType + ":" + exporterName;
        const messageContent = message.substring(idx3 + 1);

        switch (exporterType) {
            case "event":
                if (messageContent.length) {
                    const eventData = JSON.parse(messageContent);
                    this.processEventData(appId, exporterType, exporterName, exporterKey, eventData);
                }
                break;
            case "data":
                if (messageContent.length) {
                    if (subType === "chart") {
                        const chartData = JSON.parse(messageContent);
                        this.processChartData(appId, exporterType, exporterName, exporterKey, chartData);
                    }
                }
                break;
            case "metric":
                if (messageContent.length) {
                    const metricData = JSON.parse(messageContent);
                    this.processMetricData(appId, exporterType, exporterName, exporterKey, metricData);
                }
                break;
            case "log":
                this.printLogMessage(appId, exporterType, exporterName, exporterKey, messageContent, subType);
                break;
        }
    }

    printLogMessage(appId, exporterType, logId, exporterKey, messageContent, subType) {
        this.indicate(appId, exporterType, logId);
        const $console = this.getConsole$(exporterKey);
        if ($console) {
            if (subType === "p") {
                if (messageContent) {
                    let prevBuffer = $console.data("log-prev-buffer");
                    if (!prevBuffer) {
                        prevBuffer = [];
                        $console.data("log-prev-buffer", prevBuffer);
                    }
                    prevBuffer.push(messageContent);
                    this.prependToConsole($console);
                } else {
                    let prevBuffer = $console.data("log-prev-buffer");
                    if (!prevBuffer) {
                        prevBuffer = [];
                        $console.data("log-prev-buffer", prevBuffer);
                    }
                    prevBuffer.push({ html: "No more logs to load.", className: "event ellipses" });
                    this.prependToConsole($console, true);
                    $console.closest(".console-box").find(".load-previous").hide();
                }
            } else if (!$console.data("pause")) {
                let buffer = $console.data("log-buffer");
                if (!buffer) {
                    buffer = [];
                    $console.data("log-buffer", buffer);
                }
                buffer.push(messageContent);
                this.scrollToBottom($console);
            }
        }
    }

    processEventData(appId, exporterType, eventId, exporterKey, eventData) {
        switch (eventId) {
            case "activity":
                this.indicate(appId, exporterType, eventId);
                if (eventData.activities) {
                    this.printActivityStatus(exporterKey, eventData.activities);
                }
                if (this.visible) {
                    const $track = this.getDisplay$(exporterKey);
                    if ($track) {
                        const varName = exporterKey.replace(/:/g, '_');
                        if (!this.currentActivityCounts[varName]) {
                            this.currentActivityCounts[varName] = 0;
                            this.printCurrentActivityCount(exporterKey, 0);
                        }
                        this.launchBullet($track, eventData, () => {
                            this.currentActivityCounts[varName]++;
                            this.printCurrentActivityCount(exporterKey, this.currentActivityCounts[varName]);
                        }, () => {
                            if (this.currentActivityCounts[varName] > 0) {
                                this.currentActivityCounts[varName]--;
                            }
                            this.printCurrentActivityCount(exporterKey, this.currentActivityCounts[varName]);
                        });
                    }
                } else {
                    this.printCurrentActivityCount(exporterKey, 0);
                }
                this.updateActivityCount(
                    appId + ":" + exporterType + ":session",
                    eventData.sessionId,
                    eventData.activityCount || 0);
                break;
            case "session":
                this.printSessionEventData(exporterKey, eventData);
                break;
        }
    }

    processMetricData(appId, exporterType, metricId, exporterKey, metricData) {
        const $metric = this.getMetric$(exporterKey);
        if ($metric) {
            let formatted = metricData.format;
            for (let key in metricData.data) {
                formatted = formatted.replace("{" + key + "}", metricData.data[key]);
            }
            $metric.find("dd")
                .text(formatted)
                .attr("title", JSON.stringify(metricData.data, null, 2));
        }
    }

    launchBullet($track, eventData, onLeaving, onArriving) {
        if (eventData.elapsedTime === undefined || eventData.elapsedTime === null) return;

        // Skip visualization and counting if tab is hidden
        if (document.hidden) return;

        if (onLeaving) onLeaving();

        // Find the painter associated with this track-box
        let painter = null;
        for (let key in this.displays) {
            if (this.displays[key][0] === $track[0]) {
                painter = this.painters[key];
                break;
            }
        }

        if (painter) {
            if (this.activeBulletCount < this.maxBullets) {
                this.activeBulletCount++;
                painter.addBullet(eventData, () => {
                    this.activeBulletCount--;
                    if (onArriving) onArriving();
                });
            } else {
                // Still update counts via timer even if capped
                setTimeout(() => {
                    if (onArriving) onArriving();
                }, eventData.elapsedTime + 900);
            }
        }
    }

    clearBullets() {
        for (let key in this.painters) {
            this.painters[key].clear();
        }
        this.activeBulletCount = 0;
    }

    indicate(appId, exporterType, exporterName) {
        this.blink(this.getIndicator$("group:event:"));
        this.blink(this.getIndicator$("node:event:"));
        if (this.visible) {
            this.blink(this.getIndicator$("app:event:" + appId));
            if (exporterType === "log") {
                this.blink(this.getIndicator$(appId + ":log:" + exporterName));
            }
        }
    }

    blink($indicator) {
        if ($indicator && !$indicator.hasClass("on")) {
            $indicator.addClass("blink on");
            setTimeout(() => {
                $indicator.removeClass("blink on");
            }, 500);
        }
    }

    printActivityStatus(exporterKey, activities) {
        const $activityStatus = this.getIndicator$(exporterKey);
        if ($activityStatus) {
            const separator = (activities.errors > 0 ? " / " : (activities.interim > 0 ? "+" : "-"));
            $activityStatus.find(".interim .separator").text(separator);
            $activityStatus.find(".interim .total").text(activities.interim > 0 ? activities.interim : "");
            $activityStatus.find(".interim .errors").text(activities.errors > 0 ? activities.errors : "");
            $activityStatus.find(".cumulative .total").text(activities.total);
        }
    }

    resetInterimActivityStatus(exporterKey) {
        const $activityStatus = this.getIndicator$(exporterKey);
        if ($activityStatus) {
            $activityStatus.find(".interim .separator").text("");
            $activityStatus.find(".interim .total").text(0);
            $activityStatus.find(".interim .errors").text("");
        }
    }

    resetInterimTimer(exporterKey) {
        if (this.sampleInterval) {
            const $activityStatus = this.getIndicator$(exporterKey);
            if ($activityStatus) {
                const $samplingTimerBar = $activityStatus.find(".sampling-timer-bar");
                const $samplingTimerStatus = $activityStatus.find(".sampling-timer-status");
                if ($samplingTimerBar.length) {
                    let timer = $samplingTimerBar.data("timer");
                    if (timer) {
                        clearInterval(timer);
                        $samplingTimerBar.removeData("timer");
                    }
                    let second = (dayjs().minute() * 60 + dayjs().second()) % this.sampleInterval;
                    $samplingTimerBar.animate({ height: 0 }, 600);
                    $samplingTimerBar.animate({ height: (second++ / this.sampleInterval * 100).toFixed(2) + "%" }, 400);
                    $samplingTimerStatus.text(second + "/" + this.sampleInterval);
                    timer = setInterval(() => {
                        if (!this.enable) {
                            clearInterval(timer);
                            $samplingTimerBar.removeData("timer");
                            return;
                        }
                        const percent = second++ / this.sampleInterval * 100;
                        $samplingTimerBar.css("height", percent.toFixed(2) + "%");
                        $samplingTimerStatus.text(second + "/" + this.sampleInterval);
                        if (second > 300) second = 0;
                        else if (second % 10 === 0) {
                            second = (dayjs().minute() * 60 + dayjs().second()) % this.sampleInterval;
                        }
                    }, 1000);
                    $samplingTimerBar.data("timer", timer);
                }
            }
        }
    }

    resetAllInterimTimers() {
        for (let key in this.indicators) {
            const $activityStatus = this.getIndicator$(key);
            if ($activityStatus.hasClass("activity-status")) {
                this.resetInterimTimer(key);
            }
        }
    }

    printCurrentActivityCount(exporterKey, count) {
        const $activityStatus = this.getIndicator$(exporterKey);
        if ($activityStatus) {
            $activityStatus.find(".current .total").text(count);
        }
    }

    printSessionEventData(exporterKey, eventData) {
        const $display = this.getDisplay$(exporterKey);
        if ($display) {
            $display.find(".numberOfCreated").text(eventData.numberOfCreated);
            $display.find(".numberOfExpired").text(eventData.numberOfExpired);
            $display.find(".numberOfActives").text(eventData.numberOfActives);
            $display.find(".highestNumberOfActives").text(eventData.highestNumberOfActives);
            $display.find(".numberOfUnmanaged").text(eventData.numberOfUnmanaged);
            $display.find(".numberOfRejected").text(eventData.numberOfRejected);
            if (eventData.startTime) {
                $display.find(".startTime").text(dayjs.utc(eventData.startTime).local().format("LLL"));
            }
            const $sessions = $display.find("ul.sessions");

            if (eventData.fullSync && eventData.createdSessions) {
                const newSids = eventData.createdSessions.map(s => {
                    const session = (typeof s === "string" ? JSON.parse(s) : s);
                    return session.sessionId;
                });
                $sessions.find("li").each(function () {
                    const sid = $(this).data("sid");
                    if (sid && !newSids.includes(sid)) {
                        const timer = $(this).data("timer");
                        if (timer) clearTimeout(timer);
                        $(this).remove();
                    }
                });
            }

            if (eventData.createdSessions) {
                eventData.createdSessions.forEach(session => this.addSession($sessions, typeof session === "string" ? JSON.parse(session) : session));
            }
            if (eventData.destroyedSessions) {
                eventData.destroyedSessions.forEach(sessionId => $sessions.find("li[data-sid='" + sessionId + "']").remove());
            }
            if (eventData.evictedSessions) {
                eventData.evictedSessions.forEach(sessionId => {
                    const $item = $sessions.find("li[data-sid='" + sessionId + "']");
                    if (!$item.hasClass("inactive")) {
                        $item.addClass("inactive");
                        const inactiveInterval = Math.min($item.data("inactive-interval") || this.tempResidentInactiveSecs, this.tempResidentInactiveSecs);
                        setTimeout(() => $item.remove(), inactiveInterval * 1000);
                    }
                });
            }
            if (eventData.residedSessions) {
                eventData.residedSessions.forEach(session => this.addSession($sessions, typeof session === "string" ? JSON.parse(session) : session));
            }
        }
    }

    addSession($sessions, session) {
        $sessions.find("li[data-sid='" + session.sessionId + "']").each(function () {
            const timer = $(this).data("timer");
            if (timer) clearTimeout(timer);
        }).remove();

        const $count = $("<div class='count'></div>").text(session.activityCount || 0);
        if (session.activityCount > 1 || !session.countryCode) $count.addClass("counting");
        if (session.username) $count.addClass("active");

        const $li = $("<li/>")
            .attr("data-sid", session.sessionId)
            .attr("data-inactive-interval", session.inactiveInterval)
            .append($count);

        if (session.tempResident) {
            $li.addClass("inactive");
            const inactiveInterval = Math.min(session.inactiveInterval || 30, 30);
            const timer = setTimeout(() => $li.remove(), inactiveInterval * 1000);
            $li.data("timer", timer);
        }

        if (session.countryCode) {
            const code = session.countryCode.toLowerCase();
            const countryInfo = (typeof countries !== "undefined" && countries[session.countryCode])
                ? countries[session.countryCode]
                : null;
            $("<img class='flag' alt=''/>")
                .attr("src", this.flagsUrl + code + ".png")
                .attr("alt", session.countryCode)
                .attr("title", countryInfo ? countryInfo.name : session.countryCode)
                .appendTo($li);
        }
        if (session.username) {
            $("<div class='username'/>").text(session.username).appendTo($li);
        }

        const $detail = $("<div class='detail'/>")
            .append($("<p/>").text(session.sessionId))
            .append($("<p/>").text(dayjs.utc(session.createAt).local().format("LLL")));
        if (session.ipAddress) $detail.append($("<p/>").text(session.ipAddress));
        $detail.appendTo($li);

        if (session.tempResident) $li.appendTo($sessions);
        else $li.prependTo($sessions);
    }

    updateActivityCount(exporterKey, sessionId, activityCount) {
        const $display = this.getDisplay$(exporterKey);
        if ($display) {
            const $li = $display.find("ul.sessions li[data-sid='" + sessionId + "']");
            const $count = $li.find(".count").text(activityCount);
            if (activityCount > 1) $count.addClass("counting");
            $li.show();
        }
    }

    processChartData(appId, exporterType, eventId, exporterKey, chartData) {
        const dashboardChart = this.getChart$(exporterKey);
        if (!dashboardChart) return;
        this.setLoading(appId, false);

        if (eventId === "activity") {
            const prefix = appId + ":event:" + eventId;
            if (!dashboardChart.isDrawn()) this.resetInterimTimer(prefix);
            else if (chartData.rolledUp) {
                this.resetInterimTimer(prefix);
                this.resetInterimActivityStatus(prefix);
            }
        }
        const dateUnit = (chartData.rolledUp ? dashboardChart.dateUnit : chartData.dateUnit);
        const dateOffset = (chartData.rolledUp ? dashboardChart.dateOffset : chartData.dateOffset);
        const labels = chartData.labels;
        const data1 = chartData.data1;
        const data2 = chartData.data2.map(n => (eventId === "activity" ? n : null));

        if (!dashboardChart.isDrawn() || !chartData.rolledUp) {
            dashboardChart.ensureCanvas();
            this.pruneDataPoints(labels, data1, data2, dashboardChart.$container);
            dashboardChart.draw(dateUnit, labels, data1, data2);
            dashboardChart.dateOffset = dateOffset;
        } else if (!dateOffset) {
            if (!dateUnit) {
                dashboardChart.rollup(labels, data1, data2);
                this.pruneDataPoints(dashboardChart.getLabels(), dashboardChart.getDataset(0), dashboardChart.getDataset(1), dashboardChart.$container);
                dashboardChart.update();
            } else if (this.client) {
                setTimeout(() => {
                    const options = [
                        "appId:" + appId,
                        "dateUnit:" + dateUnit,
                        "timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone
                    ];
                    this.client.refresh(options);
                }, 900);
            }
        }
    }

    pruneDataPoints(labels, data1, data2, $container) {
        if (this.cachedCanvasWidth === 0) {
            let w = 0;
            if ($container) {
                w = $container.find("canvas").width();
                if (w === 0) w = $container.width();
            }
            if (w === 0) {
                for (let key in this.charts) {
                    const dashboardChart = this.charts[key];
                    if (dashboardChart) {
                        w = dashboardChart.$container.find("canvas").width();
                        if (w === 0) w = dashboardChart.$container.width();
                        if (w > 0) break;
                    }
                }
            }
            if (w > 0) {
                this.cachedCanvasWidth = w - 90;
            }
        }
        const maxLabels = (this.cachedCanvasWidth > 0 ? Math.floor(this.cachedCanvasWidth / 21) : 0);
        if (maxLabels > 0) {
            const cnt = labels.length - maxLabels;
            if (cnt > 0) {
                labels.splice(0, cnt);
                data1.splice(0, cnt);
                data2.splice(0, cnt);
            }
        }
        return maxLabels;
    }

    getMaxStartDatetime(appId) {
        let result = "";
        for (let key in this.charts) {
            if (key.startsWith(appId + ":")) {
                const dashboardChart = this.charts[key];
                if (dashboardChart && dashboardChart.isDrawn()) {
                    const labels = dashboardChart.getLabels();
                    if (labels.length && labels[0] > result) {
                        result = labels[0];
                    }
                }
            }
        }
        return result;
    }
}

/**
 * The builder component for the AppMon dashboard.
 * Responsible for assembling the dashboard UI based on configuration data.
 *
 * @version 4.1
 * @last-modified 2026-08-29
 */
class DashboardBuilder {
    constructor(options = {}) {
        this.options = options;
        this.settings = {};
        this.clusterMode = "direct";
        this.isGatewayMode = false;
        this.counterPersistInterval = 5;
        this.groups = [];
        this.nodes = [];
        this.apps = [];
        this.metrics = [];
        this.viewers = [];
        this.clients = [];
        this.currentGroupId = null;
        this.selectedNodeIdByGroup = {};
    }

    build(baseUrl, appsToSubscribe, nodeToSubscribe) {
        this.baseUrl = baseUrl;
        this.appsToSubscribe = appsToSubscribe;
        this.nodeToSubscribe = nodeToSubscribe;
        this.currentGroupId = null;
        this.selectedNodeIdByGroup = {};
        this.suspendMonitoring();
        this.clearView();
        $.ajax({
            url: baseUrl + "/appmon/config/data",
            type: "get",
            dataType: "json",
            data: {
                nodeToSubscribe: nodeToSubscribe || null,
                appsToSubscribe: appsToSubscribe || null
            },
            success: (data) => {
                if (data) {
                    if (!data.appsToSubscribe) {
                        alert("No verified apps found. Please check the configuration of the backend.");
                        return;
                    }

                    this.settings = { ...data.settings };
                    this.clusterMode = this.settings.clusterMode || "direct";
                    this.isGatewayMode = (this.settings.clusterMode === "gateway");
                    this.counterPersistInterval = this.settings.counterPersistInterval || 5;
                    this.groups = [];
                    this.nodes = [];
                    this.apps = [];
                    this.viewers = [];
                    this.clients = [];

                    let index = 0;
                    data.nodes.forEach(nodeInfo => {
                        if (this.nodeToSubscribe && this.nodeToSubscribe !== nodeInfo.id) {
                            return;
                        }
                        const node = {
                            ...nodeInfo,
                            index: index++,
                            active: true,
                            alive: false,
                            primary: false,
                            subscribed: false,
                            subscribeAttempts: 0
                        };
                        node.endpoint.mode = node.endpoint.mode || "auto";
                        node.endpoint.path = baseUrl + node.endpoint.path + "/" + node.id;
                        node.endpoint.token = data.token;
                        this.nodes.push(node);
                        this.viewers[node.index] = new DashboardViewer(this.counterPersistInterval * 60, this.options);
                        console.log(index, "node", node);
                    });

                    // Assign group-specific logical numbers to each node
                    const groupNodeCounts = {};
                    this.nodes.forEach(node => {
                        const groupId = node.group;
                        if (!groupNodeCounts[groupId]) {
                            groupNodeCounts[groupId] = 0;
                        }
                        groupNodeCounts[groupId]++;
                        node.nodeNoInGroup = groupNodeCounts[groupId];
                    });

                    if (data.groups) {
                        data.groups.forEach(groupInfo => {
                            if (this.nodes.some(node => node.group === groupInfo.id)) {
                                const group = { ...groupInfo, active: false };
                                if (data.myGroupId && groupInfo.id === data.myGroupId) {
                                    this.groups.unshift(group);
                                } else {
                                    this.groups.push(group);
                                }
                            }
                        });
                    }

                    data.apps.forEach(appInfo => {
                        const app = { ...appInfo, active: false };
                        this.apps.push(app);
                        console.log("app", app);
                    });

                    this.buildView();
                    this.bindEvents();
                    if (this.nodes.length) {
                        this.connect(0);
                    }

                    // Select the initial group
                    if (this.groups.length > 0) {
                        let initialGroupId = null;
                        if (this.nodeToSubscribe) {
                            const targetNode = data.nodes.find(n => n.id === this.nodeToSubscribe);
                            if (targetNode && targetNode.group) {
                                initialGroupId = targetNode.group;
                            }
                        }
                        if (!initialGroupId) {
                            initialGroupId = this.groups[0].id;
                        }
                        this.changeGroup(initialGroupId);
                    }

                    if (location.hash) {
                        const appId = location.hash.substring(1);
                        const targetApp = this.apps.find(app => app.id === appId);
                        if (targetApp) {
                            if (targetApp.group && targetApp.group !== this.currentGroupId) {
                                this.changeGroup(targetApp.group);
                            }
                            this.changeApp(appId);
                        }
                    }
                }
            },
            error: (xhr) => {
                if (xhr.status === 403) {
                    alert("Authentication has expired. You will be redirected to the main page.");
                    location.href = baseUrl;
                }
            }
        });
    }

    rebuild() {
        this.build(this.baseUrl, this.appsToSubscribe, this.nodeToSubscribe);
    }

    connect(nodeIndex) {
        const onSubscribed = (node, primary) => {
            if (node.subscribed && node.subscribeAttempts > 0) return;
            if (primary) {
                console.log("primary connection node:", node.id);
                node.primary = true;
            }
            node.subscribed = true;
            node.subscribeAttempts++;
            console.log(node.id, "subscribe attempts:", node.subscribeAttempts);
            //this.clearConsole(node.index);
            this.changeNodeState(node);
            if (node.subscribeAttempts === 1) {
                this.initView();
            } else {
                this.clearSessions(node.index);
            }
            if (node.alive) this.viewers[node.index].setEnable(true);
            if (node.alive && node.active) this.viewers[node.index].setVisible(true);
            if (node.subscribeAttempts === 1 && node.index + 1 < this.nodes.length) {
                console.log("connecting next node:", node.index + 1);
                this.connect(node.index + 1);
            }
        };

        const onClosed = (node) => {
            node.subscribed = false;
            node.alive = false;
            node.primary = false;
            this.changeNodeState(node);
            this.viewers[node.index].setEnable(false);
        };

        const onFailed = (node) => {
            this.changeNodeState(node, true);
            if (node.endpoint.mode === "polling" && node.subscribeAttempts < 1) {
                const currentClient = this.clients[node.index];
                if (currentClient && currentClient.constructor.name === "PollingClient") {
                    return;
                }
                setTimeout(() => {
                    const currentClientAsync = this.clients[node.index];
                    if (currentClientAsync && currentClientAsync.constructor.name === "PollingClient") {
                        return;
                    }
                    const viewer = this.viewers[node.index];
                    const client = new PollingClient(node, viewer, onSubscribed, onClosed, onFailed, this.isGatewayMode);
                    if (this.isGatewayMode) {
                        this.sharedClient = client;
                        client.addClusterViewer(node.id, viewer);
                        client.addClusterNode(node, onSubscribed);
                        client.onNodeJoined = onNodeJoined;
                        client.onNodeStatusChanged = onNodeStatusChanged;
                        client.onNodeLeft = onNodeLeft;
                        client.onRequireRebuild = onRequireRebuild;
                    }
                    this.viewers[node.index].setClient(client);
                    this.clients[node.index] = client;
                    client.start(this.appsToSubscribe, this.nodeToSubscribe);
                }, (node.index - 1) * 1000);
            }
        };

        const onNodeJoined = (node) => {
            this.groups.forEach(group => {
                if (group.id === node.group) {
                    setTimeout(() => {
                        this.showNewNodeNotification(node.id);
                    }, 3000);
                }
            });
        };

        const onNodeStatusChanged = (node) => {
            const existing = this.nodes.find(n => n.id === node.id);
            if (existing) {
                existing.status = node.status;
                this.changeNodeState(existing);
            }
        };

        const onNodeLeft = (nodeId) => {
            const node = this.nodes.find(n => n.id === nodeId);
            if (node) {
                node.subscribed = false;
                node.alive = false;
                this.changeNodeState(node);
                this.viewers[node.index].setEnable(false);
                if (!node.primary) {
                    this.viewers[node.index].printErrorMessage("Node " + nodeId + " is left");
                }
            }
        };

        const onRequireRebuild = () => {
            this.rebuild();
        };

        const node = this.nodes[nodeIndex];
        if (nodeIndex === 0) {
            console.log("cluster mode:", this.clusterMode);
            console.log("endpoint mode:", node.endpoint.mode);
        }
        console.log("connecting node:", nodeIndex);

        if (node.subscribed) return;
        const viewer = this.viewers[nodeIndex];

        if (this.isGatewayMode && this.sharedClient) {
            this.sharedClient.addClusterViewer(node.id, viewer);
            this.sharedClient.addClusterNode(node, onSubscribed);
            viewer.setClient(this.sharedClient);
            this.clients[node.index] = this.sharedClient;
            this.sharedClient.connect(node.id);
            return;
        }

        let client;
        if (node.endpoint.mode === "polling") {
            client = new PollingClient(node, viewer, onSubscribed, onClosed, onFailed, this.isGatewayMode);
        } else {
            client = new WebsocketClient(node, viewer, onSubscribed, onClosed, onFailed, this.isGatewayMode);
        }
        if (this.isGatewayMode) {
            this.sharedClient = client;
            client.addClusterViewer(node.id, viewer);
            client.addClusterNode(node, onSubscribed);
            client.onNodeJoined = onNodeJoined;
            client.onNodeStatusChanged = onNodeStatusChanged;
            client.onNodeLeft = onNodeLeft;
            client.onRequireRebuild = onRequireRebuild;
        }
        viewer.setClient(client);
        this.clients[node.index] = client;
        client.start(this.appsToSubscribe, this.nodeToSubscribe);
    }

    showNewNodeNotification(nodeId) {
        const $notification = $("#new-node-notification");
        if ($notification.length > 0) {
            $notification.find(".node-id").text(nodeId);
            $notification.find(".refresh-btn").off("click").on("click", () => {
                this.rebuild();
            });
            $notification.fadeIn();
        } else {
            const result = confirm("A new node '" + nodeId + "' has joined the cluster. Would you like to refresh the dashboard?");
            if (result) {
                this.rebuild();
            }
        }
    }

    changeNode(nodeIndex) {
        const availableTabs = $(".node.tabs .tabs-title.available");
        if (availableTabs.length <= 1) return;

        const node = this.nodes[nodeIndex];
        const wasActive = node.active;

        // Reset all nodes in the current group
        this.nodes.forEach(n => {
            if (n.group === this.currentGroupId) {
                n.active = false;
            }
        });

        // Toggle or exclusively activate
        if (!wasActive) {
            node.active = true;
            this.selectedNodeIdByGroup[this.currentGroupId] = node.id;
        } else {
            delete this.selectedNodeIdByGroup[this.currentGroupId];
        }

        this.nodes.forEach(n => {
            if (n.group === this.currentGroupId) {
                this.showNode(n);
            }
        });
        this.updateNodeTabs();

        if (this.isGatewayMode) {
            const activeApp = this.apps.find(a => a.active);
            if (activeApp) {
                const targetNodeId = (node.active ? node.id : null);
                this.nodes.forEach(n => {
                    if (n.primary) {
                        const client = this.clients[n.index];
                        if (client && client.focus) client.focus(activeApp.id, targetNodeId);
                    }
                });
            }
        }
    }

    showNode(node) {
        this.apps.forEach(app => {
            if (app.active) {
                this.updateNodeVisibility(node, app.id);
            }
        });
    }

    updateNodeTabs() {
        const availableTabs = $(`.node.tabs .tabs-title[data-group-id=${this.currentGroupId}]`);
        availableTabs.removeClass("active");
        this.nodes.filter(d => d.active && d.group === this.currentGroupId).forEach(d => {
            $(".node.tabs .tabs-title[data-node-index=" + d.index + "]").addClass("active");
        })
    }

    updateNodeVisibility(node, appId) {
        const activeNodesInGroup = this.nodes.filter(n => n.group === this.currentGroupId && n.active);
        const isVisible = (node.group === this.currentGroupId && (activeNodesInGroup.length === 0 || node.active));
        const action = isVisible ? "show" : "hide";

        const selector = `[data-node-index=${node.index}][data-app-id=${appId}]`;
        const otherSelector = `[data-node-index=${node.index}][data-app-id!=${appId}]`;

        $(`.event-box${otherSelector}, .visual-box${otherSelector}, .console-box${otherSelector}`).hide();
        $(`.event-box${selector}, .visual-box${selector}, .console-box${selector}`)[action]();

        this.viewers[node.index].setVisible(isVisible);
        if (isVisible) {
            $(`.track-box[data-node-index=${node.index}] .bullet`).remove();
            $(`.console-box${selector}`).each((_, el) => {
                const $console = $(el).find(".console");
                if (!$console.data("pause")) {
                    this.viewers[node.index].refreshConsole($console);
                }
            });
            $(`.node.metrics-bar[data-node-index=${node.index}]`).show();
        } else {
            $(`.node.metrics-bar[data-node-index=${node.index}]`).hide();
        }
    }

    changeNodeState(node, errorOccurred) {
        const $indicator = $(`.node.tabs .tabs-title[data-node-index=${node.index}] .indicator`);
        $indicator.removeClass($indicator.data("icon-connected") + " connected " +
                           $indicator.data("icon-disconnected") + " disconnected " +
                           $indicator.data("icon-error") + " error");
        if (errorOccurred) {
            $indicator.addClass($indicator.data("icon-error") + " error");
        } else if (node.subscribed && node.alive) {
            $indicator.addClass($indicator.data("icon-connected") + " connected");
        } else {
            $indicator.addClass($indicator.data("icon-disconnected") + " disconnected");
        }
    }

    changeGroup(groupId) {
        if (this.currentGroupId === groupId) return;
        this.currentGroupId = groupId;

        this.groups.forEach(group => {
            const $tabTitle = $(".group.tabs .tabs-title[data-group-id=" + group.id + "]");
            if (group.id === groupId) {
                group.active = true;
                $tabTitle.addClass("active");
            } else {
                group.active = false;
                $tabTitle.removeClass("active");
            }
        });

        // Filter Node Tabs
        let nodeCount = 0;
        const selectedNodeId = this.selectedNodeIdByGroup[groupId];
        this.nodes.forEach(node => {
            const $tab = $(".node.tabs .tabs-title[data-node-index=" + node.index + "]");
            if (!groupId || node.group === groupId) $tab.show(); else $tab.hide();
            if (selectedNodeId) {
                node.active = (node.id === selectedNodeId);
            } else {
                node.active = false; // Start with no nodes explicitly active
            }
            if (node.group === groupId) nodeCount++;
        });

        if (!selectedNodeId && nodeCount === 1) {
            this.nodes.forEach(node => {
                if (node.group === groupId) node.active = true;
            });
        }

        // Filter App Tabs
        this.apps.forEach(app => {
            const $tab = $(".app.tabs .tabs-title[data-app-id=" + app.id + "]");
            if (!groupId || !app.group || app.group === groupId) $tab.show(); else $tab.hide();
        });

        // Select first available app in the new group context
        const firstAvailableApp = this.apps.find(app => {
            return !groupId || !app.group || app.group === groupId;
        });

        this.changeApp(firstAvailableApp ? firstAvailableApp.id : null);
        this.updateNodeTabs();
    }

    changeApp(appId) {
        let exists = false;
        this.apps.forEach(app => {
            if (!appId) appId = app.id;
            const $tabTitle = $(".app.tabs .tabs-title[data-app-id=" + app.id + "]");
            if (app.id === appId) {
                app.active = true;
                setTimeout(() => this.showNodeApp(appId), 0);
                $tabTitle.addClass("active");
                exists = true;
                this.nodes.forEach(node => {
                    if (node.primary) {
                        const client = this.clients[node.index];
                        if (client && client.focus) {
                            setTimeout(() => client.focus(appId, node.id), 10);
                        }
                    }
                });
            } else {
                app.active = false;
                $tabTitle.removeClass("active");
            }
        });
        if (!exists && appId) return this.changeApp();
        return appId;
    }

    showNodeApp(appId) {
        $(".control-bar[data-app-id!=" + appId + "]").hide();
        $(".control-bar[data-app-id=" + appId + "]").show();
        this.nodes.forEach(node => {
            this.updateNodeVisibility(node, appId);
        });
        this.updateNodeTabs();
    }

    initView() {
        if (this.groups.length) $(".group-bar").show();
        $(".speed-options").addClass("hide");
        if (this.nodes.some(d => d.endpoint.mode === "polling")) {
            $(".speed-options").removeClass("hide");
        }
        this.apps.forEach(app => {
            const $eventBox = $(`.event-box[data-app-id=${app.id}]`);
            const $visualBox = $(`.visual-box[data-app-id=${app.id}]`);
            if ($eventBox.length && $visualBox.length && $eventBox.find(".session-box.available").length === 0) {
                $eventBox.removeClass("col-lg-6").addClass("fixed-layout");
                $visualBox.removeClass("col-lg-6").addClass("fixed-layout");
            }
        });
    }

    bindEvents() {
        $(".group.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const groupId = $(e.currentTarget).closest(".tabs-title").data("group-id");
            this.changeGroup(groupId);
        });
        $(".node.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const nodeIndex = $(e.currentTarget).closest(".tabs-title").data("node-index");
            this.changeNode(nodeIndex);
        });
        $(".app.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const appId = $(e.currentTarget).closest(".tabs-title").data("app-id");
            this.changeApp(appId);
        });
        $(".layout-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const appId = $btn.closest(".control-bar").data("app-id");
            const isCompact = $btn.hasClass("compact");
            if (!$btn.hasClass("on")) {
                if (isCompact) {
                    $btn.addClass("on");
                    $(`.event-box.available:not(.fixed-layout)[data-app-id=${appId}], 
                       .visual-box.available:not(.fixed-layout)[data-app-id=${appId}], 
                       .console-box.available[data-app-id=${appId}]`).addClass("col-lg-6");
                }
            } else if (isCompact) {
                $btn.removeClass("on");
                $(`.event-box.available:not(.fixed-layout)[data-app-id=${appId}], 
                   .visual-box.available:not(.fixed-layout)[data-app-id=${appId}], 
                   .console-box.available[data-app-id=${appId}]`).removeClass("col-lg-6");
            }
            this.viewers.forEach(v => v.updateCanvasWidth());
            this.refreshData(appId, false);
        });
        $(".date-unit-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $controlBar = $btn.closest(".control-bar");
            const appId = $controlBar.data("app-id");
            const unit = $btn.data("unit") || "";
            $btn.parent().data("unit", unit).find(".btn").removeClass("on");
            $btn.addClass("on");
            $controlBar.find(".date-offset-options").data("offset", "").find(".btn.current").removeClass("on");
            this.viewers.forEach(v => v.updateCanvasWidth());
            this.refreshData(appId, false);
        });
        $(".date-offset-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $controlBar = $btn.closest(".control-bar");
            const appId = $controlBar.data("app-id");
            const offset = $btn.data("offset") || "";
            const $parent = $btn.parent();
            if (offset !== "current") {
                $parent.find(".btn.current").addClass("on");
            } else {
                $parent.find(".btn").addClass("on");
                $parent.find(".btn.current").removeClass("on");
            }
            $parent.data("offset", offset);
            this.refreshData(appId, false, offset);
        });
        $(".speed-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const faster = !$btn.hasClass("on");
            $btn.toggleClass("on", faster);
            this.nodes.forEach(node => {
                if (node.endpoint.mode === "polling") {
                    this.clients[node.index].changePollingInterval(faster ? 1 : 0);
                }
            });
        });
        $(".open-popup").off("click").on("click", (e) => {
            let url = this.baseUrl + "/appmon/dashboard/popup/" + (this.appsToSubscribe || "");
            if (this.nodeToSubscribe) {
                url += "?nodeId=" + encodeURIComponent(this.nodeToSubscribe);
            }
            const name = "appmon_dashboard_popup";
            const features = "width=1500,height=1070,menubar=no,toolbar=no,location=no,status=no,resizable=yes,scrollbars=yes";
            const popup = window.open(url, name, features);
            if (popup) {
                this.suspendMonitoring();
                this.showPopupModeMessage();
                popup.focus();
            }
        });
        $(document).off("click", ".session-box .panel.status .knob-bar")
            .on("click", ".session-box .panel.status .knob-bar", function() {
                $(this).parent().toggleClass("expanded");
            });
        $(document).off("click", ".session-box ul.sessions li")
            .on("click", ".session-box ul.sessions li", function() {
                $(this).toggleClass("designated");
            });
        $(".console-box .tailing-switch").off("click").on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $consoleBox = $btn.closest(".console-box");
            const $console = $consoleBox.find(".console");
            const nodeIndex = $consoleBox.data("node-index");
            const isTailing = !!$console.data("tailing");
            const newTailingState = !isTailing;

            $console.data("tailing", newTailingState);
            $consoleBox.find(".tailing-status").toggleClass("on", newTailingState);
            $btn.attr("title", newTailingState ? $btn.data("title-on") : $btn.data("title-off"));

            if (newTailingState) {
                this.viewers[nodeIndex].refreshConsole($console);
            }
        });
        $(".console-box .pause-switch").off("click").on("click", function() {
            const $btn = $(this);
            const $icon = $btn.find(".icon");
            const $console = $btn.closest(".console-box").find(".console");
            const isPause = !!$console.data("pause");
            const newPauseState = !isPause;

            $console.data("pause", newPauseState);
            $btn.toggleClass("on", newPauseState);

            if (newPauseState) {
                $btn.attr("title", $btn.data("title-resume"));
                $icon.removeClass($icon.data("icon-pause")).addClass($icon.data("icon-resume"));
            } else {
                $btn.attr("title", $btn.data("title-pause"));
                $icon.removeClass($icon.data("icon-resume")).addClass($icon.data("icon-pause"));
            }
        });
        $(".console-box .expand-switch").off("click").on("click", function() {
            const $btn = $(this);
            const $icon = $btn.find(".icon");
            const $consoleBox = $btn.closest(".console-box");
            const isMaximized = $consoleBox.hasClass("maximized");
            const newMaximizedState = !isMaximized;

            $consoleBox.toggleClass("maximized", newMaximizedState);
            $btn.toggleClass("on", newMaximizedState);

            if (newMaximizedState) {
                $btn.attr("title", $btn.data("title-compress"));
                $icon.removeClass($icon.data("icon-expand")).addClass($icon.data("icon-compress"));
                $("body").css("overflow", "hidden");
            } else {
                $btn.attr("title", $btn.data("title-expand"));
                $icon.removeClass($icon.data("icon-compress")).addClass($icon.data("icon-expand"));
                $("body").css("overflow", "");
            }
        });
        $(".console-box .clear-screen").off("click").on("click", (e) => {
            const $consoleBox = $(e.currentTarget).closest(".console-box");
            this.viewers[$consoleBox.data("node-index")].clearConsole($consoleBox.find(".console"));
        });
        $(".console-box .console").off("scroll").on("scroll", (e) => {
            const $console = $(e.currentTarget);
            const $consoleBox = $console.closest(".console-box");
            if ($console.scrollTop() === 0) {
                $consoleBox.find(".load-previous").fadeIn();
            } else {
                $consoleBox.find(".load-previous").fadeOut();
            }
        });
        $(".console-box .load-previous").off("click").on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $consoleBox = $btn.closest(".console-box");
            const $console = $consoleBox.find(".console");
            const nodeIndex = $consoleBox.data("node-index");
            const appId = $consoleBox.data("app-id");
            const logId = $consoleBox.data("log-id");
            const loadedLines = $console.find("p").length;

            if ($console.data("tailing")) {
                $console.data("tailing", false);
                const $tailingSwitch = $consoleBox.find(".tailing-switch");
                $consoleBox.find(".tailing-status").removeClass("on");
                $tailingSwitch.attr("title", $tailingSwitch.data("title-off"));
            }

            this.clients[nodeIndex].loadPrevious(appId, logId, loadedLines, this.nodes[nodeIndex].id);
        });
        $(window).off("resize").on("resize", () => {
            this.viewers.forEach(v => v.updateCanvasWidth());
        });
        $(document).off("visibilitychange").on("visibilitychange", () => {
            if (!document.hidden) {
                this.viewers.forEach(v => {
                    v.resetCurrentActivityCounts();
                });
                this.apps.forEach(app => {
                    if (this.nodeToSubscribe || !app.hidden) {
                        this.refreshData(app.id, true);
                    }
                });
            }
        });
    }

    refreshData(appId, withLogs, dateOffset) {
        const options = ["appId:" + appId];
        if (withLogs) options.push("withLogs:true");
        const dateUnit = $(".control-bar[data-app-id=" + appId + "] .date-unit-options").data("unit");
        if (dateUnit) options.push("dateUnit:" + dateUnit);
        if (dateOffset === "previous") {
            let maxStartDate = "";
            this.viewers.forEach(v => {
                const startDate = v.getMaxStartDatetime(appId);
                if (startDate > maxStartDate) maxStartDate = startDate;
            });
            if (maxStartDate) {
                options.push("dateOffset:" + maxStartDate);
            } else {
                $(".control-bar[data-app-id=" + appId + "] .date-offset-options .btn.previous").removeClass("on");
                return;
            }
        }
        setTimeout(() => {
            const activeNodesInGroup = this.nodes.filter(n => n.group === this.currentGroupId && n.active);
            this.nodes.forEach(node => {
                console.log("Refreshing node:", node);
                const isVisible = (node.group === this.currentGroupId && (activeNodesInGroup.length === 0 || node.active));
                if (isVisible && node.alive) {
                    this.viewers[node.index].setLoading(appId, true);
                    if (withLogs) this.clearConsole(node.index);
                    this.clients[node.index].refresh(options, node.id);
                }
            });
        }, 50);
    }

    suspendMonitoring() {
        this.clients.forEach(client => {
            if (client) client.stop();
        });
        this.viewers.forEach(viewer => {
            if (viewer) viewer.setEnable(false);
        });
        this.sharedClient = null;
    }

    showPopupModeMessage() {
        this.clearView();
        const $container = $("#content-area > .container-fluid");
        $container.find(".row, .tabs, .control-bar, .console-box").hide();
        const $messageBox = $("#appmon-popup-message");
        if ($messageBox.length > 0) {
            $messageBox.find(".resume-here").off("click").on("click", () => {
                location.reload();
            });
            $messageBox.show();
        }
    }

    clearView() {
        $("#appmon-popup-message").hide();
        $(".group.tabs .tabs-title.available, .node.tabs .tabs-title.available, .app.tabs .tabs-title.available, " +
          ".node.metrics-bar.available, .app.metrics-bar.available, .control-bar.available, " +
          ".event-box.available, .visual-box.available, .chart-box.available, .console-box.available").remove();
        $(".group.tabs .tabs-title, .node.tabs .tabs-title, .app.tabs .tabs-title, .app.metrics-bar, .console-box").show();
    }

    clearConsole(nodeIndex) {
        $(`.console-box[data-node-index=${nodeIndex}] .console`).empty();
    }

    clearSessions(nodeIndex) {
        $(`.session-box[data-node-index=${nodeIndex}] .sessions`).empty();
    }

    buildView() {
        if (this.groups.length > 0) {
            $(".group-bar").show();
            this.groups.forEach(group => {
                const $groupTab = this.addGroupTab(group);
                const $groupIndicator = $groupTab.find(".indicator");
                this.nodes.forEach(node => {
                    if (node.group === group.id) {
                        this.viewers[node.index].putIndicator$("group", "event", "", $groupIndicator);
                    }
                })
            });
        } else {
            $(".group-bar").hide();
        }
        this.nodes.forEach(node => {
            const $nodeTab = this.addNodeTab(node);
            this.viewers[node.index].putIndicator$("node", "event", "", $nodeTab.find(".indicator"));
            this.addNodeMetricsBar(node);
        });
        this.apps.forEach(app => {
            const $appTab = this.addAppTab(app);
            const $appIndicator = $appTab.find(".indicator");
            this.addControlBar(app);
            this.nodes.forEach(node => {
                if (!app.group || app.group === node.group) {
                    const viewer = this.viewers[node.index];
                    viewer.putIndicator$("app", "event", app.id, $appIndicator);
                    if (app.events && app.events.length) {
                        const $eventBox = this.addEventBox(node, app);
                        app.events.forEach(event => {
                            if (event.id === "activity") {
                                const $trackBox = this.addTrackBox($eventBox, node, app, event);
                                viewer.putDisplay$(app.id, event.id, $trackBox);
                                viewer.putIndicator$(app.id, "event", event.id, $trackBox.find(".activity-status"));
                            } else if (event.id === "session") {
                                viewer.putDisplay$(app.id, event.id, this.addSessionBox($eventBox, node, app, event));
                            }
                        });
                        const $visualBox = this.addVisualBox(node, app);
                        app.events.forEach(event => {
                            if (event.id === "activity" || event.id === "session") {
                                viewer.putChart$(app.id, event.id, this.addChartBox($visualBox, node, app, event).find(".chart"));
                            }
                        });
                    }
                    if (app.metrics && app.metrics.length) {
                        const $eventBox = $(`.event-box[data-node-index=${node.index}][data-app-id=${app.id}]`);
                        app.metrics.forEach(metric => {
                            const $metric = (metric.heading || !$eventBox.length) ? 
                                this.addNodeMetric(node, metric) :
                                this.addAppMetric($eventBox, node, app, metric);
                            viewer.putMetric$(app.id, metric.id, $metric);
                        });
                    }
                    if (app.logs) {
                        app.logs.forEach(logInfo => {
                            const $consoleBox = this.addConsoleBox(node, app, logInfo);
                            const $console = $consoleBox.find(".console").data("tailing", true);
                            $consoleBox.find(".tailing-status").addClass("on");
                            viewer.putConsole$(app.id, logInfo.id, $console);
                            viewer.putIndicator$(app.id, "log", logInfo.id, $consoleBox.find(".status-bar"));
                        });
                    }
                }
            });
        });
        this.changeApp();
    }

    addGroupTab(groupInfo) {
        const $tabs = $(".group.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-group-id": groupInfo.id, "title": groupInfo.description });
        $tab.find("a .title").text(" " + (groupInfo.title || groupInfo.id) + " ");
        return $tab.show().appendTo($tabs);
    }

    addNodeTab(nodeInfo) {
        const $tabs = $(".node.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-node-id": nodeInfo.id , "data-group-id": nodeInfo.group });
        $tab.find("a .title").text(" " + (nodeInfo.title || nodeInfo.id) + " ");
        const nodesInGroup = this.nodes.filter(n => n.group === nodeInfo.group);
        if (nodesInGroup.length > 1) {
            $tab.find(".number").text(" " + nodeInfo.nodeNoInGroup);
        } else {
            $tab.find(".number").empty();
        }
        return $tab.show().appendTo($tabs);
    }

    addAppTab(appInfo) {
        const $tabs = $(".app.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-app-id": appInfo.id, "data-group-id": appInfo.group, "title": appInfo.title });
        $tab.find("a .title").text(" " + appInfo.title + " ");
        return $tab.show().appendTo($tabs);
    }

    addNodeMetricsBar(nodeInfo) {
        const $metricsBar = $(".node.metrics-bar");
        const $newBar = $metricsBar.first().hide().clone().addClass("available").attr("data-node-index", nodeInfo.index);
        $newBar.find(".number").text(" " + nodeInfo.nodeNoInGroup);
        return $newBar.insertAfter($metricsBar.last());
    }

    addNodeMetric(nodeInfo, metricInfo) {
        const $bar = $(`.node.metrics-bar[data-node-index=${nodeInfo.index}]`).show();
        const $metric = $bar.find(".metric").first().hide().clone().addClass("available");
        $metric.find("dt").text(metricInfo.title + " :").attr("title", metricInfo.description);
        return $metric.appendTo($bar).show();
    }

    addControlBar(appInfo) {
        const $bar = $(".control-bar");
        const $newBar = $bar.first().hide().clone().addClass("available").attr("data-app-id", appInfo.id);
        $newBar.find(".btn.default").text(this.counterPersistInterval + "min.");
        return $newBar.insertAfter($bar.last());
    }

    addEventBox(nodeInfo, appInfo) {
        const $box = $(".event-box").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id });
        const $titleBar = $box.find(".title-bar");
        $titleBar.find("h4").text(nodeInfo.title || nodeInfo.id);
        
        const nodesInGroup = this.nodes.filter(n => n.group === nodeInfo.group);
        if (nodesInGroup.length > 1) {
            $titleBar.find(".number").text(" " + nodeInfo.nodeNoInGroup);
        } else {
            $titleBar.find(".number").empty();
        }
        return $box.insertBefore($(".console-box").first());
    }

    addTrackBox($eventBox, nodeInfo, appInfo, eventInfo) {
        const $track = $eventBox.find(".track-box");
        return $track.first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .insertAfter($track.last()).show();
    }

    addAppMetric($eventBox, nodeInfo, appInfo, metricInfo) {
        const $bar = $eventBox.find(".metrics-bar").show();
        const $metric = $bar.find(".metric").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-metric-id": metricInfo.id });
        $metric.find("dt").text(metricInfo.title + " :").attr("title", metricInfo.description);
        return $metric.appendTo($bar).show();
    }

    addSessionBox($eventBox, nodeInfo, appInfo, eventInfo) {
        const $session = $eventBox.find(".session-box");
        return $session.first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .insertAfter($session.last()).show();
    }

    addVisualBox(nodeInfo, appInfo) {
        return $(".visual-box").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id })
            .insertBefore($(".console-box").first()).show();
    }

    addChartBox($visualBox, nodeInfo, appInfo, eventInfo) {
        const $chart = $visualBox.find(".chart-box");
        return $chart.first().hide().clone().addClass("available col-12 col-lg-6")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .appendTo($visualBox).show();
    }

    addConsoleBox(nodeInfo, appInfo, logInfo) {
        const $console = $(".console-box");
        const $newBox = $console.first().hide().clone().addClass("available col-lg-6")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-log-id": logInfo.id });
        $newBox.find(".status-bar h4").text((nodeInfo.title || nodeInfo.id) + " ›› " + logInfo.file);
        return $newBox.insertAfter($console.last());
    }
}

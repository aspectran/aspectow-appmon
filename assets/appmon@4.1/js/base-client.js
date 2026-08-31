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

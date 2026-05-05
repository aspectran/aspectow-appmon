/*
 * Aspectow AppMon 4.0
 * Last modified: 2026-04-29
 */

/**
 * The base class for AppMon communication clients.
 * Provides common functionality for connection management and retries.
 */
class BaseClient {
    constructor(node, viewer, onJoined, onEstablished, onClosed, onFailed) {
        this.node = node;
        this.viewer = viewer;
        this.onJoined = onJoined;
        this.onEstablished = onEstablished;
        this.onClosed = onClosed;
        this.onFailed = onFailed;
        this.retryCount = 0;
        this.maxRetries = 10;
        this.retryInterval = 5000;
        this.sessionId = null;
    }

    /**
     * Starts the client connection.
     * @param {string} [appsToJoin] - Names of apps to join.
     */
    start(appsToJoin) {
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
     */
    refresh(options) {
        this.checkSessionId();
        let cmdOptions = [
            "command:refresh",
            "sessionId:" + this.sessionId
        ];
        if (options) {
            cmdOptions.push(...options);
        }
        this.sendCommand(cmdOptions);
    }

    focus(appId) {
        this.checkSessionId();
        this.sendCommand([
            "command:focus",
            "sessionId:" + this.sessionId,
            "appId:" + appId
        ]);
    }

    loadPrevious(appId, logId, loadedLines) {
        this.checkSessionId();
        this.sendCommand([
            "command:loadPrevious",
            "sessionId:" + this.sessionId,
            "appId:" + appId,
            "logId:" + logId,
            "loadedLines:" + loadedLines
        ]);
    }

    checkSessionId() {
        if (!this.sessionId) {
            throw new Error("Session ID is not set. Cannot perform operation.");
        }
    }

    setSessionId(sessionId) {
        this.sessionId = sessionId;
    }

    clearSessionId() {
        this.sessionId = null;
    }

    /**
     * Sends a command with the specified options.
     * @param {string[]} [options] - Command options.
     */
    sendCommand(options) {
        throw new Error("Method 'sendCommand()' must be implemented.");
    }

    /**
     * Handles reconnection logic when a connection is lost or fails.
     * @param {string} [appsToJoin] - Names of apps to join.
     */
    rejoin(appsToJoin) {
        if (this.retryCount++ < this.maxRetries) {
            const retryInterval = (this.retryInterval * this.retryCount) + (this.node.index * 200) + this.node.random1000;
            const status = "(" + this.retryCount + "/" + this.maxRetries + ", interval=" + retryInterval + ")";
            console.log(this.node.id, "trying to reconnect", status);
            this.viewer.printMessage("Trying to reconnect... " + status);
            setTimeout(() => {
                this.start(appsToJoin);
            }, retryInterval);
        } else {
            console.log(this.node.id, "abort reconnect attempt");
            this.viewer.printMessage("Max connection attempts exceeded.");
            if (this.onFailed) {
                this.onFailed(this.node);
            }
        }
    }
}

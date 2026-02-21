/*
 * Aspectow AppMon 3.0
 * Last modified: 2026-02-15
 */

/**
 * The base class for AppMon communication clients.
 * Provides common functionality for connection management and retries.
 */
class BaseClient {
    constructor(domain, viewer, onJoined, onEstablished, onClosed, onFailed) {
        this.domain = domain;
        this.viewer = viewer;
        this.onJoined = onJoined;
        this.onEstablished = onEstablished;
        this.onClosed = onClosed;
        this.onFailed = onFailed;
        this.retryCount = 0;
        this.maxRetries = 10;
        this.retryInterval = 5000;
    }

    /**
     * Starts the client connection.
     * @param {string} [instancesToJoin] - Names of instances to join.
     */
    start(instancesToJoin) {
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
        throw new Error("Method 'refresh()' must be implemented.");
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
     * @param {string} [instancesToJoin] - Names of instances to join.
     */
    rejoin(instancesToJoin) {
        if (this.retryCount++ < this.maxRetries) {
            const retryInterval = (this.retryInterval * this.retryCount) + (this.domain.index * 200) + this.domain.random1000;
            const status = "(" + this.retryCount + "/" + this.maxRetries + ", interval=" + retryInterval + ")";
            console.log(this.domain.name, "trying to reconnect", status);
            this.viewer.printMessage("Trying to reconnect... " + status);
            setTimeout(() => {
                this.start(instancesToJoin);
            }, retryInterval);
        } else {
            console.log(this.domain.name, "abort reconnect attempt");
            this.viewer.printMessage("Max connection attempts exceeded.");
            if (this.onFailed) {
                this.onFailed(this.domain);
            }
        }
    }
}

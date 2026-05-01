/*
 * Aspectow AppMon 4.0
 * Last modified: 2026-04-29
 */

/**
 * HTTP Polling implementation of the AppMon client.
 */
class PollingClient extends BaseClient {
    constructor(node, viewer, onJoined, onEstablished, onClosed, onFailed) {
        super(node, viewer, onJoined, onEstablished, onClosed, onFailed);
        this.endpointMode = "polling";
        this.commands = [];
        this.pollingTimer = null;
        this.stopped = false;
    }

    start(appsToJoin) {
        this.stopped = false;
        this.join(appsToJoin);
    }

    stop() {
        this.stopped = true;
        if (this.pollingTimer) {
            clearTimeout(this.pollingTimer);
            this.pollingTimer = null;
        }
    }

    speed(speed) {
        this.changePollingInterval(speed);
    }

    refresh(options) {
        let cmdOptions = ["command:refresh"];
        if (options) {
            cmdOptions.push(...options);
        }
        this.sendCommand(cmdOptions);
    }

    sendCommand(options) {
        if (options) {
            options.forEach(option => this.withCommand(option));
        }
    }

    withCommand(command) {
        if (!this.commands.includes(command)) {
            this.commands.push(command);
        }
    }

    join(appsToJoin) {
        $.ajax({
            url: this.node.endpoint.path + "/appmon/polling/join",
            type: "post",
            dataType: "json",
            data: {
                timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                appsToJoin: appsToJoin
            },
            success: (data) => {
                if (data) {
                    this.retryCount = 0;
                    this.node.endpoint['mode'] = this.endpointMode;
                    this.node.endpoint['pollingInterval'] = data.pollingInterval;
                    if (this.onJoined) {
                        this.onJoined(this.node, data);
                    }
                    if (this.onEstablished) {
                        this.onEstablished(this.node);
                    }
                    this.viewer.printMessage("Polling every " + data.pollingInterval + " milliseconds.");
                    this.polling(appsToJoin);
                } else {
                    console.log(this.node.id, "connection failed");
                    this.viewer.printErrorMessage("Connection failed.");
                    this.rejoin(appsToJoin);
                }
            },
            error: (xhr, status, error) => {
                console.log(this.node.id, "connection failed", error);
                this.viewer.printErrorMessage("Connection failed.");
                this.rejoin(appsToJoin);
            }
        });
    }

    polling(appsToJoin) {
        if (this.stopped) return;
        let withCommands = null;
        if (this.commands.length) {
            withCommands = this.commands.slice();
            this.commands.length = 0;
        }
        $.ajax({
            url: this.node.endpoint.path + "/appmon/polling/pull",
            type: "get",
            cache: false,
            data: withCommands ? {
                commands: withCommands
            } : null,
            success: (data) => {
                if (this.stopped) return;
                if (data && data.messages) {
                    data.messages.forEach(msg => this.viewer.processMessage(msg));
                    this.pollingTimer = setTimeout(() => {
                        this.polling(appsToJoin);
                    }, this.node.endpoint.pollingInterval);
                } else {
                    console.log(this.node.id, "connection lost");
                    this.viewer.printErrorMessage("Connection lost.");
                    if (this.onClosed) {
                        this.onClosed(this.node);
                    }
                    this.rejoin(appsToJoin);
                }
            },
            error: (xhr, status, error) => {
                if (this.stopped) return;
                console.log(this.node.id, "connection lost", error);
                this.viewer.printErrorMessage("Connection lost.");
                if (this.onClosed) {
                    this.onClosed(this.node);
                }
                this.rejoin(appsToJoin);
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
}

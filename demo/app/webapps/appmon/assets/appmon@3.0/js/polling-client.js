/*
 * Aspectow AppMon 3.0
 * Last modified: 2026-02-21
 */

/**
 * HTTP Polling implementation of the AppMon client.
 */
class PollingClient extends BaseClient {
    constructor(domain, viewer, onJoined, onEstablished, onClosed, onFailed) {
        super(domain, viewer, onJoined, onEstablished, onClosed, onFailed);
        this.endpointMode = "polling";
        this.commands = [];
    }

    start(instancesToJoin) {
        this.join(instancesToJoin);
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

    join(instancesToJoin) {
        $.ajax({
            url: this.domain.endpoint.url + "/polling/join",
            type: "post",
            dataType: "json",
            data: {
                timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                instances: instancesToJoin
            },
            success: (data) => {
                if (data) {
                    this.retryCount = 0;
                    this.domain.endpoint['mode'] = this.endpointMode;
                    this.domain.endpoint['pollingInterval'] = data.pollingInterval;
                    if (this.onJoined) {
                        this.onJoined(this.domain, data);
                    }
                    if (this.onEstablished) {
                        this.onEstablished(this.domain);
                    }
                    this.viewer.printMessage("Polling every " + data.pollingInterval + " milliseconds.");
                    this.polling(instancesToJoin);
                } else {
                    console.log(this.domain.name, "connection failed");
                    this.viewer.printErrorMessage("Connection failed.");
                    this.rejoin(instancesToJoin);
                }
            },
            error: (xhr, status, error) => {
                console.log(this.domain.name, "connection failed", error);
                this.viewer.printErrorMessage("Connection failed.");
                this.rejoin(instancesToJoin);
            }
        });
    }

    polling(instancesToJoin) {
        let withCommands = null;
        if (this.commands.length) {
            withCommands = this.commands.slice();
            this.commands.length = 0;
        }
        $.ajax({
            url: this.domain.endpoint.url + "/polling/pull",
            type: "get",
            cache: false,
            data: withCommands ? {
                commands: withCommands
            } : null,
            success: (data) => {
                if (data && data.messages) {
                    data.messages.forEach(msg => this.viewer.processMessage(msg));
                    setTimeout(() => {
                        this.polling(instancesToJoin);
                    }, this.domain.endpoint.pollingInterval);
                } else {
                    console.log(this.domain.name, "connection lost");
                    this.viewer.printErrorMessage("Connection lost.");
                    if (this.onClosed) {
                        this.onClosed(this.domain);
                    }
                    this.rejoin(instancesToJoin);
                }
            },
            error: (xhr, status, error) => {
                console.log(this.domain.name, "connection lost", error);
                this.viewer.printErrorMessage("Connection lost.");
                if (this.onClosed) {
                    this.onClosed(this.domain);
                }
                this.rejoin(instancesToJoin);
            }
        });
    }

    changePollingInterval(speed) {
        $.ajax({
            url: this.domain.endpoint.url + "/polling/interval",
            type: "post",
            dataType: "json",
            data: { speed: speed },
            success: (data) => {
                if (data && data.pollingInterval) {
                    this.domain.endpoint.pollingInterval = data.pollingInterval;
                    console.log(this.domain.name, "pollingInterval", data.pollingInterval);
                    this.viewer.printMessage("Polling every " + data.pollingInterval + " milliseconds.");
                } else {
                    console.log(this.domain.name, "failed to change polling interval");
                    this.viewer.printMessage("Failed to change polling interval.");
                }
            },
            error: (xhr, status, error) => {
                console.log(this.domain.name, "failed to change polling interval", error);
                this.viewer.printMessage("Failed to change polling interval.");
            }
        });
    }
}

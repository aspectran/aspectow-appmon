function PollingClient(domain, viewer, onJoined, onEstablished, onClosed, onFailed) {

    const ENDPOINT_MODE = "polling";
    const MAX_RETRIES = 10;
    const RETRY_INTERVAL = 5000;

    let retryCount = 0;
    let commands = [];

    this.start = function (specificInstances) {
        join(specificInstances);
    };

    this.speed = function (speed) {
        changePollingInterval(speed);
    };

    this.refresh = function () {
        withCommand("refresh");
    };

    const withCommand = function (command) {
        if (!commands.includes(command)) {
            commands.push(command);
        }
    }

    const join = function (specificInstances) {
        $.ajax({
            url: domain.endpoint.url + "/" + domain.endpoint.token + "/polling/join",
            type: "post",
            dataType: "json",
            data: {
                instances: specificInstances
            },
            success: function (data) {
                if (data) {
                    retryCount = 0;
                    domain.endpoint['mode'] = ENDPOINT_MODE;
                    domain.endpoint['token'] = data.token;
                    domain.endpoint['pollingInterval'] = data.pollingInterval;
                    if (onJoined) {
                        onJoined(domain, data);
                    }
                    if (onEstablished) {
                        onEstablished(domain);
                    }
                    viewer.printMessage("Polling every " + data.pollingInterval + " milliseconds.");
                    polling(specificInstances);
                } else {
                    console.log(domain.name, "connection failed");
                    viewer.printErrorMessage("Connection failed.");
                    rejoin(specificInstances);
                }
            },
            error: function (xhr, status, error) {
                console.log(domain.name, "connection failed", error);
                viewer.printErrorMessage("Connection failed.");
                rejoin(specificInstances);
            }
        });
    };

    const rejoin = function (specificInstances) {
        if (retryCount++ < MAX_RETRIES) {
            let retryInterval = (RETRY_INTERVAL * retryCount) + (domain.index * 200) + domain.random1000;
            let status = "(" + retryCount + "/" + MAX_RETRIES + ", interval=" + retryInterval + ")";
            console.log(domain.name, "trying to reconnect", status);
            viewer.printMessage("Trying to reconnect... " + status);
            setTimeout(function () {
                join(specificInstances);
            }, retryInterval);
        } else {
            viewer.printMessage("Max connection attempts exceeded.");
            if (onFailed) {
                onFailed(domain);
            }
        }
    };

    const polling = function (specificInstances) {
        let withCommands = null;
        if (commands.length) {
            withCommands = commands.slice();
            commands.length = 0;
        }
        $.ajax({
            url: domain.endpoint.url + "/" + domain.endpoint.token + "/polling/pull",
            type: "get",
            data: {
                commands: withCommands
            },
            success: function (data) {
                if (data && data.token && data.messages) {
                    domain.endpoint['token'] = data.token;
                    for (let key in data.messages) {
                        viewer.processMessage(data.messages[key]);
                    }
                    setTimeout(function () {
                        polling(specificInstances);
                    }, domain.endpoint.pollingInterval);
                } else {
                    console.log(domain.name, "connection lost");
                    viewer.printErrorMessage("Connection lost.");
                    if (onClosed) {
                        onClosed(domain);
                    }
                    rejoin(specificInstances);
                }
            },
            error: function (xhr, status, error) {
                console.log(domain.name, "connection lost", error);
                viewer.printErrorMessage("Connection lost.");
                if (onClosed) {
                    onClosed(domain);
                }
                rejoin(specificInstances);
            }
        });
    };

    const changePollingInterval = function (speed) {
        $.ajax({
            url: domain.endpoint.url + "/" + domain.endpoint.token + "/polling/interval",
            type: "post",
            dataType: "json",
            data: {
                speed: speed
            },
            success: function (data) {
                if (data && data.pollingInterval) {
                    domain.endpoint.pollingInterval = data.pollingInterval;
                    console.log(domain.name, "pollingInterval", data.pollingInterval);
                    viewer.printMessage("Polling every " + data + " milliseconds.");
                } else {
                    console.log(domain.name, "failed to change polling interval");
                    viewer.printMessage("Failed to change polling interval.");
                }
            },
            error: function (xhr, status, error) {
                console.log(domain.name, "failed to change polling interval", error);
                viewer.printMessage("Failed to change polling interval.");
            }
        });
    };
}

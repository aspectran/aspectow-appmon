function PollingClient(endpoint, viewer, onJoined, onEstablished) {

    const MODE = "polling";
    const MAX_RETRIES = 10;
    const RETRY_INTERVAL = 5000;

    let retryCount = 0;

    this.start = function (specificInstances) {
        join(specificInstances);
    };

    this.speed = function (speed) {
        changePollingInterval(speed);
    };

    const join = function (specificInstances) {
        $.ajax({
            url: endpoint.url + "/" + endpoint.token + "/polling/join",
            type: "post",
            dataType: "json",
            data: {
                instances: specificInstances
            },
            success: function (data) {
                if (data) {
                    retryCount = 0;
                    endpoint['mode'] = MODE;
                    endpoint['token'] = data.token;
                    endpoint['pollingInterval'] = data.pollingInterval;
                    if (onJoined) {
                        onJoined(endpoint, data);
                    }
                    if (onEstablished) {
                        onEstablished(endpoint);
                    }
                    viewer.printMessage("Polling every " + data.pollingInterval + " milliseconds.");
                    polling(specificInstances);
                } else {
                    console.log(endpoint.name, "connection failed");
                    viewer.printErrorMessage("Connection failed.");
                    rejoin(specificInstances);
                }
            },
            error: function (xhr, status, error) {
                console.log(endpoint.name, "connection failed", error);
                viewer.printErrorMessage("Connection failed.");
                rejoin(specificInstances);
            }
        });
    };

    const rejoin = function (specificInstances) {
        if (retryCount++ < MAX_RETRIES) {
            let retryInterval = (RETRY_INTERVAL * retryCount) + (endpoint.index * 200) + endpoint.random1000;
            let status = "(" + retryCount + "/" + MAX_RETRIES + ", interval=" + retryInterval + ")";
            console.log(endpoint.name, "trying to reconnect", status);
            viewer.printMessage("Trying to reconnect... " + status);
            setTimeout(function () {
                join(specificInstances);
            }, retryInterval);
        } else {
            viewer.printMessage("Max connection attempts exceeded.");
        }
    };

    const polling = function (specificInstances) {
        $.ajax({
            url: endpoint.url + "/" + endpoint.token + "/polling/pull",
            type: "get",
            success: function (data) {
                if (data && data.token && data.messages) {
                    endpoint['token'] = data.token;
                    for (let key in data.messages) {
                        viewer.processMessage(data.messages[key]);
                    }
                    setTimeout(function () {
                        polling(specificInstances);
                    }, endpoint.pollingInterval);
                } else {
                    console.log(endpoint.name, "connection lost");
                    viewer.printErrorMessage("Connection lost.");
                    rejoin(specificInstances);
                }
            },
            error: function (xhr, status, error) {
                console.log(endpoint.name, "connection lost", error);
                viewer.printErrorMessage("Connection lost.");
                rejoin(specificInstances);
            }
        });
    };

    const changePollingInterval = function (speed) {
        $.ajax({
            url: endpoint.url + "/" + endpoint.token + "/polling/interval",
            type: "post",
            dataType: "json",
            data: {
                speed: speed
            },
            success: function (data) {
                if (data && data.pollingInterval) {
                    endpoint.pollingInterval = data.pollingInterval;
                    console.log(endpoint.name, "pollingInterval", data.pollingInterval);
                    viewer.printMessage("Polling every " + data + " milliseconds.");
                } else {
                    console.log(endpoint.name, "failed to change polling interval");
                    viewer.printMessage("Failed to change polling interval.");
                }
            },
            error: function (xhr, status, error) {
                console.log(endpoint.name, "failed to change polling interval", error);
                viewer.printMessage("Failed to change polling interval.");
            }
        });
    };
}

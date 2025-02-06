function PollingClient(endpoint, viewer, onJoined, onEstablished) {

    const MODE = "polling";
    const MAX_RETRIES = 10;
    const RETRY_INTERVAL = 5000;

    let retryCount = 0;

    this.start = function (joinInstances) {
        join(joinInstances);
    };

    this.speed = function (speed) {
        changePollingInterval(speed);
    };

    const join = function (joinInstances) {
        $.ajax({
            url: endpoint.url + "/" + endpoint.token + "/polling/join",
            type: "post",
            dataType: "json",
            data: {
                joinInstances: joinInstances
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
                    polling(joinInstances);
                } else {
                    console.log(endpoint.name, "connection failed");
                    viewer.printErrorMessage("Connection failed.");
                    rejoin(joinInstances);
                }
            },
            error: function () {
                console.log(endpoint.name, "connection failed");
                viewer.printErrorMessage("Connection failed.");
                rejoin(joinInstances);
            }
        });
    };

    const rejoin = function (joinInstances) {
        if (retryCount++ < MAX_RETRIES) {
            let retryInterval = RETRY_INTERVAL * retryCount + random(1, 1000);
            let status = "(" + retryCount + "/" + MAX_RETRIES + ", interval=" + retryInterval + ")";
            console.log(endpoint.name, "reconnect", status);
            viewer.printMessage("Trying to reconnect... " + status);
            setTimeout(function () {
                join(joinInstances);
            }, retryInterval);
        } else {
            viewer.printMessage("Max connection attempts exceeded.");
        }
    };

    const polling = function (joinInstances) {
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
                        polling(joinInstances);
                    }, endpoint.pollingInterval);
                } else {
                    console.log(endpoint.name, "connection lost");
                    viewer.printErrorMessage("Connection lost.");
                    rejoin(joinInstances);
                }
            },
            error: function () {
                console.log(endpoint.name, "connection lost");
                viewer.printErrorMessage("Connection lost.");
                rejoin(joinInstances);
            }
        });
    };

    const changePollingInterval = function (speed) {
        $.ajax({
            url: endpoint.basePath + "backend/polling/" + endpoint.token + "/pollingInterval",
            type: 'post',
            dataType: "json",
            data: {
                speed: speed
            },
            success: function (data) {
                console.log(endpoint.name, "pollingInterval", data);
                if (data) {
                    endpoint.pollingInterval = data;
                    viewer.printMessage("Polling every " + data + " milliseconds.");
                }
            }
        });
    };

    const random = function(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    };
}

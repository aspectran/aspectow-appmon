function PollingClient(endpoint, onEndpointJoined, onEstablishCompleted) {
    this.start = function () {
        join();
    };

    this.speed = function (speed) {
        changePollingInterval(speed);
    };

    const join = function () {
        $.ajax({
            url: endpoint.basePath + "backend/endpoint/" + endpoint.token + "/join",
            type: 'post',
            dataType: "json",
            success: function (data) {
                if (data) {
                    endpoint['mode'] = "polling";
                    endpoint['token'] = data.token;
                    endpoint['pollingInterval'] = data.pollingInterval;
                    if (onEndpointJoined) {
                        onEndpointJoined(endpoint, data);
                    }
                    if (onEstablishCompleted) {
                        onEstablishCompleted(endpoint, data);
                    }
                    endpoint.viewer.printMessage("Polling every " + data.pollingInterval + " milliseconds.");
                    polling();
                }
            }
        });
    };

    const polling = function () {
        $.ajax({
            url: endpoint.basePath + "backend/endpoint/" + endpoint.token + "/pull",
            type: 'get',
            success: function (data) {
                if (data && data.token && data.messages) {
                    endpoint['token'] = data.token;
                    for (let key in data.messages) {
                        endpoint.viewer.processMessage(data.messages[key]);
                    }
                    setTimeout(polling, endpoint.pollingInterval);
                } else {
                    endpoint.viewer.printErrorMessage("Connection lost. Please refresh this page to try again!");
                }
            }
        });
    };

    const changePollingInterval = function (speed) {
        $.ajax({
            url: endpoint.basePath + "backend/endpoint/" + endpoint.token + "/pollingInterval",
            type: 'post',
            dataType: "json",
            data: {
                speed: speed
            },
            success: function (data) {
                console.log("pollingInterval", data);
                if (data) {
                    endpoint.pollingInterval = data;
                    endpoint.viewer.printMessage("Polling every " + data + " milliseconds.");
                }
            }
        });
    };
}

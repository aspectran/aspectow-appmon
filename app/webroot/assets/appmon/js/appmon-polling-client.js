function AppmonPollingClient(endpoint, onEndpointJoined, onEstablishCompleted) {
    this.start = function () {
        join();
    };

    this.speed = function (speed) {
        changePollingInterval(speed);
    };

    const join = function () {
        $.ajax({
            url: endpoint.basePath + "appmon/endpoint/join",
            type: 'post',
            dataType: "json",
            success: function (payload) {
                if (payload) {
                    endpoint['mode'] = "polling";
                    endpoint['pollingInterval'] = payload.pollingInterval;
                    if (onEndpointJoined) {
                        onEndpointJoined(endpoint, payload);
                    }
                    if (onEstablishCompleted) {
                        onEstablishCompleted(endpoint, payload);
                    }
                    endpoint.viewer.printEventMessage("Polling every " + payload.pollingInterval + " milliseconds.");
                    polling();
                }
            }
        });
    };

    const rejoin = function () {
        endpoint.viewer.printErrorMessage("Connection lost. It will retry in 5 seconds.");
        setTimeout(function () {
            location.reload();
        }, 5000)
    };

    const polling = function () {
        $.ajax({
            url: endpoint.basePath + "appmon/endpoint/pull",
            type: 'get',
            success: function (data) {
                if (data) {
                    for (let key in data) {
                        endpoint.viewer.processMessage(data[key]);
                    }
                    setTimeout(polling, endpoint.pollingInterval);
                } else {
                    rejoin();
                }
            }
        });
    };

    const changePollingInterval = function (speed) {
        $.ajax({
            url: endpoint.basePath + "appmon/endpoint/pollingInterval",
            type: 'post',
            dataType: "json",
            data: {
                speed: speed
            },
            success: function (data) {
                console.log("pollingInterval", data);
                if (data) {
                    endpoint.pollingInterval = data;
                    endpoint.viewer.printEventMessage("Polling every " + data + " milliseconds.");
                }
            }
        });
    };
}

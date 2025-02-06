function WebsocketClient(endpoint, viewer, onJoined, onEstablished, onFailed) {

    const MODE = "websocket";
    const MAX_RETRIES = 10;
    const RETRY_INTERVAL = 5000;
    const HEARTBEAT_INTERVAL = 5000;

    let socket = null;
    let heartbeatTimer = null;
    let pendingMessages = [];
    let established = false;
    let retryCount = 0;

    this.start = function (joinInstances) {
        openSocket(joinInstances);
    };

    this.stop = function () {
        closeSocket();
    };

    const openSocket = function (joinInstances) {
        // For test
        // onFailed(endpoint);
        // return;
        closeSocket(false);
        let url = new URL(endpoint.url + "/" + endpoint.token, location.href);
        url.protocol = url.protocol.replace("https:", "wss:");
        url.protocol = url.protocol.replace("http:", "ws:");
        socket = new WebSocket(url.href);
        socket.onopen = function () {
            console.log(endpoint.name, "socket connected:", endpoint.url);
            pendingMessages.push("Socket connection successful");
            socket.send("join:" + (joinInstances||""));
            heartbeatPing();
            retryCount = 0;
        };
        socket.onmessage = function (event) {
            if (typeof event.data === "string") {
                let msg = event.data;
                if (established) {
                    if (msg.startsWith("pong:")) {
                        endpoint.token = msg.substring(5);
                        heartbeatPing();
                    } else {
                        viewer.processMessage(msg);
                    }
                } else if (msg.startsWith("joined:")) {
                    console.log(endpoint.name, msg, endpoint.token);
                    let payload = (msg.length > 7 ? JSON.parse(msg.substring(7)) : null);
                    establish(payload);
                }
            }
        };
        socket.onclose = function (event) {
            closeSocket(true);
            if (event.code === 1003) {
                console.log(endpoint.name, "socket connection refused: ", event.code);
                viewer.printMessage("Socket connection refused by server.");
                return;
            }
            if (event.code === 1000 || retryCount === 0) {
                console.log(endpoint.name, "socket connection closed: ", event.code);
                viewer.printMessage("Socket connection closed.");
            }
            if (event.code !== 1000) {
                if (retryCount++ < MAX_RETRIES) {
                    let retryInterval = RETRY_INTERVAL * retryCount + random(1, 1000);
                    let status = "(" + retryCount + "/" + MAX_RETRIES + ", interval=" + retryInterval + ")";
                    console.log(endpoint.name, "reconnect " + status);
                    viewer.printMessage("Trying to reconnect... " + status);
                    setTimeout(function () {
                        openSocket(joinInstances);
                    }, retryInterval);
                } else {
                    viewer.printMessage("Max connection attempts exceeded");
                }
            }
        };
        socket.onerror = function (event) {
            if (endpoint.mode === MODE) {
                console.error(endpoint.name, "webSocket error:", event);
                viewer.printErrorMessage("Could not connect to the WebSocket server.");
            } else if (onFailed) {
                onFailed(endpoint);
            }
        };
    };

    const closeSocket = function (afterClosing) {
        if (socket) {
            established = false;
            if (!afterClosing) {
                socket.close();
            }
            socket = null;
        }
    };

    const establish = function (payload) {
        if (onJoined) {
            endpoint['mode'] = MODE;
            onJoined(endpoint, payload);
        }
        while (pendingMessages.length) {
            viewer.printMessage(pendingMessages.shift());
        }
        if (onEstablished) {
            onEstablished(endpoint);
        }
        while (pendingMessages.length) {
            viewer.printMessage(pendingMessages.shift());
        }
        established = true;
        socket.send("established:");
    };

    const heartbeatPing = function () {
        if (heartbeatTimer) {
            clearTimeout(heartbeatTimer);
        }
        heartbeatTimer = setTimeout(function () {
            if (socket) {
                socket.send("ping:");
            }
        }, HEARTBEAT_INTERVAL);
    };

    const random = function(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    };
}

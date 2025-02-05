function WebsocketClient(endpoint, viewer, onEndpointJoined, onEstablishCompleted, onErrorObserved) {

    const MODE = "websocket";
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
        // onErrorObserved(endpoint);
        // return;
        closeSocket();
        let url = new URL(endpoint.url + '/' + endpoint.token, location.href);
        url.protocol = url.protocol.replace('https:', 'wss:');
        url.protocol = url.protocol.replace('http:', 'ws:');
        socket = new WebSocket(url.href);
        socket.onopen = function (event) {
            pendingMessages.push("Socket connection successful");
            socket.send("join:" + (joinInstances||""));
            heartbeatPing();
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
                    console.log(msg);
                    let payload = (msg.length > 7 ? JSON.parse(msg.substring(7)) : null);
                    establish(payload);
                }
            }
        };
        socket.onclose = function (event) {
            if (event.code === 1000) {
                viewer.printMessage("Socket connection closed.");
            } else {
                closeSocket();
                if (retryCount++ < 10) {
                    viewer.printMessage("Socket connection closed. Trying to reconnect...");
                    setTimeout(function () {
                        openSocket();
                    }, 5000);
                }
            }
        };
        socket.onerror = function (event) {
            if (endpoint.mode === "websocket") {
                console.error("WebSocket error observed:", event);
                viewer.printErrorMessage("Could not connect to WebSocket server.");
            } else if (onErrorObserved) {
                onErrorObserved(endpoint);
            }
        };
    };

    const closeSocket = function () {
        if (socket) {
            socket.close();
            socket = null;
        }
    };

    const establish = function (payload) {
        if (onEndpointJoined) {
            endpoint['mode'] = MODE;
            onEndpointJoined(endpoint, payload);
        }
        while (pendingMessages.length) {
            viewer.printMessage(pendingMessages.shift());
        }
        if (onEstablishCompleted) {
            onEstablishCompleted(endpoint);
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
        }, 50000);
    };
}

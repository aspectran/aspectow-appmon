function WebsocketClient(domain, viewer, onJoined, onEstablished, onClosed, onFailed) {

    const ENDPOINT_MODE = "websocket";
    const MAX_RETRIES = 10;
    const RETRY_INTERVAL = 5000;
    const HEARTBEAT_INTERVAL = 5000;

    let socket = null;
    let retryCount = 0;
    let heartbeatTimer = null;
    let pendingMessages = [];
    let established = false;

    this.start = function (instancesToJoin) {
        openSocket(instancesToJoin);
    };

    this.stop = function () {
        closeSocket();
    };

    this.refresh = function (options) {
        if (socket) {
            socket.send("command:refresh;" + (options ? options.join(";") : ""));
        }
    };

    const openSocket = function (instancesToJoin) {
        closeSocket(false);
        let url = new URL(domain.endpoint.url + "/" + domain.endpoint.token + "/websocket", location.href);
        url.protocol = url.protocol.replace("https:", "wss:");
        url.protocol = url.protocol.replace("http:", "ws:");
        socket = new WebSocket(url.href);
        socket.onopen = function () {
            console.log(domain.name, "socket connected:", domain.endpoint.url);
            pendingMessages.push("Socket connection successful");
            let options = [];
            options.push("command:join");
            options.push("timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone);
            if (instancesToJoin) {
                options.push("instancesToJoin:" + instancesToJoin);
            }
            socket.send(options.join(";"));
            heartbeatPing();
            retryCount = 0;
        };
        socket.onmessage = function (event) {
            if (typeof event.data === "string") {
                let msg = event.data;
                if (established) {
                    if (msg.startsWith("pong:")) {
                        domain.endpoint.token = msg.substring(5);
                        heartbeatPing();
                    } else {
                        viewer.processMessage(msg);
                    }
                } else if (msg.startsWith("joined:")) {
                    console.log(domain.name, msg, domain.endpoint.token);
                    let payload = (msg.length > 7 ? JSON.parse(msg.substring(7)) : null);
                    establish(payload);
                }
            }
        };
        socket.onclose = function (event) {
            closeSocket(true);
            if (domain.endpoint.mode === ENDPOINT_MODE) {
                if (onClosed) {
                    onClosed(domain);
                }
                if (event.code === 1003) {
                    console.log(domain.name, "socket connection refused: ", event.code);
                    viewer.printErrorMessage("Socket connection refused by server.");
                    return;
                }
                if (event.code === 1000 || retryCount === 0) {
                    console.log(domain.name, "socket connection closed: ", event.code);
                    viewer.printMessage("Socket connection closed.");
                }
                if (event.code !== 1000) {
                    if (retryCount++ < MAX_RETRIES) {
                        let retryInterval = (RETRY_INTERVAL * retryCount) + (domain.index * 200) + domain.random1000;
                        let status = "(" + retryCount + "/" + MAX_RETRIES + ", interval=" + retryInterval + ")";
                        console.log(domain.name, "trying to reconnect", status);
                        viewer.printMessage("Trying to reconnect... " + status);
                        setTimeout(function () {
                            openSocket(instancesToJoin);
                        }, retryInterval);
                    } else {
                        console.log(domain.name, "abort reconnect attempt");
                        viewer.printMessage("Max connection attempts exceeded.");
                        if (onFailed) {
                            onFailed(domain);
                        }
                    }
                }
            }
        };
        socket.onerror = function (event) {
            if (domain.endpoint.mode === ENDPOINT_MODE) {
                console.log(domain.name, "websocket error:", event);
                viewer.printErrorMessage("Could not connect to the WebSocket server.");
            }
            if (onFailed) {
                onFailed(domain);
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
        domain.endpoint['mode'] = ENDPOINT_MODE;
        if (onJoined) {
            onJoined(domain, payload);
        }
        while (pendingMessages.length) {
            viewer.printMessage(pendingMessages.shift());
        }
        if (onEstablished) {
            onEstablished(domain);
        }
        while (pendingMessages.length) {
            viewer.printMessage(pendingMessages.shift());
        }
        established = true;
        socket.send("command:established");
    };

    const heartbeatPing = function () {
        if (heartbeatTimer) {
            clearTimeout(heartbeatTimer);
        }
        heartbeatTimer = setTimeout(function () {
            if (socket) {
                socket.send("command:ping");
            }
        }, HEARTBEAT_INTERVAL);
    };
}

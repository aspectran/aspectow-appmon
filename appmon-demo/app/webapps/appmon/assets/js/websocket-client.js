/*
 * Aspectow AppMon 4.0
 * Last modified: 2026-04-29
 */

/**
 * WebSocket implementation of the AppMon client.
 */
class WebsocketClient extends BaseClient {
    constructor(node, viewer, onJoined, onEstablished, onClosed, onFailed) {
        super(node, viewer, onJoined, onEstablished, onClosed, onFailed);
        this.endpointMode = "websocket";
        this.heartbeatInterval = 5000;
        this.socket = null;
        this.heartbeatTimer = null;
        this.pendingMessages = [];
        this.established = false;
    }

    start(appsToJoin) {
        this.openSocket(appsToJoin);
    }

    stop() {
        this.closeSocket();
    }

    refresh(options) {
        let cmdOptions = ["command:refresh"];
        if (options) {
            cmdOptions.push(...options);
        }
        this.sendCommand(cmdOptions);
    }

    sendCommand(options) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(options ? options.join(";") : "");
        }
    }

    openSocket(appsToJoin) {
        this.closeSocket(false);
        const url = new URL(this.node.endpoint.path + "/appmon/websocket/" + this.node.endpoint.token, location.href);
        url.protocol = url.protocol.replace("https:", "wss:").replace("http:", "ws:");

        this.socket = new WebSocket(url.href);

        this.socket.onopen = () => {
            console.log(this.node.id, "socket connected:", this.node.endpoint.path);
            this.pendingMessages.push("Socket connection successful");
            const options = [
                "command:join",
                "timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone
            ];
            if (appsToJoin) {
                options.push("appsToJoin:" + appsToJoin);
            }
            this.socket.send(options.join(";"));
            this.heartbeatPing();
            this.retryCount = 0;
        };

        this.socket.onmessage = (event) => {
            if (typeof event.data === "string") {
                const msg = event.data;
                if (this.established) {
                    if (msg.startsWith("pong:")) {
                        this.node.endpoint.token = msg.substring(5);
                        this.heartbeatPing();
                    } else {
                        this.viewer.processMessage(msg);
                    }
                } else if (msg.startsWith("joined:")) {
                    console.log(this.node.id, msg, this.node.endpoint.token);
                    const payload = (msg.length > 7 ? JSON.parse(msg.substring(7)) : null);
                    this.establish(payload);
                }
            }
        };

        this.socket.onclose = (event) => {
            this.closeSocket(true);
            if (this.node.endpoint.mode === this.endpointMode) {
                if (this.onClosed) {
                    this.onClosed(this.node);
                }
                if (event.code === 1003) {
                    console.log(this.node.id, "socket connection refused: ", event.code);
                    this.viewer.printErrorMessage("Socket connection refused by server.");
                    return;
                }
                if (event.code === 1000 || this.retryCount === 0) {
                    console.log(this.node.id, "socket connection closed: ", event.code);
                    this.viewer.printMessage("Socket connection closed.");
                }
                if (event.code !== 1000) {
                    this.rejoin(appsToJoin);
                }
            }
        };

        this.socket.onerror = (event) => {
            if (this.node.endpoint.mode === this.endpointMode) {
                console.log(this.node.id, "websocket error:", event);
                this.viewer.printErrorMessage("Could not connect to the WebSocket server.");
            }
            if (this.onFailed) {
                this.onFailed(this.node);
            }
        };
    }

    closeSocket(afterClosing) {
        if (this.socket) {
            this.established = false;
            if (!afterClosing) {
                this.socket.close();
            }
            this.socket = null;
        }
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }

    establish(payload) {
        this.node.endpoint['mode'] = this.endpointMode;
        if (this.onJoined) {
            this.onJoined(this.node, payload);
        }
        while (this.pendingMessages.length) {
            this.viewer.printMessage(this.pendingMessages.shift());
        }
        if (this.onEstablished) {
            this.onEstablished(this.node);
        }
        while (this.pendingMessages.length) {
            this.viewer.printMessage(this.pendingMessages.shift());
        }
        this.established = true;
        this.socket.send("command:established");
    }

    heartbeatPing() {
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
        }
        this.heartbeatTimer = setTimeout(() => {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                this.socket.send("command:ping");
            }
        }, this.heartbeatInterval);
    }
}

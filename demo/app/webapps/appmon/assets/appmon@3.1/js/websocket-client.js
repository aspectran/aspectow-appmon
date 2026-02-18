/**
 * WebSocket implementation of the AppMon client.
 */
class WebsocketClient extends BaseClient {
    constructor(domain, viewer, onJoined, onEstablished, onClosed, onFailed) {
        super(domain, viewer, onJoined, onEstablished, onClosed, onFailed);
        this.endpointMode = "websocket";
        this.heartbeatInterval = 5000;
        this.socket = null;
        this.heartbeatTimer = null;
        this.pendingMessages = [];
        this.established = false;
    }

    start(instancesToJoin) {
        this.openSocket(instancesToJoin);
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

    openSocket(instancesToJoin) {
        this.closeSocket(false);
        const url = new URL(this.domain.endpoint.url + "/websocket/" + this.domain.endpoint.token, location.href);
        url.protocol = url.protocol.replace("https:", "wss:").replace("http:", "ws:");

        this.socket = new WebSocket(url.href);

        this.socket.onopen = () => {
            console.log(this.domain.name, "socket connected:", this.domain.endpoint.url);
            this.pendingMessages.push("Socket connection successful");
            const options = [
                "command:join",
                "timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone
            ];
            if (instancesToJoin) {
                options.push("instancesToJoin:" + instancesToJoin);
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
                        this.domain.endpoint.token = msg.substring(5);
                        this.heartbeatPing();
                    } else {
                        this.viewer.processMessage(msg);
                    }
                } else if (msg.startsWith("joined:")) {
                    console.log(this.domain.name, msg, this.domain.endpoint.token);
                    const payload = (msg.length > 7 ? JSON.parse(msg.substring(7)) : null);
                    this.establish(payload);
                }
            }
        };

        this.socket.onclose = (event) => {
            this.closeSocket(true);
            if (this.domain.endpoint.mode === this.endpointMode) {
                if (this.onClosed) {
                    this.onClosed(this.domain);
                }
                if (event.code === 1003) {
                    console.log(this.domain.name, "socket connection refused: ", event.code);
                    this.viewer.printErrorMessage("Socket connection refused by server.");
                    return;
                }
                if (event.code === 1000 || this.retryCount === 0) {
                    console.log(this.domain.name, "socket connection closed: ", event.code);
                    this.viewer.printMessage("Socket connection closed.");
                }
                if (event.code !== 1000) {
                    this.rejoin(instancesToJoin);
                }
            }
        };

        this.socket.onerror = (event) => {
            if (this.domain.endpoint.mode === this.endpointMode) {
                console.log(this.domain.name, "websocket error:", event);
                this.viewer.printErrorMessage("Could not connect to the WebSocket server.");
            }
            if (this.onFailed) {
                this.onFailed(this.domain);
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
        this.domain.endpoint['mode'] = this.endpointMode;
        if (this.onJoined) {
            this.onJoined(this.domain, payload);
        }
        while (this.pendingMessages.length) {
            this.viewer.printMessage(this.pendingMessages.shift());
        }
        if (this.onEstablished) {
            this.onEstablished(this.domain);
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

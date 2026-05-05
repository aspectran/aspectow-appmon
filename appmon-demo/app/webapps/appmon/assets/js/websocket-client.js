/*
 * Aspectow AppMon 4.0
 * Last modified: 2026-04-29
 */

/**
 * A bridge that multiplexes multiple virtual sockets over a single physical WebSocket connection.
 */
class GatewaySocketBridge {
    constructor(url) {
        this.url = url;
        this.socket = null;
        this.virtualSockets = {};
        this.connectionPromise = null;
        this.isConnected = false;
    }

    connect() {
        if (this.connectionPromise) {
            return this.connectionPromise;
        }

        this.connectionPromise = new Promise((resolve, reject) => {
            console.log("Gateway bridge connecting to:", this.url);
            this.socket = new WebSocket(this.url);

            this.socket.onopen = () => {
                this.isConnected = true;
                resolve();
                Object.values(this.virtualSockets).forEach(vs => {
                    if (vs.onopen) vs.onopen();
                });
            };

            this.socket.onmessage = (event) => {
                if (typeof event.data === "string") {
                    const msg = event.data;
                    const idx = msg.indexOf(':');
                    if (idx !== -1) {
                        const nodeId = msg.substring(0, idx);
                        const payload = msg.substring(idx + 1);
                        const vs = this.virtualSockets[nodeId];
                        if (vs && vs.onmessage) {
                            vs.onmessage({ data: payload });
                        }
                    } else {
                        // Broadcast to all if no nodeId prefix is found (e.g. pong)
                        Object.values(this.virtualSockets).forEach(vs => {
                            if (vs.onmessage) vs.onmessage(event);
                        });
                    }
                }
            };

            this.socket.onclose = (event) => {
                this.isConnected = false;
                this.connectionPromise = null;
                Object.values(this.virtualSockets).forEach(vs => {
                    if (vs.onclose) vs.onclose(event);
                });
            };

            this.socket.onerror = (event) => {
                this.connectionPromise = null;
                if (reject) reject(event);
                Object.values(this.virtualSockets).forEach(vs => {
                    if (vs.onerror) vs.onerror(event);
                });
            };
        });

        return this.connectionPromise;
    }

    createVirtualSocket(nodeId) {
        const vs = new VirtualSocket(this, nodeId);
        this.virtualSockets[nodeId] = vs;
        if (this.isConnected && vs.onopen) {
            setTimeout(() => vs.onopen(), 0);
        }
        return vs;
    }

    send(nodeId, data) {
        if (this.isConnected && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send("[" + nodeId + "]" + data);
        }
    }

    close(nodeId) {
        delete this.virtualSockets[nodeId];
        if (Object.keys(this.virtualSockets).length === 0 && this.socket) {
            this.socket.close();
            this.socket = null;
            this.isConnected = false;
            this.connectionPromise = null;
        }
    }
}

class VirtualSocket {
    constructor(bridge, nodeId) {
        this.bridge = bridge;
        this.nodeId = nodeId;
        this.readyState = WebSocket.CONNECTING;
        
        // Define constants to mimic real WebSocket
        this.CONNECTING = WebSocket.CONNECTING;
        this.OPEN = WebSocket.OPEN;
        this.CLOSING = WebSocket.CLOSING;
        this.CLOSED = WebSocket.CLOSED;
        
        this._onopen = null;
        this._onmessage = null;
        this._onclose = null;
        this._onerror = null;
    }

    get onopen() { return this._onopen; }
    set onopen(fn) {
        this._onopen = () => {
            this.readyState = WebSocket.OPEN;
            if (fn) fn();
        };
    }

    get onmessage() { return this._onmessage; }
    set onmessage(fn) { this._onmessage = fn; }

    get onclose() { return this._onclose; }
    set onclose(fn) {
        this._onclose = (event) => {
            this.readyState = WebSocket.CLOSED;
            if (fn) fn(event);
        };
    }

    get onerror() { return this._onerror; }
    set onerror(fn) { this._onerror = fn; }

    send(data) {
        this.bridge.send(this.nodeId, data);
    }

    close() {
        this.readyState = WebSocket.CLOSED;
        this.bridge.close(this.nodeId);
    }
}

// Global instance for the gateway bridge
window.gatewaySocketBridge = null;

/**
 * WebSocket implementation of the AppMon client.
 */
class WebsocketClient extends BaseClient {
    constructor(node, viewer, onJoined, onEstablished, onClosed, onFailed, isGatewayMode = false) {
        super(node, viewer, onJoined, onEstablished, onClosed, onFailed);
        this.endpointMode = "websocket";
        this.heartbeatInterval = 5000;
        this.socket = null;
        this.heartbeatTimer = null;
        this.pendingMessages = [];
        this.established = false;
        this.isGatewayMode = isGatewayMode;
    }

    start(appsToJoin) {
        this.openSocket(appsToJoin);
    }

    stop() {
        this.closeSocket();
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

        if (this.isGatewayMode) {
            if (!window.gatewaySocketBridge) {
                window.gatewaySocketBridge = new GatewaySocketBridge(url.href);
            }
            window.gatewaySocketBridge.connect();
            this.socket = window.gatewaySocketBridge.createVirtualSocket(this.node.id);
        } else {
            this.socket = new WebSocket(url.href);
        }

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
                    const payload = msg.substring(7);
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
                if (event && event.code === 1003) {
                    console.log(this.node.id, "socket connection refused: ", event.code);
                    this.viewer.printErrorMessage("Socket connection refused by server.");
                    return;
                }
                if ((event && event.code === 1000) || this.retryCount === 0) {
                    console.log(this.node.id, "socket connection closed: ", event ? event.code : 'unknown');
                    this.viewer.printMessage("Socket connection closed.");
                }
                if (!event || event.code !== 1000) {
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
        this.clearSessionId();
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
            this.setSessionId(payload);
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
                //this.socket.send("command:ping");
            }
        }, this.heartbeatInterval);
    }
}

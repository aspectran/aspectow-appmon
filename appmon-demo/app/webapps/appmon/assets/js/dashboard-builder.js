/*
 * Copyright (c) 2020-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The builder component for the AppMon dashboard.
 * Responsible for assembling the dashboard UI based on configuration data.
 *
 * @version 4.1
 * @last-modified 2026-09-09
 */
class DashboardBuilder {
    constructor(options = {}) {
        this.options = options;
        this.settings = {};
        this.clusterMode = "direct";
        this.isGatewayMode = false;
        this.counterPersistInterval = 5;
        this.groups = [];
        this.nodes = [];
        this.apps = [];
        this.metrics = [];
        this.viewers = [];
        this.clients = [];
        this.currentGroupId = null;
        this.selectedNodeIdByGroup = {};
        this.currentAjax = null;
        this.nodeJoinedTimer = null;
    }

    build(baseUrl, appsToSubscribe, nodeToSubscribe) {
        this.baseUrl = baseUrl;
        this.appsToSubscribe = appsToSubscribe;
        this.nodeToSubscribe = nodeToSubscribe;
        this.currentGroupId = null;
        this.selectedNodeIdByGroup = {};

        if (this.currentAjax) {
            this.currentAjax.abort();
            this.currentAjax = null;
        }

        this.suspendMonitoring();
        this.clearView();
        this.currentAjax = $.ajax({
            url: baseUrl + "/appmon/config/data",
            type: "get",
            dataType: "json",
            data: {
                nodeToSubscribe: nodeToSubscribe || null,
                appsToSubscribe: appsToSubscribe || null
            },
            success: (data) => {
                this.currentAjax = null;
                if (data) {
                    if (!data.appsToSubscribe) {
                        alert("No verified apps found. Please check the configuration of the backend.");
                        return;
                    }

                    this.settings = { ...data.settings };
                    this.clusterMode = this.settings.clusterMode || "direct";
                    this.isGatewayMode = (this.settings.clusterMode === "gateway");
                    this.counterPersistInterval = this.settings.counterPersistInterval || 5;
                    this.groups = [];
                    this.nodes = [];
                    this.apps = [];
                    this.viewers = [];
                    this.clients = [];

                    let index = 0;
                    data.nodes.forEach(nodeInfo => {
                        if (this.nodeToSubscribe && this.nodeToSubscribe !== nodeInfo.id) {
                            return;
                        }
                        const node = {
                            ...nodeInfo,
                            index: index++,
                            active: true,
                            alive: false,
                            primary: false,
                            subscribed: false,
                            subscribeAttempts: 0
                        };
                        node.endpoint.mode = node.endpoint.mode || "auto";
                        node.endpoint.path = baseUrl + node.endpoint.path + "/" + node.id;
                        node.endpoint.token = data.token;
                        this.nodes.push(node);
                        this.viewers[node.index] = new DashboardViewer(this.counterPersistInterval * 60, this.options);
                        console.log(index, "node", node);
                    });

                    // Assign group-specific logical numbers to each node
                    const groupNodeCounts = {};
                    this.nodes.forEach(node => {
                        const groupId = node.group;
                        if (!groupNodeCounts[groupId]) {
                            groupNodeCounts[groupId] = 0;
                        }
                        groupNodeCounts[groupId]++;
                        node.nodeNoInGroup = groupNodeCounts[groupId];
                    });

                    if (data.groups) {
                        data.groups.forEach(groupInfo => {
                            if (this.nodes.some(node => node.group === groupInfo.id)) {
                                const group = { ...groupInfo, active: false };
                                if (data.myGroupId && groupInfo.id === data.myGroupId) {
                                    this.groups.unshift(group);
                                } else {
                                    this.groups.push(group);
                                }
                            }
                        });
                    }

                    data.apps.forEach(appInfo => {
                        const app = { ...appInfo, active: false };
                        this.apps.push(app);
                        console.log("app", app);
                    });

                    this.clearView();
                    this.buildView();
                    this.bindEvents();
                    if (this.nodes.length) {
                        this.connect(0);
                    }

                    // Select the initial group
                    if (this.groups.length > 0) {
                        let initialGroupId = null;
                        if (this.nodeToSubscribe) {
                            const targetNode = data.nodes.find(n => n.id === this.nodeToSubscribe);
                            if (targetNode && targetNode.group) {
                                initialGroupId = targetNode.group;
                            }
                        }
                        if (!initialGroupId) {
                            initialGroupId = this.groups[0].id;
                        }
                        this.changeGroup(initialGroupId);
                    }

                    if (location.hash) {
                        const appId = location.hash.substring(1);
                        const targetApp = this.apps.find(app => app.id === appId);
                        if (targetApp) {
                            if (targetApp.group && targetApp.group !== this.currentGroupId) {
                                this.changeGroup(targetApp.group);
                            }
                            this.changeApp(appId);
                        }
                    }
                }
            },
            error: (xhr, status) => {
                this.currentAjax = null;
                if (status === "abort") {
                    return;
                }
                if (xhr.status === 403) {
                    alert("Authentication has expired. You will be redirected to the main page.");
                    location.href = baseUrl;
                }
            }
        });
    }

    rebuild() {
        if (this.nodeJoinedTimer) {
            clearTimeout(this.nodeJoinedTimer);
            this.nodeJoinedTimer = null;
        }
        this.build(this.baseUrl, this.appsToSubscribe, this.nodeToSubscribe);
    }

    connect(nodeIndex) {
        const onSubscribed = (node, primary) => {
            if (node.subscribed && node.subscribeAttempts > 0) return;
            if (primary) {
                console.log("primary connection node:", node.id);
                node.primary = true;
            }
            node.subscribed = true;
            node.subscribeAttempts++;
            console.log(node.id, "subscribe attempts:", node.subscribeAttempts);
            //this.clearConsole(node.index);
            this.changeNodeState(node);
            if (node.subscribeAttempts === 1) {
                this.initView();
            } else {
                this.clearSessions(node.index);
            }
            if (node.alive) this.viewers[node.index].setEnable(true);
            if (node.alive && node.active) this.viewers[node.index].setVisible(true);
            if (node.subscribeAttempts === 1 && node.index + 1 < this.nodes.length) {
                console.log("connecting next node:", node.index + 1);
                this.connect(node.index + 1);
            }
        };

        const onClosed = (node) => {
            node.subscribed = false;
            node.alive = false;
            node.primary = false;
            this.changeNodeState(node);
            this.viewers[node.index].setEnable(false);
        };

        const onFailed = (node) => {
            this.changeNodeState(node, true);
            if (node.endpoint.mode === "polling" && node.subscribeAttempts < 1) {
                const currentClient = this.clients[node.index];
                if (currentClient && currentClient.constructor.name === "PollingClient") {
                    return;
                }
                setTimeout(() => {
                    const currentClientAsync = this.clients[node.index];
                    if (currentClientAsync && currentClientAsync.constructor.name === "PollingClient") {
                        return;
                    }
                    const viewer = this.viewers[node.index];
                    const client = new PollingClient(node, viewer, onSubscribed, onClosed, onFailed, this.isGatewayMode);
                    if (this.isGatewayMode) {
                        this.sharedClient = client;
                        client.addClusterViewer(node.id, viewer);
                        client.addClusterNode(node, onSubscribed);
                        client.onNodeJoined = onNodeJoined;
                        client.onNodeStatusChanged = onNodeStatusChanged;
                        client.onNodeLeft = onNodeLeft;
                        client.onRequireRebuild = onRequireRebuild;
                    }
                    this.viewers[node.index].setClient(client);
                    this.clients[node.index] = client;
                    client.start(this.appsToSubscribe, this.nodeToSubscribe);
                }, (node.index - 1) * 1000);
            }
        };

        const onNodeJoined = (node) => {
            this.groups.forEach(group => {
                if (group.id === node.group) {
                    if (this.nodeJoinedTimer) {
                        clearTimeout(this.nodeJoinedTimer);
                    }
                    this.nodeJoinedTimer = setTimeout(() => {
                        this.nodeJoinedTimer = null;
                        this.showNewNodeNotification(node.id);
                    }, 3000);
                }
            });
        };

        const onNodeStatusChanged = (node) => {
            const existing = this.nodes.find(n => n.id === node.id);
            if (existing) {
                existing.status = node.status;
                this.changeNodeState(existing);
            }
        };

        const onNodeLeft = (nodeId) => {
            const node = this.nodes.find(n => n.id === nodeId);
            if (node) {
                node.subscribed = false;
                node.alive = false;
                this.changeNodeState(node);
                this.viewers[node.index].setEnable(false);
                if (!node.primary) {
                    this.viewers[node.index].printErrorMessage("Node " + nodeId + " is left");
                }
            }
        };

        const onRequireRebuild = () => {
            this.rebuild();
        };

        const node = this.nodes[nodeIndex];
        if (nodeIndex === 0) {
            console.log("cluster mode:", this.clusterMode);
            console.log("endpoint mode:", node.endpoint.mode);
        }
        console.log("connecting node:", nodeIndex);

        if (node.subscribed) return;
        const viewer = this.viewers[nodeIndex];

        if (this.isGatewayMode && this.sharedClient) {
            this.sharedClient.addClusterViewer(node.id, viewer);
            this.sharedClient.addClusterNode(node, onSubscribed);
            viewer.setClient(this.sharedClient);
            this.clients[node.index] = this.sharedClient;
            this.sharedClient.connect(node.id);
            return;
        }

        let client;
        if (node.endpoint.mode === "polling") {
            client = new PollingClient(node, viewer, onSubscribed, onClosed, onFailed, this.isGatewayMode);
        } else {
            client = new WebsocketClient(node, viewer, onSubscribed, onClosed, onFailed, this.isGatewayMode);
        }
        if (this.isGatewayMode) {
            this.sharedClient = client;
            client.addClusterViewer(node.id, viewer);
            client.addClusterNode(node, onSubscribed);
            client.onNodeJoined = onNodeJoined;
            client.onNodeStatusChanged = onNodeStatusChanged;
            client.onNodeLeft = onNodeLeft;
            client.onRequireRebuild = onRequireRebuild;
        }
        viewer.setClient(client);
        this.clients[node.index] = client;
        client.start(this.appsToSubscribe, this.nodeToSubscribe);
    }

    showNewNodeNotification(nodeId) {
        const $notification = $("#new-node-notification");
        if ($notification.length > 0) {
            $notification.find(".node-id").text(nodeId);
            $notification.find(".refresh-btn").off("click").on("click", () => {
                $notification.hide();
                this.rebuild();
            });
            $notification.fadeIn();
        } else {
            const result = confirm("A new node '" + nodeId + "' has joined the cluster. Would you like to refresh the dashboard?");
            if (result) {
                this.rebuild();
            }
        }
    }

    changeNode(nodeIndex) {
        const availableTabs = $(".node.tabs .tabs-title.available");
        if (availableTabs.length <= 1) return;

        const node = this.nodes[nodeIndex];
        const wasActive = node.active;

        // Reset all nodes in the current group
        this.nodes.forEach(n => {
            if (n.group === this.currentGroupId) {
                n.active = false;
            }
        });

        // Toggle or exclusively activate
        if (!wasActive) {
            node.active = true;
            this.selectedNodeIdByGroup[this.currentGroupId] = node.id;
        } else {
            delete this.selectedNodeIdByGroup[this.currentGroupId];
        }

        this.nodes.forEach(n => {
            if (n.group === this.currentGroupId) {
                this.showNode(n);
            }
        });
        this.updateNodeTabs();

        if (this.isGatewayMode) {
            const activeApp = this.apps.find(a => a.active);
            if (activeApp) {
                const targetNodeId = (node.active ? node.id : null);
                this.nodes.forEach(n => {
                    if (n.primary) {
                        const client = this.clients[n.index];
                        if (client && client.focus) client.focus(activeApp.id, targetNodeId);
                    }
                });
            }
        }
    }

    showNode(node) {
        this.apps.forEach(app => {
            if (app.active) {
                this.updateNodeVisibility(node, app.id);
            }
        });
    }

    updateNodeTabs() {
        const availableTabs = $(`.node.tabs .tabs-title[data-group-id=${this.currentGroupId}]`);
        availableTabs.removeClass("active");
        this.nodes.filter(d => d.active && d.group === this.currentGroupId).forEach(d => {
            $(".node.tabs .tabs-title[data-node-index=" + d.index + "]").addClass("active");
        })
    }

    updateNodeVisibility(node, appId) {
        const activeNodesInGroup = this.nodes.filter(n => n.group === this.currentGroupId && n.active);
        const isVisible = (node.group === this.currentGroupId && (activeNodesInGroup.length === 0 || node.active));
        const action = isVisible ? "show" : "hide";

        const selector = `[data-node-index=${node.index}][data-app-id=${appId}]`;
        const otherSelector = `[data-node-index=${node.index}][data-app-id!=${appId}]`;

        $(`.event-box${otherSelector}, .visual-box${otherSelector}, .console-box${otherSelector}`).hide();
        $(`.event-box${selector}, .visual-box${selector}, .console-box${selector}`)[action]();

        this.viewers[node.index].setVisible(isVisible);
        if (isVisible) {
            $(`.track-box[data-node-index=${node.index}] .bullet`).remove();
            $(`.console-box${selector}`).each((_, el) => {
                const $console = $(el).find(".console");
                if (!$console.data("pause")) {
                    this.viewers[node.index].refreshConsole($console);
                }
            });
            $(`.node.metrics-bar[data-node-index=${node.index}]`).show();
        } else {
            $(`.node.metrics-bar[data-node-index=${node.index}]`).hide();
        }
    }

    changeNodeState(node, errorOccurred) {
        const $indicator = $(`.node.tabs .tabs-title[data-node-index=${node.index}] .indicator`);
        $indicator.removeClass($indicator.data("icon-connected") + " connected " +
                           $indicator.data("icon-disconnected") + " disconnected " +
                           $indicator.data("icon-error") + " error");
        if (errorOccurred) {
            $indicator.addClass($indicator.data("icon-error") + " error");
        } else if (node.subscribed && node.alive) {
            $indicator.addClass($indicator.data("icon-connected") + " connected");
        } else {
            $indicator.addClass($indicator.data("icon-disconnected") + " disconnected");
        }
    }

    changeGroup(groupId) {
        if (this.currentGroupId === groupId) return;
        this.currentGroupId = groupId;

        this.groups.forEach(group => {
            const $tabTitle = $(".group.tabs .tabs-title[data-group-id=" + group.id + "]");
            if (group.id === groupId) {
                group.active = true;
                $tabTitle.addClass("active");
            } else {
                group.active = false;
                $tabTitle.removeClass("active");
            }
        });

        // Filter Node Tabs
        let nodeCount = 0;
        const selectedNodeId = this.selectedNodeIdByGroup[groupId];
        this.nodes.forEach(node => {
            const $tab = $(".node.tabs .tabs-title[data-node-index=" + node.index + "]");
            if (!groupId || node.group === groupId) $tab.show(); else $tab.hide();
            if (selectedNodeId) {
                node.active = (node.id === selectedNodeId);
            } else {
                node.active = false; // Start with no nodes explicitly active
            }
            if (node.group === groupId) nodeCount++;
        });

        if (!selectedNodeId && nodeCount === 1) {
            this.nodes.forEach(node => {
                if (node.group === groupId) node.active = true;
            });
        }

        // Filter App Tabs
        this.apps.forEach(app => {
            const $tab = $(".app.tabs .tabs-title[data-app-id=" + app.id + "]");
            if (!groupId || !app.group || app.group === groupId) $tab.show(); else $tab.hide();
        });

        // Select first available app in the new group context
        const firstAvailableApp = this.apps.find(app => {
            return !groupId || !app.group || app.group === groupId;
        });

        this.changeApp(firstAvailableApp ? firstAvailableApp.id : null);
        this.updateNodeTabs();
    }

    changeApp(appId) {
        let exists = false;
        this.apps.forEach(app => {
            if (!appId) appId = app.id;
            const $tabTitle = $(".app.tabs .tabs-title[data-app-id=" + app.id + "]");
            if (app.id === appId) {
                app.active = true;
                setTimeout(() => this.showNodeApp(appId), 0);
                $tabTitle.addClass("active");
                exists = true;
                this.nodes.forEach(node => {
                    if (node.primary) {
                        const client = this.clients[node.index];
                        if (client && client.focus) {
                            setTimeout(() => client.focus(appId, node.id), 10);
                        }
                    }
                });
            } else {
                app.active = false;
                $tabTitle.removeClass("active");
            }
        });
        if (!exists && appId) return this.changeApp();
        return appId;
    }

    showNodeApp(appId) {
        $(".control-bar[data-app-id!=" + appId + "]").hide();
        $(".control-bar[data-app-id=" + appId + "]").show();
        this.nodes.forEach(node => {
            this.updateNodeVisibility(node, appId);
        });
        this.updateNodeTabs();
    }

    initView() {
        if (this.groups.length) $(".group-bar").show();
        $(".speed-options").addClass("hide");
        if (this.nodes.some(d => d.endpoint.mode === "polling")) {
            $(".speed-options").removeClass("hide");
        }
        this.apps.forEach(app => {
            const $eventBox = $(`.event-box[data-app-id=${app.id}]`);
            const $visualBox = $(`.visual-box[data-app-id=${app.id}]`);
            if ($eventBox.length && $visualBox.length && $eventBox.find(".session-box.available").length === 0) {
                $eventBox.removeClass("col-lg-6").addClass("fixed-layout");
                $visualBox.removeClass("col-lg-6").addClass("fixed-layout");
            }
        });
    }

    bindEvents() {
        $(".group.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const groupId = $(e.currentTarget).closest(".tabs-title").data("group-id");
            this.changeGroup(groupId);
        });
        $(".node.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const nodeIndex = $(e.currentTarget).closest(".tabs-title").data("node-index");
            this.changeNode(nodeIndex);
        });
        $(".app.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const appId = $(e.currentTarget).closest(".tabs-title").data("app-id");
            this.changeApp(appId);
        });
        $(".layout-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const appId = $btn.closest(".control-bar").data("app-id");
            const isCompact = $btn.hasClass("compact");
            if (!$btn.hasClass("on")) {
                if (isCompact) {
                    $btn.addClass("on");
                    $(`.event-box.available:not(.fixed-layout)[data-app-id=${appId}], 
                       .visual-box.available:not(.fixed-layout)[data-app-id=${appId}], 
                       .console-box.available[data-app-id=${appId}]`).addClass("col-lg-6");
                }
            } else if (isCompact) {
                $btn.removeClass("on");
                $(`.event-box.available:not(.fixed-layout)[data-app-id=${appId}], 
                   .visual-box.available:not(.fixed-layout)[data-app-id=${appId}], 
                   .console-box.available[data-app-id=${appId}]`).removeClass("col-lg-6");
            }
            this.viewers.forEach(v => v.updateCanvasWidth());
            this.refreshData(appId, false);
        });
        $(".date-unit-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $controlBar = $btn.closest(".control-bar");
            const appId = $controlBar.data("app-id");
            const unit = $btn.data("unit") || "";
            $btn.parent().data("unit", unit).find(".btn").removeClass("on");
            $btn.addClass("on");
            $controlBar.find(".date-offset-options").data("offset", "").find(".btn.current").removeClass("on");
            this.viewers.forEach(v => v.updateCanvasWidth());
            this.refreshData(appId, false);
        });
        $(".date-offset-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $controlBar = $btn.closest(".control-bar");
            const appId = $controlBar.data("app-id");
            const offset = $btn.data("offset") || "";
            const $parent = $btn.parent();
            if (offset !== "current") {
                $parent.find(".btn.current").addClass("on");
            } else {
                $parent.find(".btn").addClass("on");
                $parent.find(".btn.current").removeClass("on");
            }
            $parent.data("offset", offset);
            this.refreshData(appId, false, offset);
        });
        $(".speed-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const faster = !$btn.hasClass("on");
            $btn.toggleClass("on", faster);
            this.nodes.forEach(node => {
                if (node.endpoint.mode === "polling") {
                    this.clients[node.index].changePollingInterval(faster ? 1 : 0);
                }
            });
        });
        $(".open-popup").off("click").on("click", (e) => {
            let url = this.baseUrl + "/appmon/dashboard/popup/" + (this.appsToSubscribe || "");
            if (this.nodeToSubscribe) {
                url += "?nodeId=" + encodeURIComponent(this.nodeToSubscribe);
            }
            const name = "appmon_dashboard_popup";
            const features = "width=1500,height=1070,menubar=no,toolbar=no,location=no,status=no,resizable=yes,scrollbars=yes";
            const popup = window.open(url, name, features);
            if (popup) {
                this.suspendMonitoring();
                this.showPopupModeMessage();
                popup.focus();
            }
        });
        $(document).off("click", ".session-box .panel.status .knob-bar")
            .on("click", ".session-box .panel.status .knob-bar", function() {
                $(this).parent().toggleClass("expanded");
            });
        $(document).off("click", ".session-box ul.sessions li")
            .on("click", ".session-box ul.sessions li", function() {
                $(this).toggleClass("designated");
            });
        $(".console-box .tailing-switch").off("click").on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $consoleBox = $btn.closest(".console-box");
            const $console = $consoleBox.find(".console");
            const nodeIndex = $consoleBox.data("node-index");
            const isTailing = !!$console.data("tailing");
            const newTailingState = !isTailing;

            $console.data("tailing", newTailingState);
            $consoleBox.find(".tailing-status").toggleClass("on", newTailingState);
            $btn.attr("title", newTailingState ? $btn.data("title-on") : $btn.data("title-off"));

            if (newTailingState) {
                this.viewers[nodeIndex].refreshConsole($console);
            }
        });
        $(".console-box .pause-switch").off("click").on("click", function() {
            const $btn = $(this);
            const $icon = $btn.find(".icon");
            const $console = $btn.closest(".console-box").find(".console");
            const isPause = !!$console.data("pause");
            const newPauseState = !isPause;

            $console.data("pause", newPauseState);
            $btn.toggleClass("on", newPauseState);

            if (newPauseState) {
                $btn.attr("title", $btn.data("title-resume"));
                $icon.removeClass($icon.data("icon-pause")).addClass($icon.data("icon-resume"));
            } else {
                $btn.attr("title", $btn.data("title-pause"));
                $icon.removeClass($icon.data("icon-resume")).addClass($icon.data("icon-pause"));
            }
        });
        $(".console-box .expand-switch").off("click").on("click", function() {
            const $btn = $(this);
            const $icon = $btn.find(".icon");
            const $consoleBox = $btn.closest(".console-box");
            const isMaximized = $consoleBox.hasClass("maximized");
            const newMaximizedState = !isMaximized;

            $consoleBox.toggleClass("maximized", newMaximizedState);
            $btn.toggleClass("on", newMaximizedState);

            if (newMaximizedState) {
                $btn.attr("title", $btn.data("title-compress"));
                $icon.removeClass($icon.data("icon-expand")).addClass($icon.data("icon-compress"));
                $("body").css("overflow", "hidden");
            } else {
                $btn.attr("title", $btn.data("title-expand"));
                $icon.removeClass($icon.data("icon-compress")).addClass($icon.data("icon-expand"));
                $("body").css("overflow", "");
            }
        });
        $(".console-box .clear-screen").off("click").on("click", (e) => {
            const $consoleBox = $(e.currentTarget).closest(".console-box");
            this.viewers[$consoleBox.data("node-index")].clearConsole($consoleBox.find(".console"));
        });
        $(".console-box .console").off("scroll").on("scroll", (e) => {
            const $console = $(e.currentTarget);
            const $consoleBox = $console.closest(".console-box");
            if ($console.scrollTop() === 0) {
                $consoleBox.find(".load-previous").fadeIn();
            } else {
                $consoleBox.find(".load-previous").fadeOut();
            }
        });
        $(".console-box .load-previous").off("click").on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $consoleBox = $btn.closest(".console-box");
            const $console = $consoleBox.find(".console");
            const nodeIndex = $consoleBox.data("node-index");
            const appId = $consoleBox.data("app-id");
            const logId = $consoleBox.data("log-id");
            const loadedLines = $console.find("p").length;

            if ($console.data("tailing")) {
                $console.data("tailing", false);
                const $tailingSwitch = $consoleBox.find(".tailing-switch");
                $consoleBox.find(".tailing-status").removeClass("on");
                $tailingSwitch.attr("title", $tailingSwitch.data("title-off"));
            }

            this.clients[nodeIndex].loadPrevious(appId, logId, loadedLines, this.nodes[nodeIndex].id);
        });
        $(window).off("resize").on("resize", () => {
            this.viewers.forEach(v => v.updateCanvasWidth());
        });
        $(document).off("visibilitychange").on("visibilitychange", () => {
            if (!document.hidden) {
                this.viewers.forEach(v => {
                    v.resetCurrentActivityCounts();
                });
                this.apps.forEach(app => {
                    if (this.nodeToSubscribe || !app.hidden) {
                        this.refreshData(app.id, true);
                    }
                });
            }
        });
        $(document).off("click.metricPopover", ".metrics-bar .metric.available")
            .on("click.metricPopover", ".metrics-bar .metric.available", (e) => {
                e.stopPropagation();
                const $metric = $(e.currentTarget);
                const nodeIndex = $metric.data("node-index");
                const exporterKey = $metric.data("exporter-key");
                if (nodeIndex !== undefined && this.viewers[nodeIndex]) {
                    this.viewers.forEach((v, idx) => {
                        if (idx !== nodeIndex) v.hideMetricPopover();
                    });
                    this.viewers[nodeIndex].toggleMetricPopover(exporterKey, $metric);
                }
            });
        $(document).off("click.metricPopoverClose", "#metric-popover .btn-close-popover")
            .on("click.metricPopoverClose", "#metric-popover .btn-close-popover", (e) => {
                e.stopPropagation();
                this.viewers.forEach(v => v.hideMetricPopover());
            });
        $(document).off("click.metricPopoverOutside")
            .on("click.metricPopoverOutside", (e) => {
                if (!$(e.target).closest("#metric-popover, .metrics-bar .metric.available").length) {
                    this.viewers.forEach(v => v.hideMetricPopover());
                }
            });
        $(document).off("keydown.metricPopover")
            .on("keydown.metricPopover", (e) => {
                if (e.key === "Escape") {
                    this.viewers.forEach(v => v.hideMetricPopover());
                }
            });
    }

    refreshData(appId, withLogs, dateOffset) {
        const options = ["appId:" + appId];
        if (withLogs) options.push("withLogs:true");
        const dateUnit = $(".control-bar[data-app-id=" + appId + "] .date-unit-options").data("unit");
        if (dateUnit) options.push("dateUnit:" + dateUnit);
        if (dateOffset === "previous") {
            let maxStartDate = "";
            this.viewers.forEach(v => {
                const startDate = v.getMaxStartDatetime(appId);
                if (startDate > maxStartDate) maxStartDate = startDate;
            });
            if (maxStartDate) {
                options.push("dateOffset:" + maxStartDate);
            } else {
                $(".control-bar[data-app-id=" + appId + "] .date-offset-options .btn.previous").removeClass("on");
                return;
            }
        }
        setTimeout(() => {
            const activeNodesInGroup = this.nodes.filter(n => n.group === this.currentGroupId && n.active);
            this.nodes.forEach(node => {
                console.log("Refreshing node:", node);
                const isVisible = (node.group === this.currentGroupId && (activeNodesInGroup.length === 0 || node.active));
                if (isVisible && node.alive) {
                    this.viewers[node.index].setLoading(appId, true);
                    if (withLogs) this.clearConsole(node.index);
                    this.clients[node.index].refresh(options, node.id);
                }
            });
        }, 50);
    }

    suspendMonitoring() {
        if (this.nodeJoinedTimer) {
            clearTimeout(this.nodeJoinedTimer);
            this.nodeJoinedTimer = null;
        }
        this.clients.forEach(client => {
            if (client) client.stop();
        });
        this.viewers.forEach(viewer => {
            if (viewer) viewer.setEnable(false);
        });
        this.sharedClient = null;
    }

    showPopupModeMessage() {
        this.clearView();
        const $container = $("#content-area > .container-fluid");
        $container.find(".row, .tabs, .control-bar, .console-box").hide();
        const $messageBox = $("#appmon-popup-message");
        if ($messageBox.length > 0) {
            $messageBox.find(".resume-here").off("click").on("click", () => {
                location.reload();
            });
            $messageBox.show();
        }
    }

    clearView() {
        $("#appmon-popup-message").hide();
        $(".group.tabs .tabs-title.available, .node.tabs .tabs-title.available, .app.tabs .tabs-title.available, " +
          ".node.metrics-bar.available, .node.metrics-bar .metric.available, .control-bar.available, " +
          ".event-box.available, .visual-box.available, .chart-box.available, .console-box.available").remove();
        $(".group.tabs .tabs-title:not(.available), .node.tabs .tabs-title:not(.available), .app.tabs .tabs-title:not(.available), " +
          ".node.metrics-bar:not(.available), .console-box:not(.available)").hide();
    }

    clearConsole(nodeIndex) {
        $(`.console-box[data-node-index=${nodeIndex}] .console`).empty();
    }

    clearSessions(nodeIndex) {
        $(`.session-box[data-node-index=${nodeIndex}] .sessions`).empty();
    }

    buildView() {
        if (this.groups.length > 0) {
            $(".group-bar").show();
            this.groups.forEach(group => {
                const $groupTab = this.addGroupTab(group);
                const $groupIndicator = $groupTab.find(".indicator");
                this.nodes.forEach(node => {
                    if (node.group === group.id) {
                        this.viewers[node.index].putIndicator$("group", "event", "", $groupIndicator);
                    }
                })
            });
        } else {
            $(".group-bar").hide();
        }
        this.nodes.forEach(node => {
            const $nodeTab = this.addNodeTab(node);
            this.viewers[node.index].putIndicator$("node", "event", "", $nodeTab.find(".indicator"));
            this.addNodeMetricsBar(node);
        });
        this.apps.forEach(app => {
            const $appTab = this.addAppTab(app);
            const $appIndicator = $appTab.find(".indicator");
            this.addControlBar(app);
            this.nodes.forEach(node => {
                if (!app.group || app.group === node.group) {
                    const viewer = this.viewers[node.index];
                    viewer.putIndicator$("app", "event", app.id, $appIndicator);
                    if (app.events && app.events.length) {
                        const $eventBox = this.addEventBox(node, app);
                        app.events.forEach(event => {
                            if (event.id === "activity") {
                                const $trackBox = this.addTrackBox($eventBox, node, app, event);
                                viewer.putDisplay$(app.id, event.id, $trackBox);
                                viewer.putIndicator$(app.id, "event", event.id, $trackBox.find(".activity-status"));
                            } else if (event.id === "session") {
                                viewer.putDisplay$(app.id, event.id, this.addSessionBox($eventBox, node, app, event));
                            }
                        });
                        const $visualBox = this.addVisualBox(node, app);
                        app.events.forEach(event => {
                            if (event.id === "activity" || event.id === "session") {
                                viewer.putChart$(app.id, event.id, this.addChartBox($visualBox, node, app, event).find(".chart"));
                            }
                        });
                    }
                    if (app.metrics && app.metrics.length) {
                        const $eventBox = $(`.event-box[data-node-index=${node.index}][data-app-id=${app.id}]`);
                        app.metrics.forEach(metric => {
                            const $metric = (metric.heading || !$eventBox.length) ? 
                                this.addNodeMetric(node, metric) :
                                this.addAppMetric(node, app, metric);
                            $metric.data("exporter-key", app.id + ":metric:" + metric.id);
                            viewer.putMetric$(app.id, metric.id, $metric);
                        });
                    }
                    if (app.logs) {
                        app.logs.forEach(logInfo => {
                            const $consoleBox = this.addConsoleBox(node, app, logInfo);
                            const $console = $consoleBox.find(".console").data("tailing", true);
                            $consoleBox.find(".tailing-status").addClass("on");
                            viewer.putConsole$(app.id, logInfo.id, $console);
                            viewer.putIndicator$(app.id, "log", logInfo.id, $consoleBox.find(".status-bar"));
                        });
                    }
                }
            });
        });
        this.changeApp();
    }

    addGroupTab(groupInfo) {
        const $tabs = $(".group.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-group-id": groupInfo.id, "title": groupInfo.description });
        $tab.find("a .title").text(" " + (groupInfo.title || groupInfo.id) + " ");
        return $tab.show().appendTo($tabs);
    }

    addNodeTab(nodeInfo) {
        const $tabs = $(".node.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-node-id": nodeInfo.id , "data-group-id": nodeInfo.group });
        $tab.find("a .title").text(" " + (nodeInfo.title || nodeInfo.id) + " ");
        const nodesInGroup = this.nodes.filter(n => n.group === nodeInfo.group);
        if (nodesInGroup.length > 1) {
            $tab.find(".number").text(" " + nodeInfo.nodeNoInGroup);
        } else {
            $tab.find(".number").empty();
        }
        return $tab.show().appendTo($tabs);
    }

    addAppTab(appInfo) {
        const $tabs = $(".app.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-app-id": appInfo.id, "data-group-id": appInfo.group, "title": appInfo.title });
        $tab.find("a .title").text(" " + appInfo.title + " ");
        return $tab.show().appendTo($tabs);
    }

    addNodeMetricsBar(nodeInfo) {
        const $metricsBar = $(".node.metrics-bar");
        const $newBar = $metricsBar.first().hide().clone().addClass("available").attr("data-node-index", nodeInfo.index);
        $newBar.find(".number").text(" " + nodeInfo.nodeNoInGroup);
        return $newBar.insertAfter($metricsBar.last());
    }

    addNodeMetric(nodeInfo, metricInfo) {
        const $bar = $(`.node.metrics-bar[data-node-index=${nodeInfo.index}]`).show();
        const $container = $bar.find(".node-metrics");
        const $metric = $container.find(".metric").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-metric-id": metricInfo.id });
        $metric.find("dt .name").text(metricInfo.title).attr("title", metricInfo.description);
        if (metricInfo.unit) {
            $metric.find(".unit").text(metricInfo.unit);
        }
        return $metric.appendTo($container).show();
    }

    addAppMetric(nodeInfo, appInfo, metricInfo) {
        const $bar = $(`.node.metrics-bar[data-node-index=${nodeInfo.index}]`).show();
        const $container = $bar.find(".app-metrics");
        const $metric = $container.find(".metric").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-metric-id": metricInfo.id });
        $metric.find("dt .name").text(metricInfo.title).attr("title", metricInfo.description);
        if (metricInfo.unit) {
            $metric.find(".unit").text(metricInfo.unit);
        }
        return $metric.appendTo($container).show();
    }

    addControlBar(appInfo) {
        const $bar = $(".control-bar");
        const $newBar = $bar.first().hide().clone().addClass("available").attr("data-app-id", appInfo.id);
        $newBar.find(".btn.default").text(this.counterPersistInterval + "min.");
        return $newBar.insertAfter($bar.last());
    }

    addEventBox(nodeInfo, appInfo) {
        const $box = $(".event-box").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id });
        const $titleBar = $box.find(".title-bar");
        $titleBar.find("h4").text(nodeInfo.title || nodeInfo.id);

        const nodesInGroup = this.nodes.filter(n => n.group === nodeInfo.group);
        if (nodesInGroup.length > 1) {
            $titleBar.find(".number").text(" " + nodeInfo.nodeNoInGroup);
        } else {
            $titleBar.find(".number").empty();
        }
        return $box.insertBefore($(".console-box").first());
    }

    addTrackBox($eventBox, nodeInfo, appInfo, eventInfo) {
        const $track = $eventBox.find(".track-box");
        return $track.first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .insertAfter($track.last()).show();
    }

    addSessionBox($eventBox, nodeInfo, appInfo, eventInfo) {
        const $session = $eventBox.find(".session-box");
        return $session.first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .insertAfter($session.last()).show();
    }

    addVisualBox(nodeInfo, appInfo) {
        return $(".visual-box").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id })
            .insertBefore($(".console-box").first()).show();
    }

    addChartBox($visualBox, nodeInfo, appInfo, eventInfo) {
        const $chart = $visualBox.find(".chart-box");
        return $chart.first().hide().clone().addClass("available col-12 col-lg-6")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .appendTo($visualBox).show();
    }

    addConsoleBox(nodeInfo, appInfo, logInfo) {
        const $console = $(".console-box");
        const $newBox = $console.first().hide().clone().addClass("available col-lg-6")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-log-id": logInfo.id });
        $newBox.find(".status-bar h4").text((nodeInfo.title || nodeInfo.id) + " ›› " + logInfo.file);
        return $newBox.insertAfter($console.last());
    }
}

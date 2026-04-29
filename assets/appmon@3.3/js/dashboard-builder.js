/*
 * Aspectow AppMon 3.3
 * Last modified: 2026-03-20
 */

/**
 * The builder component for the AppMon dashboard.
 * Responsible for assembling the dashboard UI based on configuration data.
 */
class DashboardBuilder {
    constructor(options = {}) {
        this.options = options;
        this.settings = {};
        this.nodes = [];
        this.apps = [];
        this.metrics = [];
        this.viewers = [];
        this.clients = [];
    }

    build(basePath, appsToJoin, nodeIdToJoin) {
        this.basePath = basePath;
        this.appsToJoin = appsToJoin;
        this.nodeIdToJoin = nodeIdToJoin;
        this.clearView();
        $.ajax({
            url: basePath + "/appmon/config/data",
            type: "get",
            dataType: "json",
            data: appsToJoin ? { apps: appsToJoin } : null,
            success: (data) => {
                if (data) {
                    this.settings = { ...data.settings };
                    this.nodes = [];
                    this.apps = [];
                    this.viewers = [];
                    this.clients = [];

                    let index = 0;
                    const random1000 = this.random(1, 1000);

                    data.nodes.forEach(nodeData => {
                        if (this.nodeIdToJoin && this.nodeIdToJoin !== nodeData.id) {
                            return;
                        }
                        console.log("nodeData", nodeData);
                        const node = {
                            ...nodeData,
                            index: index++,
                            random1000: random1000,
                            active: true,
                            client: { established: false, establishCount: 0 }
                        };
                        node.endpoint.path = basePath + node.endpoint.path + "/" + node.id;
                        node.endpoint.token = data.token;
                        this.nodes.push(node);
                        this.viewers[node.index] = new DashboardViewer(this.settings.counterPersistInterval * 60, this.options);
                        console.log("node", node);
                    });

                    data.apps.forEach(appData => {
                        const app = { ...appData, active: false };
                        this.apps.push(app);
                        console.log("app", app);
                    });

                    this.buildView();
                    this.bindEvents();
                    if (this.nodes.length) {
                        this.establish(0, appsToJoin);
                    }
                }
            },
            error: (xhr) => {
                if (xhr.status === 403) {
                    alert("Authentication has expired. You will be redirected to the main page.");
                    location.href = (typeof contextPath !== 'undefined' && contextPath ? contextPath : "/");
                }
            }
        });
    }

    random(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    }

    establish(nodeIndex, appsToJoin) {
        const node = this.nodes[nodeIndex];
        const viewer = this.viewers[nodeIndex];

        const onJoined = (node, payload) => {
            this.clearConsole(node.index);
            if (payload && payload.messages) {
                payload.messages.forEach(msg => viewer.processMessage(msg));
            }
        };

        const onEstablished = (node) => {
            node.client.established = true;
            node.client.establishCount++;
            console.log(node.id, "connection established:", node.client.establishCount);
            this.changeNodeState(node);
            viewer.setEnable(true);
            if (node.active) {
                viewer.setVisible(true);
            }
            if (node.client.establishCount === 1) {
                this.initView();
            } else {
                this.clearSessions(node.index);
            }
            if (node.client.establishCount + node.index < this.nodes.length) {
                this.establish(node.index + 1, appsToJoin);
            }
        };

        const onClosed = (node) => {
            node.client.established = false;
            this.changeNodeState(node);
            viewer.setEnable(false);
        };

        const onFailed = (node) => {
            this.changeNodeState(node, true);
            if (node.endpoint.mode !== "websocket") {
                setTimeout(() => {
                    const client = new PollingClient(node, viewer, onJoined, onEstablished, onClosed, onFailed);
                    this.clients[node.index] = client;
                    client.start(appsToJoin);
                }, (node.index - 1) * 1000);
            }
        };

        console.log("establishing", nodeIndex);
        let client;
        if (node.endpoint.mode === "polling") {
            client = new PollingClient(node, viewer, onJoined, onEstablished, onClosed, onFailed);
        } else {
            client = new WebsocketClient(node, viewer, onJoined, onEstablished, onClosed, onFailed);
        }
        viewer.setClient(client);
        this.clients[nodeIndex] = client;
        client.start(appsToJoin);
    }

    changeNode(nodeIndex) {
        const availableTabs = $(".node.tabs .tabs-title.available");
        if (availableTabs.length <= 1) return;

        const activeTabs = availableTabs.filter(".active");
        const node = this.nodes[nodeIndex];

        if (activeTabs.length === 0) {
            this.nodes.forEach(d => { if (d.active) { d.active = false; this.showNode(d); } });
            node.active = true;
            this.showNode(node);
        } else if (activeTabs.length === 1 && node.active) {
            this.nodes.forEach(d => { if (d.index !== node.index) { d.active = true; this.showNode(d); } });
        } else if (activeTabs.length === 1 && !node.active) {
            this.nodes.forEach(d => { if (d.index !== node.index) { d.active = false; this.showNode(d); } });
            node.active = true;
            this.showNode(node);
        } else {
            node.active = !node.active;
            this.showNode(node);
        }

        this.updateNodeTabs();
    }

    showNode(node) {
        this.apps.forEach(app => {
            if (app.active) {
                this.updateNodeVisibility(node, app.id);
            }
        });
    }

    updateNodeTabs() {
        const availableTabs = $(".node.tabs .tabs-title.available");
        const activeCount = this.nodes.filter(d => d.active).length;
        availableTabs.removeClass("active");
        if (availableTabs.length > activeCount) {
            this.nodes.forEach(d => {
                if (d.active) $(".node.tabs .tabs-title[data-node-index=" + d.index + "]").addClass("active");
            });
        }
        $(".node.metrics-bar.available").toggleClass("full-width", (this.nodes.length === 1 || this.nodes.length !== activeCount));
    }

    updateNodeVisibility(node, appId) {
        const action = node.active ? "show" : "hide";
        const selector = `[data-node-index=${node.index}][data-app-id=${appId}]`;
        const otherSelector = `[data-node-index=${node.index}][data-app-id!=${appId}]`;

        $(`.event-box${otherSelector}, .visual-box${otherSelector}, .console-box${otherSelector}`).hide();
        $(`.event-box${selector}, .visual-box${selector}, .console-box${selector}`)[action]();

        this.viewers[node.index].setVisible(node.active);
        if (node.active) {
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
        } else if (node.client.established) {
            $indicator.addClass($indicator.data("icon-connected") + " connected");
        } else {
            $indicator.addClass($indicator.data("icon-disconnected") + " disconnected");
        }
    }

    changeInstance(appId) {
        let exists = false;
        this.apps.forEach(app => {
            if (!appId) appId = app.id;
            const $tabTitle = $(".app.tabs .tabs-title[data-app-id=" + app.id + "]");
            if (app.id === appId) {
                app.active = true;
                this.showNodeInstance(appId);
                $tabTitle.addClass("active");
                exists = true;
            } else {
                app.active = false;
                $tabTitle.removeClass("active");
            }
        });
        if (!exists && appId) return this.changeInstance();
        return appId;
    }

    showNodeInstance(appId) {
        $(".control-bar[data-app-id!=" + appId + "]").hide();
        $(".control-bar[data-app-id=" + appId + "]").show();
        this.nodes.forEach(node => {
            this.updateNodeVisibility(node, appId);
        });
        this.updateNodeTabs();
    }

    initView() {
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
        $(".node.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const nodeIndex = $(e.currentTarget).closest(".tabs-title").data("node-index");
            this.changeNode(nodeIndex);
        });
        $(".app.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const appId = $(e.currentTarget).closest(".tabs-title").data("app-id");
            this.changeInstance(appId);
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
            this.refreshData(appId);
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
            this.refreshData(appId);
        });
        $(".date-offset-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $controlBar = $btn.closest(".control-bar");
            const appId = $controlBar.data("app-id");
            const offset = $btn.data("offset") || "";
            const $parent = $btn.parent();
            if (offset !== "current") $parent.find(".btn.current").addClass("on");
            else {
                $parent.find(".btn").addClass("on");
                $parent.find(".btn.current").removeClass("on");
            }
            $parent.data("offset", offset);
            this.refreshData(appId, offset);
        });
        $(".speed-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const faster = !$btn.hasClass("on");
            $btn.toggleClass("on", faster);
            this.nodes.forEach(node => {
                if (node.endpoint.mode === "polling") {
                    this.clients[node.index].speed(faster ? 1 : 0);
                }
            });
        });
        $(".open-popup").off("click").on("click", (e) => {
            const url = this.basePath + "/appmon/dashboard/popup/" + (this.appsToJoin || "");
            const name = "appmon_dashboard_popup";
            const features = "width=1200,height=800,menubar=no,toolbar=no,location=no,status=no,resizable=yes,scrollbars=yes";
            const popup = window.open(url, name, features);
            if (popup) {
                this.suspendMonitoring();
                this.showPopupModeMessage();
                popup.focus();
            } else {
                alert("Please allow popups for this site.");
            }
        });
        $(document).off("click", ".session-box .panel.status .knob-bar")
            .on("click", ".session-box .panel.status .knob-bar", function() {
                if ($("#navigation .title-bar").is(":visible")) $(this).parent().toggleClass("expanded");
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

            const options = [
                "command:loadPrevious",
                "appId:" + appId,
                "logId:" + logId,
                "loadedLines:" + loadedLines
            ];
            this.clients[nodeIndex].sendCommand(options);
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
                    if (!app.hidden) {
                        this.refreshData(app.id);
                    }
                });
            }
        });
    }

    refreshData(appId, dateOffset) {
        const options = ["app:" + appId];
        const dateUnit = $(".control-bar[data-app-id=" + appId + "] .date-unit-options").data("unit");
        if (dateUnit) options.push("dateUnit:" + dateUnit);
        if (dateOffset === "previous") {
            let maxStartDate = "";
            this.viewers.forEach(v => {
                const startDate = v.getMaxStartDatetime(appId);
                if (startDate > maxStartDate) maxStartDate = startDate;
            });
            if (maxStartDate) options.push("dateOffset:" + maxStartDate);
            else {
                $(".control-bar[data-app-id=" + appId + "] .date-offset-options .btn.previous").removeClass("on");
                return;
            }
        }
        setTimeout(() => {
            this.nodes.forEach(node => {
                this.viewers[node.index].setLoading(appId, true);
                this.clients[node.index].refresh(options);
            });
        }, 50);
    }

    suspendMonitoring() {
        this.clients.forEach(client => {
            if (client) client.stop();
        });
        this.viewers.forEach(viewer => {
            if (viewer) viewer.setEnable(false);
        });
    }

    showPopupModeMessage() {
        this.clearView();
        const $container = $(".container-fluid.my-3");
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
        $(".node.tabs .tabs-title.available, .app.tabs .tabs-title.available, " +
          ".node.metrics-bar.available, .app.metrics-bar.available, " +
          ".event-box.available, .visual-box.available, .chart-box.available, .console-box.available").remove();
        $(".node.tabs .tabs-title, .app.tabs .tabs-title, .app.metrics-bar, .console-box").show();
    }

    clearConsole(nodeIndex) {
        $(`.console-box[data-node-index=${nodeIndex}] .console`).empty();
    }

    clearSessions(nodeIndex) {
        $(`.session-box[data-node-index=${nodeIndex}] .sessions`).empty();
    }

    buildView() {
        this.nodes.forEach(node => {
            const $titleTab = this.addNodeTab(node);
            this.viewers[node.index].putIndicator$("node", "event", "", $titleTab.find(".indicator"));
            this.addNodeMetricsBar(node);
        });
        this.apps.forEach(app => {
            const $appTab = this.addInstanceTab(app);
            const $appIndicator = $appTab.find(".indicator");
            this.addControlBar(app);
            this.nodes.forEach(node => {
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
                                       this.addInstanceMetric($eventBox, node, app, metric);
                        viewer.putMetric$(app.id, metric.id, $metric);
                    });
                }
                app.logs.forEach(logInfo => {
                    const $consoleBox = this.addConsoleBox(node, app, logInfo);
                    const $console = $consoleBox.find(".console").data("tailing", true);
                    $consoleBox.find(".tailing-status").addClass("on");
                    viewer.putConsole$(app.id, logInfo.id, $console);
                    viewer.putIndicator$(app.id, "log", logInfo.id, $consoleBox.find(".status-bar"));
                });
            });
        });
        let appId = this.changeInstance();
        if (appId && location.hash) {
            const appId2 = location.hash.substring(1);
            if (appId !== appId2) this.changeInstance(appId2);
        }
    }

    addNodeTab(nodeInfo) {
        const $tabs = $(".node.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-node-id": nodeInfo.id });
        $tab.find("a .title").text(" " + nodeInfo.title + " ");
        if (this.nodes.length > 1) $tab.find(".number").text(" " + (nodeInfo.index + 1));
        return $tab.show().appendTo($tabs);
    }

    addInstanceTab(appInfo) {
        const $tabs = $(".app.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-app-id": appInfo.id, "title": appInfo.title });
        $tab.find("a .title").text(" " + appInfo.title + " ");
        return $tab.show().appendTo($tabs);
    }

    addNodeMetricsBar(nodeInfo) {
        const $metricsBar = $(".node.metrics-bar");
        const $newBar = $metricsBar.first().hide().clone().addClass("available").attr("data-node-index", nodeInfo.index);
        if (this.nodes.length > 1) {
            $newBar.find(".number").text(" " + (nodeInfo.index + 1));
            $newBar.removeClass("full-width");
        } else {
            $newBar.addClass("full-width");
        }
        return $newBar.insertAfter($metricsBar.last());
    }

    addNodeMetric(nodeInfo, metricInfo) {
        const $bar = $(`.node.metrics-bar[data-node-index=${nodeInfo.index}]`).show();
        const $metric = $bar.find(".metric").first().hide().clone().addClass("available");
        $metric.find("dt").text(metricInfo.title).attr("title", metricInfo.description);
        return $metric.appendTo($bar).show();
    }

    addControlBar(appInfo) {
        const $bar = $(".control-bar");
        const $newBar = $bar.first().hide().clone().addClass("available").attr("data-app-id", appInfo.id);
        $newBar.find(".btn.default").text(this.settings.counterPersistInterval + "min.");
        return $newBar.insertAfter($bar.last());
    }

    addEventBox(nodeInfo, appInfo) {
        const $box = $(".event-box").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id });
        const $titleBar = $box.find(".title-bar");
        $titleBar.find("h4").text(nodeInfo.title);
        if (this.nodes.length > 1) $titleBar.find(".number").text(" " + (nodeInfo.index + 1));
        return $box.insertBefore($(".console-box").first());
    }

    addTrackBox($eventBox, nodeInfo, appInfo, eventInfo) {
        const $track = $eventBox.find(".track-box");
        return $track.first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .insertAfter($track.last()).show();
    }

    addInstanceMetric($eventBox, nodeInfo, appInfo, metricInfo) {
        const $bar = $eventBox.find(".metrics-bar").show();
        const $metric = $bar.find(".metric").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-metric-id": metricInfo.id });
        $metric.find("dt").text(metricInfo.title).attr("title", metricInfo.description);
        return $metric.appendTo($bar).show();
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
        $newBox.find(".status-bar h4").text(nodeInfo.title + " ›› " + logInfo.file);
        return $newBox.insertAfter($console.last());
    }
}

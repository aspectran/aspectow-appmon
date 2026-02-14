/**
 * The builder component for the AppMon dashboard.
 * Responsible for assembling the dashboard UI based on configuration data.
 */
class DashboardBuilder {
    constructor() {
        this.settings = {};
        this.domains = [];
        this.instances = [];
        this.viewers = [];
        this.clients = [];
    }

    build(basePath, instancesToJoin) {
        this.clearView();
        $.ajax({
            url: basePath + "/backend/config/data",
            type: "get",
            dataType: "json",
            data: instancesToJoin ? { instances: instancesToJoin } : null,
            success: (data) => {
                if (data) {
                    this.settings = { ...data.settings };
                    this.domains = [];
                    this.instances = [];
                    this.viewers = [];
                    this.clients = [];

                    let index = 0;
                    const random1000 = this.random(1, 1000);

                    data.domains.forEach(domainData => {
                        const domain = {
                            ...domainData,
                            index: index++,
                            random1000: random1000,
                            active: true,
                            client: { established: false, establishCount: 0 }
                        };
                        domain.endpoint.token = data.token;
                        this.domains.push(domain);
                        this.viewers[domain.index] = new DashboardViewer(this.settings.counterPersistInterval * 60);
                        console.log("domain", domain);
                    });

                    data.instances.forEach(instanceData => {
                        const instance = { ...instanceData, active: false };
                        this.instances.push(instance);
                        console.log("instance", instance);
                    });

                    this.buildView();
                    this.bindEvents();
                    if (this.domains.length) {
                        this.establish(0, instancesToJoin);
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

    establish(domainIndex, instancesToJoin) {
        const domain = this.domains[domainIndex];
        const viewer = this.viewers[domainIndex];

        const onJoined = (domain, payload) => {
            this.clearConsole(domain.index);
            if (payload && payload.messages) {
                payload.messages.forEach(msg => viewer.processMessage(msg));
            }
        };

        const onEstablished = (domain) => {
            domain.client.established = true;
            domain.client.establishCount++;
            console.log(domain.name, "connection established:", domain.client.establishCount);
            this.changeDomainState(domain);
            viewer.setEnable(true);
            if (domain.active) {
                viewer.setVisible(true);
            }
            if (domain.client.establishCount === 1) {
                this.initView();
            } else {
                this.clearSessions(domain.index);
            }
            if (domain.client.establishCount + domain.index < this.domains.length) {
                this.establish(domain.index + 1, instancesToJoin);
            }
        };

        const onClosed = (domain) => {
            domain.client.established = false;
            this.changeDomainState(domain);
            viewer.setEnable(false);
        };

        const onFailed = (domain) => {
            this.changeDomainState(domain, true);
            if (domain.endpoint.mode !== "websocket") {
                setTimeout(() => {
                    const client = new PollingClient(domain, viewer, onJoined, onEstablished, onClosed, onFailed);
                    this.clients[domain.index] = client;
                    client.start(instancesToJoin);
                }, (domain.index - 1) * 1000);
            }
        };

        console.log("establishing", domainIndex);
        let client;
        if (domain.endpoint.mode === "polling") {
            client = new PollingClient(domain, viewer, onJoined, onEstablished, onClosed, onFailed);
        } else {
            client = new WebsocketClient(domain, viewer, onJoined, onEstablished, onClosed, onFailed);
        }
        viewer.setClient(client);
        this.clients[domainIndex] = client;
        client.start(instancesToJoin);
    }

    changeDomain(domainIndex) {
        const availableTabs = $(".domain.tabs .tabs-title.available");
        if (availableTabs.length <= 1) return;

        const activeTabs = availableTabs.filter(".active");
        const domain = this.domains[domainIndex];

        if (activeTabs.length === 0) {
            this.domains.forEach(d => { if (d.active) { d.active = false; this.showDomain(d); } });
            domain.active = true;
            this.showDomain(domain);
        } else if (activeTabs.length === 1 && domain.active) {
            this.domains.forEach(d => { if (d.index !== domain.index) { d.active = true; this.showDomain(d); } });
        } else if (activeTabs.length === 1 && !domain.active) {
            this.domains.forEach(d => { if (d.index !== domain.index) { d.active = false; this.showDomain(d); } });
            domain.active = true;
            this.showDomain(domain);
        } else {
            domain.active = !domain.active;
            this.showDomain(domain);
        }

        const activeCount = this.domains.filter(d => d.active).length;
        availableTabs.removeClass("active");
        if (availableTabs.length > activeCount) {
            this.domains.forEach(d => {
                if (d.active) $(".domain.tabs .tabs-title[data-domain-index=" + d.index + "]").addClass("active");
            });
        }

        if (availableTabs.length === activeCount) {
            $(".domain.metrics-bar.available").removeClass("full-width");
        } else {
            $(".domain.metrics-bar.available").addClass("full-width");
        }
    }

    showDomain(domain) {
        const action = domain.active ? "show" : "hide";
        this.instances.forEach(instance => {
            if (instance.active) {
                const selector = `[data-domain-index=${domain.index}][data-instance-name=${instance.name}]`;
                $(`.event-box${selector}, .visual-box${selector}, .console-box${selector}`)[action]();
            }
        });
        this.viewers[domain.index].setVisible(domain.active);
        if (domain.active) {
            this.viewers[domain.index].refreshConsole();
            $(`.domain.metrics-bar[data-domain-index=${domain.index}]`).show();
        } else {
            $(`.domain.metrics-bar[data-domain-index=${domain.index}]`).hide();
        }
    }

    changeDomainState(domain, errorOccurred) {
        const $indicator = $(`.domain.tabs .tabs-title[data-domain-index=${domain.index}] .indicator`);
        $indicator.removeClass($indicator.data("icon-connected") + " connected " +
                           $indicator.data("icon-disconnected") + " disconnected " +
                           $indicator.data("icon-error") + " error");
        if (errorOccurred) {
            $indicator.addClass($indicator.data("icon-error") + " error");
        } else if (domain.client.established) {
            $indicator.addClass($indicator.data("icon-connected") + " connected");
        } else {
            $indicator.addClass($indicator.data("icon-disconnected") + " disconnected");
        }
    }

    changeInstance(instanceName) {
        let exists = false;
        this.instances.forEach(instance => {
            if (!instanceName) instanceName = instance.name;
            const $tabTitle = $(".instance.tabs .tabs-title[data-instance-name=" + instance.name + "]");
            if (instance.name === instanceName) {
                instance.active = true;
                this.showDomainInstance(instanceName);
                $tabTitle.addClass("active");
                exists = true;
            } else {
                instance.active = false;
                $tabTitle.removeClass("active");
            }
        });
        if (!exists && instanceName) return this.changeInstance();
        return instanceName;
    }

    showDomainInstance(instanceName) {
        $(".control-bar[data-instance-name!=" + instanceName + "]").hide();
        $(".control-bar[data-instance-name=" + instanceName + "]").show();
        this.domains.forEach(domain => {
            if (domain.active) {
                $(`.track-box[data-domain-index=${domain.index}] .bullet`).remove();
                const selector = `[data-domain-index=${domain.index}][data-instance-name=${instanceName}]`;
                const otherSelector = `[data-domain-index=${domain.index}][data-instance-name!=${instanceName}]`;
                $(`.event-box${otherSelector}, .visual-box${otherSelector}, .console-box${otherSelector}`).hide();
                $(`.event-box${selector}, .visual-box${selector}`).show();
                $(`.console-box${selector}`).show().each((_, el) => {
                    const $console = $(el).find(".console");
                    if (!$console.data("pause")) {
                        this.viewers[domain.index].refreshConsole($console);
                    }
                });
            }
        });
    }

    initView() {
        $(".speed-options").addClass("hide");
        if (this.domains.some(d => d.endpoint.mode === "polling")) {
            $(".speed-options").removeClass("hide");
        }
        this.instances.forEach(instance => {
            const $eventBox = $(`.event-box[data-instance-name=${instance.name}]`);
            const $visualBox = $(`.visual-box[data-instance-name=${instance.name}]`);
            if ($eventBox.length && $visualBox.length && $eventBox.find(".session-box.available").length === 0) {
                $eventBox.removeClass("col-lg-6").addClass("fixed-layout");
                $visualBox.removeClass("col-lg-6").addClass("fixed-layout");
            }
        });
    }

    bindEvents() {
        $(".domain.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const domainIndex = $(e.currentTarget).closest(".tabs-title").data("domain-index");
            this.changeDomain(domainIndex);
        });
        $(".instance.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const instanceName = $(e.currentTarget).closest(".tabs-title").data("instance-name");
            this.changeInstance(instanceName);
        });
        $(".layout-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const instanceName = $btn.closest(".control-bar").data("instance-name");
            const isCompact = $btn.hasClass("compact");
            if (!$btn.hasClass("on")) {
                if (isCompact) {
                    $btn.addClass("on");
                    $(`.event-box.available:not(.fixed-layout)[data-instance-name=${instanceName}], 
                       .visual-box.available:not(.fixed-layout)[data-instance-name=${instanceName}], 
                       .console-box.available[data-instance-name=${instanceName}]`).addClass("col-lg-6");
                }
            } else if (isCompact) {
                $btn.removeClass("on");
                $(`.event-box.available:not(.fixed-layout)[data-instance-name=${instanceName}], 
                   .visual-box.available:not(.fixed-layout)[data-instance-name=${instanceName}], 
                   .console-box.available[data-instance-name=${instanceName}]`).removeClass("col-lg-6");
            }
            this.viewers.forEach(v => v.updateCanvasWidth());
            this.refreshData(instanceName);
        });
        $(".date-unit-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $controlBar = $btn.closest(".control-bar");
            const instanceName = $controlBar.data("instance-name");
            const unit = $btn.data("unit") || "";
            $btn.parent().data("unit", unit).find(".btn").removeClass("on");
            $btn.addClass("on");
            $controlBar.find(".date-offset-options").data("offset", "").find(".btn.current").removeClass("on");
            this.viewers.forEach(v => v.updateCanvasWidth());
            this.refreshData(instanceName);
        });
        $(".date-offset-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $controlBar = $btn.closest(".control-bar");
            const instanceName = $controlBar.data("instance-name");
            const offset = $btn.data("offset") || "";
            const $parent = $btn.parent();
            if (offset !== "current") $parent.find(".btn.current").addClass("on");
            else {
                $parent.find(".btn").addClass("on");
                $parent.find(".btn.current").removeClass("on");
            }
            $parent.data("offset", offset);
            this.refreshData(instanceName, offset);
        });
        $(".speed-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const faster = !$btn.hasClass("on");
            $btn.toggleClass("on", faster);
            this.domains.forEach(domain => {
                if (domain.endpoint.mode === "polling") {
                    this.clients[domain.index].speed(faster ? 1 : 0);
                }
            });
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
            const domainIndex = $consoleBox.data("domain-index");
            const isTailing = !!$console.data("tailing");
            const newTailingState = !isTailing;

            $console.data("tailing", newTailingState);
            $consoleBox.find(".tailing-status").toggleClass("on", newTailingState);
            $btn.attr("title", newTailingState ? $btn.data("title-on") : $btn.data("title-off"));

            if (newTailingState) {
                this.viewers[domainIndex].refreshConsole($console);
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
            this.viewers[$consoleBox.data("domain-index")].clearConsole($consoleBox.find(".console"));
        });
        $(window).off("resize").on("resize", () => {
            this.viewers.forEach(v => v.updateCanvasWidth());
        });
    }

    refreshData(instanceName, dateOffset) {
        const options = ["instance:" + instanceName];
        const dateUnit = $(".control-bar[data-instance-name=" + instanceName + "] .date-unit-options").data("unit");
        if (dateUnit) options.push("dateUnit:" + dateUnit);
        if (dateOffset === "previous") {
            let maxStartDate = "";
            this.viewers.forEach(v => {
                const startDate = v.getMaxStartDatetime(instanceName);
                if (startDate > maxStartDate) maxStartDate = startDate;
            });
            if (maxStartDate) options.push("dateOffset:" + maxStartDate);
            else {
                $(".control-bar[data-instance-name=" + instanceName + "] .date-offset-options .btn.previous").removeClass("on");
                return;
            }
        }
        setTimeout(() => {
            this.domains.forEach(domain => {
                this.viewers[domain.index].setLoading(instanceName, true);
                this.clients[domain.index].refresh(options);
            });
        }, 50);
    }

    clearView() {
        $(".domain.tabs .tabs-title.available, .instance.tabs .tabs-title.available, " +
          ".domain.metrics-bar.available, .instance.metrics-bar.available, " +
          ".event-box.available, .visual-box.available, .chart-box.available, .console-box.available").remove();
        $(".domain.tabs .tabs-title, .instance.tabs .tabs-title, .instance.metrics-bar, .console-box").show();
    }

    clearConsole(domainIndex) {
        $(`.console-box[data-domain-index=${domainIndex}] .console`).empty();
    }

    clearSessions(domainIndex) {
        $(`.session-box[data-domain-index=${domainIndex}] .sessions`).empty();
    }

    buildView() {
        this.domains.forEach(domain => {
            const $titleTab = this.addDomainTab(domain);
            this.viewers[domain.index].putIndicator$("domain", "event", "", $titleTab.find(".indicator"));
            this.addDomainMetricsBar(domain);
        });
        this.instances.forEach(instance => {
            const $instanceTab = this.addInstanceTab(instance);
            const $instanceIndicator = $instanceTab.find(".indicator");
            this.addControlBar(instance);
            this.domains.forEach(domain => {
                const viewer = this.viewers[domain.index];
                viewer.putIndicator$("instance", "event", instance.name, $instanceIndicator);
                if (instance.events && instance.events.length) {
                    const $eventBox = this.addEventBox(domain, instance);
                    instance.events.forEach(event => {
                        if (event.name === "activity") {
                            const $trackBox = this.addTrackBox($eventBox, domain, instance, event);
                            viewer.putDisplay$(instance.name, event.name, $trackBox);
                            viewer.putIndicator$(instance.name, "event", event.name, $trackBox.find(".activity-status"));
                        } else if (event.name === "session") {
                            viewer.putDisplay$(instance.name, event.name, this.addSessionBox($eventBox, domain, instance, event));
                        }
                    });
                    const $visualBox = this.addVisualBox(domain, instance);
                    instance.events.forEach(event => {
                        if (event.name === "activity" || event.name === "session") {
                            viewer.putChart$(instance.name, event.name, this.addChartBox($visualBox, domain, instance, event).find(".chart"));
                        }
                    });
                }
                if (instance.metrics && instance.metrics.length) {
                    const $eventBox = $(`.event-box[data-domain-index=${domain.index}][data-instance-name=${instance.name}]`);
                    instance.metrics.forEach(metric => {
                        const $metric = (metric.heading || !$eventBox.length) ? 
                                       this.addDomainMetric(domain, metric) : 
                                       this.addInstanceMetric($eventBox, domain, instance, metric);
                        viewer.putMetric$(instance.name, metric.name, $metric);
                    });
                }
                instance.logs.forEach(logInfo => {
                    const $consoleBox = this.addConsoleBox(domain, instance, logInfo);
                    const $console = $consoleBox.find(".console").data("tailing", true);
                    $consoleBox.find(".tailing-status").addClass("on");
                    viewer.putConsole$(instance.name, logInfo.name, $console);
                    viewer.putIndicator$(instance.name, "log", logInfo.name, $consoleBox.find(".status-bar"));
                });
            });
        });
        let instanceName = this.changeInstance();
        if (instanceName && location.hash) {
            const instanceName2 = location.hash.substring(1);
            if (instanceName !== instanceName2) this.changeInstance(instanceName2);
        }
    }

    addDomainTab(domainInfo) {
        const $tabs = $(".domain.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-domain-index": domainInfo.index, "data-domain-name": domainInfo.name });
        $tab.find("a .title").text(" " + domainInfo.title + " ");
        if (this.domains.length > 1) $tab.find(".number").text(" " + (domainInfo.index + 1));
        return $tab.show().appendTo($tabs);
    }

    addInstanceTab(instanceInfo) {
        const $tabs = $(".instance.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-instance-name": instanceInfo.name, "title": instanceInfo.title });
        $tab.find("a .title").text(" " + instanceInfo.title + " ");
        return $tab.show().appendTo($tabs);
    }

    addDomainMetricsBar(domainInfo) {
        const $metricsBar = $(".domain.metrics-bar");
        const $newBar = $metricsBar.first().hide().clone().addClass("available").attr("data-domain-index", domainInfo.index);
        if (this.domains.length > 1) $newBar.find(".number").text(" " + (domainInfo.index + 1));
        else $newBar.addClass("full-width");
        return $newBar.insertAfter($metricsBar.last());
    }

    addDomainMetric(domainInfo, metricInfo) {
        const $bar = $(`.domain.metrics-bar[data-domain-index=${domainInfo.index}]`).show();
        const $metric = $bar.find(".metric").first().hide().clone().addClass("available");
        $metric.find("dt").text(metricInfo.title).attr("title", metricInfo.description);
        return $metric.appendTo($bar).show();
    }

    addControlBar(instanceInfo) {
        const $bar = $(".control-bar");
        const $newBar = $bar.first().hide().clone().addClass("available").attr("data-instance-name", instanceInfo.name);
        $newBar.find(".btn.default").text(this.settings.counterPersistInterval + "min.");
        return $newBar.insertAfter($bar.last());
    }

    addEventBox(domainInfo, instanceInfo) {
        const $box = $(".event-box").first().hide().clone().addClass("available")
            .attr({ "data-domain-index": domainInfo.index, "data-instance-name": instanceInfo.name });
        const $titleBar = $box.find(".title-bar");
        $titleBar.find("h4").text(domainInfo.title);
        if (this.domains.length > 1) $titleBar.find(".number").text(" " + (domainInfo.index + 1));
        return $box.insertBefore($(".console-box").first());
    }

    addTrackBox($eventBox, domainInfo, instanceInfo, eventInfo) {
        const $track = $eventBox.find(".track-box");
        return $track.first().hide().clone().addClass("available")
            .attr({ "data-domain-index": domainInfo.index, "data-instance-name": instanceInfo.name, "data-event-name": eventInfo.name })
            .insertAfter($track.last()).show();
    }

    addInstanceMetric($eventBox, domainInfo, instanceInfo, metricInfo) {
        const $bar = $eventBox.find(".metrics-bar").show();
        const $metric = $bar.find(".metric").first().hide().clone().addClass("available")
            .attr({ "data-domain-index": domainInfo.index, "data-instance-name": instanceInfo.name, "data-metric-name": metricInfo.name });
        $metric.find("dt").text(metricInfo.title).attr("title", metricInfo.description);
        return $metric.appendTo($bar).show();
    }

    addSessionBox($eventBox, domainInfo, instanceInfo, eventInfo) {
        const $session = $eventBox.find(".session-box");
        return $session.first().hide().clone().addClass("available")
            .attr({ "data-domain-index": domainInfo.index, "data-instance-name": instanceInfo.name, "data-event-name": eventInfo.name })
            .insertAfter($session.last()).show();
    }

    addVisualBox(domainInfo, instanceInfo) {
        return $(".visual-box").first().hide().clone().addClass("available")
            .attr({ "data-domain-index": domainInfo.index, "data-instance-name": instanceInfo.name })
            .insertBefore($(".console-box").first()).show();
    }

    addChartBox($visualBox, domainInfo, instanceInfo, eventInfo) {
        const $chart = $visualBox.find(".chart-box");
        return $chart.first().hide().clone().addClass("available col-12 col-lg-6")
            .attr({ "data-domain-index": domainInfo.index, "data-instance-name": instanceInfo.name, "data-event-name": eventInfo.name })
            .appendTo($visualBox).show();
    }

    addConsoleBox(domainInfo, instanceInfo, logInfo) {
        const $console = $(".console-box");
        const $newBox = $console.first().hide().clone().addClass("available col-lg-6")
            .attr({ "data-domain-index": domainInfo.index, "data-instance-name": instanceInfo.name, "data-log-name": logInfo.name });
        $newBox.find(".status-bar h4").text(domainInfo.title + " ›› " + logInfo.file);
        return $newBox.insertAfter($console.last());
    }
}

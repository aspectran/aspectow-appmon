function FrontBuilder() {

    const domains = [];
    const instances = [];
    const viewers = [];
    const clients = [];

    this.build = function (basePath, token, instancesToJoin) {
        clearView();
        $.ajax({
            url: basePath + "/backend/" + token + "/config",
            type: "get",
            dataType: "json",
            data: instancesToJoin ? {
                instances: instancesToJoin
            } : null,
            success: function (data) {
                if (data) {
                    domains.length = 0;
                    instances.length = 0;
                    viewers.length = 0;
                    clients.length = 0;
                    let index = 0;
                    let random1000 = random(1, 1000);
                    for (let key in data.domains) {
                        let domain = data.domains[key];
                        domain['index'] = index++;
                        domain['random1000'] = random1000;
                        domain['active'] = true;
                        domain.endpoint['token'] = data.token;
                        domain['client'] = {
                            established: false,
                            establishCount: 0
                        };
                        domains.push(domain);
                        viewers[domain.index] = new FrontViewer(domain.sampleInterval * 60);
                        console.log("domain", domain);
                    }
                    for (let key in data.instances) {
                        let instance = data.instances[key];
                        instance['active'] = false;
                        instances.push(instance);
                        console.log("instance", instance);
                    }
                    buildView();
                    bindEvents();
                    if (domains.length) {
                        establish(0, instancesToJoin);
                    }
                }
            }
        });
    };

    const random = function(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    };

    const establish = function (domainIndex, instancesToJoin) {
        function onJoined(domain, payload) {
            clearConsole(domain.index);
            if (payload) {
                for (let key in payload.messages) {
                    let msg = payload.messages[key];
                    viewers[domain.index].processMessage(msg);
                }
            }
        }
        function onEstablished(domain) {
            domain.client.established = true;
            domain.client.establishCount++;
            console.log(domain.name, "connection established:", domain.client.establishCount);
            console.log(domain.name, "endpoint mode:", domain.endpoint.mode)
            changeDomainState(domain);
            viewers[domain.index].setEnable(true);
            console.log(domain.name, "viewer enabled");
            if (domain.active) {
                viewers[domain.index].setVisible(true);
            }
            if (domain.client.establishCount === 1) {
                console.log(domain.name, "init view");
                initView();
            } else {
                console.log(domain.name, "clear sessions");
                clearSessions(domain.index);
            }
            if (domain.client.establishCount + domain.index < domains.length) {
                establish(domain.index + 1, instancesToJoin);
            }
        }
        function onClosed(domain) {
            domain.client.established = false;
            changeDomainState(domain);
            viewers[domain.index].setEnable(false);
            console.log(domain.name, "viewer disabled");
        }
        function onFailed(domain) {
            changeDomainState(domain, true);
            if (domain.endpoint.mode !== "websocket") {
                setTimeout(function () {
                    let client = new PollingClient(domain, viewers[domain.index], onJoined, onEstablished, onClosed, onFailed);
                    clients[domain.index] = client;
                    client.start(instancesToJoin);
                }, (domain.index - 1) * 1000);
            }
        }

        console.log("establishing", domainIndex);
        let domain = domains[domainIndex];
        let viewer = viewers[domainIndex];
        let client;
        if (domain.endpoint.mode === "polling") {
            client = new PollingClient(domain, viewer, onJoined, onEstablished, onClosed, onFailed);
        } else {
            client = new WebsocketClient(domain, viewer, onJoined, onEstablished, onClosed, onFailed);
        }
        viewer.setClient(client);
        clients[domainIndex] = client;
        client.start(instancesToJoin);
    };

    const changeDomain = function (domainIndex) {
        let availableTabs = $(".domain.tabs .tabs-title.available").length;
        if (availableTabs <= 1) {
            return;
        }
        let activeTabs = $(".domain.tabs .tabs-title.available.active").length;
        let domain = domains[domainIndex];
        if (activeTabs === 0) {
            for (let key in domains) {
                if (!!domains[key].active) {
                    domains[key].active = false;
                    showDomain(domains[key]);
                }
            }
            domain.active = true;
            showDomain(domain);
        } else if (activeTabs === 1 && domain.active) {
            for (let key in domains) {
                if (domains[key].index !== domain.index) {
                    domains[key].active = true;
                    showDomain(domains[key]);
                }
            }
        } else if (activeTabs === 1 && !domain.active) {
            for (let key in domains) {
                if (domains[key].index !== domain.index) {
                    domains[key].active = false;
                    showDomain(domains[key]);
                }
            }
            domain.active = true;
            showDomain(domain);
        } else {
            domain.active = !!!domain.active;
            showDomain(domain);
        }
        let activeDomains = 0;
        for (let key in domains) {
            if (domains[key].active) {
                activeDomains++;
            }
        }
        $(".domain.tabs .tabs-title.available").removeClass("active");
        if (availableTabs > activeDomains) {
            for (let key in domains) {
                if (domains[key].active) {
                    $(".domain.tabs .tabs-title[data-domain-index=" + domains[key].index + "]").addClass("active");
                }
            }
        }
    };

    const showDomain = function (domain) {
        if (domain.active) {
            for (let key in instances) {
                let instance = instances[key];
                if (instance.active) {
                    $(".event-box[data-domain-index=" + domain.index + "][data-instance-name=" + instance.name + "]").show();
                    $(".visual-box[data-domain-index=" + domain.index + "][data-instance-name=" + instance.name + "]").show();
                    $(".console-box[data-domain-index=" + domain.index + "][data-instance-name=" + instance.name + "]").show();
                }
            }
            viewers[domain.index].setVisible(true);
            viewers[domain.index].refreshConsole();
            $(".domain.status-bar[data-domain-index=" + domain.index + "]").show();
        } else {
            viewers[domain.index].setVisible(false);
            for (let key in instances) {
                let instance = instances[key];
                if (instance.active) {
                    $(".event-box[data-domain-index=" + domain.index + "][data-instance-name=" + instance.name + "]").hide();
                    $(".visual-box[data-domain-index=" + domain.index + "][data-instance-name=" + instance.name + "]").hide();
                    $(".console-box[data-domain-index=" + domain.index + "][data-instance-name=" + instance.name + "]").hide();
                }
            }
            $(".domain.status-bar[data-domain-index=" + domain.index + "]").hide();
        }
    }

    const changeDomainState = function (domain, errorOccurred) {
        let $titleTab = $(".domain.tabs .tabs-title[data-domain-index=" + domain.index + "]");
        let $indicator = $titleTab.find(".indicator");
        $indicator.removeClass($indicator.data("icon-connected") + " connected");
        $indicator.removeClass($indicator.data("icon-disconnected") + " disconnected");
        $indicator.removeClass($indicator.data("icon-error") + " error");
        if (errorOccurred) {
            $indicator.addClass($indicator.data("icon-error") + " error");
        } else if (domain.client.established) {
            $indicator.addClass($indicator.data("icon-connected") + " connected");
        } else {
            $indicator.addClass($indicator.data("icon-disconnected") + " disconnected");
        }
    }

    const changeInstance = function (instanceName) {
        let exists = false;
        for (let key in instances) {
            let instance = instances[key];
            if (!instanceName) {
                instanceName = instance.name;
            }
            let $tabTitle = $(".instance.tabs .tabs-title[data-instance-name=" + instance.name + "]");
            if (instance.name === instanceName) {
                instance.active = true;
                showDomainInstance(instanceName);
                if (!$tabTitle.hasClass("active")) {
                    $tabTitle.addClass("active");
                }
                exists = true;
            } else {
                instance.active = false;
                $tabTitle.removeClass("active");
            }
        }
        if (!exists && instanceName) {
            return changeInstance();
        } else {
            return instanceName;
        }
    }

    const showDomainInstance = function (instanceName) {
        $(".control-bar[data-instance-name!=" + instanceName + "]").hide();
        $(".control-bar[data-instance-name=" + instanceName + "]").show();
        for (let key in domains) {
            let domain = domains[key];
            if (domain.active) {
                $(".track-box[data-domain-index=" + domain.index + "] .bullet").remove();
                $(".event-box[data-domain-index=" + domain.index + "][data-instance-name!=" + instanceName + "]").hide();
                $(".event-box[data-domain-index=" + domain.index + "][data-instance-name=" + instanceName + "]").show();
                $(".visual-box[data-domain-index=" + domain.index + "][data-instance-name!=" + instanceName + "]").hide();
                $(".visual-box[data-domain-index=" + domain.index + "][data-instance-name=" + instanceName + "]").show();
                $(".console-box[data-domain-index=" + domain.index + "][data-instance-name!=" + instanceName + "]").hide();
                $(".console-box[data-domain-index=" + domain.index + "][data-instance-name=" + instanceName + "]").show().each(function () {
                    let $console = $(this).find(".console");
                    if (!$console.data("pause")) {
                        viewers[domain.index].refreshConsole($console);
                    }
                });
            }
        }
    };

    const initView = function () {
        $(".speed-options").addClass("hide");
        let pollingMode = false;
        for (let key in domains) {
            let domain = domains[key];
            if (domain.endpoint.mode === "polling") {
                pollingMode = true;
                break;
            }
        }
        if (pollingMode) {
            $(".speed-options").removeClass("hide");
        }
        for (let key in instances) {
            let instance = instances[key];
            let $eventBox = $(".event-box[data-instance-name=" + instance.name + "]");
            let $visualBox = $(".visual-box[data-instance-name=" + instance.name + "]");
            if ($eventBox.length && $visualBox.length) {
                if ($eventBox.find(".session-box.available").length === 0) {
                    $eventBox.removeClass("col-lg-6").addClass("fixed-layout");
                    $visualBox.removeClass("col-lg-6").addClass("fixed-layout");
                }
            } else {
                $eventBox.removeClass("col-lg-6").addClass("fixed-layout");
                $visualBox.removeClass("col-lg-6").addClass("fixed-layout");
            }
        }
    };

    const bindEvents = function () {
        $(".domain.tabs .tabs-title.available a").off("click").on("click", function() {
            let domainIndex = $(this).closest(".tabs-title").data("domain-index");
            changeDomain(domainIndex);
        });
        $(".instance.tabs .tabs-title.available a").off("click").on("click", function() {
            let instanceName = $(this).closest(".tabs-title").data("instance-name");
            changeInstance(instanceName);
        });
        $(".layout-options .btn").off().on("click", function() {
            let instanceName = $(this).closest(".control-bar").data("instance-name");
            if (!$(this).hasClass("on")) {
                if ($(this).hasClass("compact")) {
                    $(this).addClass("on");
                    $(".event-box.available:not(.fixed-layout)[data-instance-name=" + instanceName + "]").addClass("col-lg-6");
                    $(".visual-box.available:not(.fixed-layout)[data-instance-name=" + instanceName + "]").addClass("col-lg-6");
                    $(".console-box.available[data-instance-name=" + instanceName + "]").addClass("col-lg-6");
                }
            } else {
                if ($(this).hasClass("compact")) {
                    $(this).removeClass("on");
                    $(".event-box.available:not(.fixed-layout)[data-instance-name=" + instanceName + "]").removeClass("col-lg-6");
                    $(".visual-box.available:not(.fixed-layout)[data-instance-name=" + instanceName + "]").removeClass("col-lg-6");
                    $(".console-box.available[data-instance-name=" + instanceName + "]").removeClass("col-lg-6");
                }
            }
            refreshData(instanceName);
        });
        $(".date-unit-options .btn").off().on("click", function() {
            let $controlBar = $(this).closest(".control-bar");
            let instanceName = $controlBar.data("instance-name");
            let unit = $(this).data("unit")||"";
            $(this).parent().data("unit", unit).find(".btn").removeClass("on");
            $(this).addClass("on");
            $controlBar.find(".date-offset-options").data("offset", "");
            $controlBar.find(".date-offset-options .btn.current").removeClass("on");
            refreshData(instanceName);
        });
        $(".date-offset-options .btn").off().on("click", function() {
            let $controlBar = $(this).closest(".control-bar");
            let instanceName = $controlBar.data("instance-name");
            let offset = $(this).data("offset")||"";
            if (offset !== "current") {
                $(this).parent().find(".btn.current").addClass("on");
            } else {
                $(this).parent().find(".btn").addClass("on");
                $(this).parent().find(".btn.current").removeClass("on");
            }
            $(this).parent().data("offset", offset);
            refreshData(instanceName, offset);
        });
        $(".speed-options .btn").off().on("click", function() {
            let faster = !$(this).hasClass("on");
            if (faster) {
                $(this).addClass("on");
            } else {
                $(this).removeClass("on");
            }
            for (let key in domains) {
                let domain = domains[key];
                if (domain.endpoint.mode === "polling") {
                    if (faster) {
                        clients[domain.index].speed(1);
                    } else {
                        clients[domain.index].speed(0);
                    }
                }
            }
        });
        $(document).off("click", ".session-box .panel.status .knob-bar")
            .on("click", ".session-box .panel.status .knob-bar", function() {
                if ($("#navigation .title-bar").is(":visible")) {
                    $(this).parent().toggleClass("expanded")
                }
        });
        $(document).off("click", ".session-box ul.sessions li")
            .on("click", ".session-box ul.sessions li", function() {
                $(this).toggleClass("designated");
        });
        $(".console-box .tailing-switch").off("click").on("click", function() {
            let $consoleBox = $(this).closest(".console-box");
            let $console = $consoleBox.find(".console");
            let domainIndex = $consoleBox.data("domain-index");
            if ($console.data("tailing")) {
                $console.data("tailing", false);
                $consoleBox.find(".tailing-status").removeClass("on");
            } else {
                $console.data("tailing", true);
                $(this).find(".tailing-status").addClass("on");
                viewers[domainIndex].refreshConsole($console);
            }
        });
        $(".console-box .pause-switch").off("click").on("click", function() {
            let $console = $(this).closest(".console-box").find(".console");
            if ($console.data("pause")) {
                $console.data("pause", false);
                $(this).removeClass("on");
            } else {
                $console.data("pause", true);
                $(this).addClass("on");
            }
        });
        $(".console-box .clear-screen").off("click").on("click", function() {
            let $consoleBox = $(this).closest(".console-box");
            let $console = $consoleBox.find(".console");
            let domainIndex = $consoleBox.data("domain-index");
            viewers[domainIndex].clearConsole($console);
        });
    };

    const refreshData = function (instanceName, dateOffset) {
        let options = [];
        options.push("instance:" + instanceName);
        let dateUnit = $(".control-bar[data-instance-name=" + instanceName + "] .date-unit-options").data("unit");
        if (dateUnit) {
            options.push("dateUnit:" + dateUnit);
        }
        if (dateOffset === "previous") {
            let maxStartDate = "";
            for (let key in viewers) {
                let viewer = viewers[key];
                let startDate = viewer.getMaxStartDatetime(instanceName);
                if (startDate > maxStartDate) {
                    maxStartDate = startDate;
                }
            }
            if (maxStartDate) {
                options.push("dateOffset:" + maxStartDate);
            } else {
                $(".control-bar[data-instance-name=" + instanceName + "] .date-offset-options .btn.previous").removeClass("on");
                return;
            }
        }
        setTimeout(function () {
            for (let key in domains) {
                let domain = domains[key];
                clients[domain.index].refresh(options);
            }
        }, 50);
    }

    const clearView = function () {
        $(".domain.tabs .tabs-title.available").remove();
        $(".domain.tabs .tabs-title").show();
        $(".instance.tabs .tabs-title.available").remove();
        $(".instance.tabs .tabs-title").show();
        $(".domain.status-bar.available").remove();
        $(".instance.status-bar.available").remove();
        $(".instance.status-bar").show();
        $(".event-box.available").remove();
        $(".visual-box.available").remove();
        $(".chart-box.available").remove();
        $(".console-box.available").remove();
        $(".console-box").show();
    };

    const clearConsole = function (domainIndex) {
        $(".console-box[data-domain-index=" + domainIndex + "] .console").empty();
    };

    const clearSessions = function (domainIndex) {
        $(".session-box[data-domain-index=" + domainIndex + "] .sessions").empty();
    };

    const buildView = function () {
        let sampleInterval = 0;
        for (let key in domains) {
            let domain = domains[key];
            let $titleTab = addDomainTab(domain);
            let $domainIndicator = $titleTab.find(".indicator");
            viewers[domain.index].putIndicator("domain", "event", "", $domainIndicator);
            sampleInterval = Math.max(domain.sampleInterval, sampleInterval);
            addDomainStatusBar(domain);
        }
        for (let key in instances) {
            let instance = instances[key];
            let $instanceTab = addInstanceTab(instance);
            let $instanceIndicator = $instanceTab.find(".indicator");
            addControlBar(instance, sampleInterval);
            for (let key in domains) {
                let domain = domains[key];
                viewers[domain.index].putIndicator("instance", "event", instance.name, $instanceIndicator);
                if (instance.events && instance.events.length) {
                    let $eventBox = addEventBox(domain, instance);
                    for (let key in instance.events) {
                        let event = instance.events[key];
                        if (event.name === "activity") {
                            let $trackBox = addTrackBox($eventBox, domain, instance, event);
                            let $activityStatus = $trackBox.find(".activity-status");
                            viewers[domain.index].putDisplay(instance.name, event.name, $trackBox);
                            viewers[domain.index].putIndicator(instance.name, "event", event.name, $activityStatus);
                        } else if (event.name === "session") {
                            let $sessionBox = addSessionBox($eventBox, domain, instance, event);
                            viewers[domain.index].putDisplay(instance.name, event.name, $sessionBox);
                        } else if (event.name.startsWith("status")) {
                            if (event.leading) {
                                let $status = addDomainStatus(domain, event);
                                viewers[domain.index].putStatus(instance.name, event.name, $status);
                            } else {
                                let $status = addInstanceStatus($eventBox, domain, instance, event);
                                viewers[domain.index].putStatus(instance.name, event.name, $status);
                            }
                        }
                    }
                    let $visualBox = addVisualBox(domain, instance);
                    for (let key in instance.events) {
                        let event = instance.events[key];
                        if (event.name === "activity" || event.name === "session") {
                            let $chartBox = addChartBox($visualBox, domain, instance, event);
                            viewers[domain.index].putChart(instance.name, event.name, $chartBox.find(".chart"));
                        }
                    }
                }
                for (let key in instance.logs) {
                    let logInfo = instance.logs[key];
                    let $consoleBox = addConsoleBox(domain, instance, logInfo);
                    let $console = $consoleBox.find(".console").data("tailing", true);
                    $consoleBox.find(".tailing-status").addClass("on");
                    viewers[domain.index].putConsole(instance.name, logInfo.name, $console);
                    let $logIndicator = $consoleBox.find(".status-bar");
                    viewers[domain.index].putIndicator(instance.name, "log", logInfo.name, $logIndicator);
                }
            }
        }
        let instanceName = changeInstance();
        if (instanceName && location.hash) {
            let instanceName2 = location.hash.substring(1);
            if (instanceName !== instanceName2) {
                changeInstance(instanceName2);
            }
        }
    };

    const addDomainTab = function (domainInfo) {
        let $tabs = $(".domain.tabs");
        let $tab = $tabs.find(".tabs-title").first().hide().clone()
            .addClass("available")
            .attr("data-domain-index", domainInfo.index)
            .attr("data-domain-name", domainInfo.name)
            .attr("data-domain-title", domainInfo.title)
            .attr("data-domain-url", domainInfo.url);
        $tab.find("a .title").text(" " + domainInfo.title + " ");
        if (domains.length > 1) {
            $tab.find(".number").text(" " + (domainInfo.index + 1));
        }
        $tab.show().appendTo($tabs);
        return $tab;
    };

    const addInstanceTab = function (instanceInfo) {
        let $tabs = $(".instance.tabs");
        let $tab0 = $tabs.find(".tabs-title").first();
        let $tab = $tab0.hide().clone()
            .addClass("available")
            .attr("data-instance-name", instanceInfo.name)
            .attr("title", instanceInfo.title);
        $tab.find("a .title").text(" " + instanceInfo.title + " ");
        $tab.show().appendTo($tabs);
        return $tab;
    };

    const addDomainStatusBar = function (domainInfo) {
        let $statusBar = $(".domain.status-bar");
        let $newStatusBar = $statusBar.first().hide().clone()
            .addClass("available")
            .attr("data-domain-index", domainInfo.index)
        if (domains.length > 1) {
            $newStatusBar.find(".number").text(" " + (domainInfo.index + 1));
        }
        return $newStatusBar.insertAfter($statusBar.last());
    };

    const addDomainStatus = function (domainInfo, eventInfo) {
        let $statusBar = $(".domain.status-bar[data-domain-index=" + domainInfo.index + "]").show();
        let $status = $statusBar.find(".status").first().hide().clone()
            .addClass("available")
        $status.find("dt").text(eventInfo.title);
        $status.find("dd").text("");
        return $status.appendTo($statusBar).show();
    };

    const addControlBar = function (instanceInfo, sampleInterval) {
        let $controlBar = $(".control-bar");
        let $newControlBar = $controlBar.first().hide().clone()
            .addClass("available")
            .attr("data-instance-name", instanceInfo.name);
        $newControlBar.find(".btn.default").text(sampleInterval + "min.");
        return $newControlBar.insertAfter($controlBar.last());
    };

    const addEventBox = function (domainInfo, instanceInfo) {
        let $eventBox = $(".event-box");
        let $newBox = $eventBox.first().hide().clone()
            .addClass("available")
            .attr("data-domain-index", domainInfo.index)
            .attr("data-instance-name", instanceInfo.name);
        let $titleBar = $newBox.find(".title-bar");
        $titleBar.find("h4").text(domainInfo.title);
        if (domains.length > 1) {
            $titleBar.find(".number").text(" " + (domainInfo.index + 1));
        }
        return $newBox.insertBefore($(".console-box").first());
    };

    const addTrackBox = function ($eventBox, domainInfo, instanceInfo, eventInfo) {
        let $trackBox = $eventBox.find(".track-box");
        let $newBox = $trackBox.first().hide().clone()
            .addClass("available")
            .attr("data-domain-index", domainInfo.index)
            .attr("data-instance-name", instanceInfo.name)
            .attr("data-event-name", eventInfo.name);
        return $newBox.insertAfter($trackBox.last()).show();
    };

    const addInstanceStatus = function ($eventBox, domainInfo, instanceInfo, eventInfo) {
        let $statusBar = $eventBox.find(".status-bar").show();
        let $status = $statusBar.find(".status").first().hide().clone()
            .addClass("available")
            .attr("data-domain-index", domainInfo.index)
            .attr("data-instance-name", instanceInfo.name)
            .attr("data-event-name", eventInfo.name);
        $status.find("dt").text(eventInfo.title);
        $status.find("dd").text("N/A");
        return $status.appendTo($statusBar).show();
    };

    const addSessionBox = function ($eventBox, domainInfo, instanceInfo, eventInfo) {
        let $sessionBox = $eventBox.find(".session-box");
        let $newBox = $sessionBox.first().hide().clone()
            .addClass("available")
            .attr("data-domain-index", domainInfo.index)
            .attr("data-instance-name", instanceInfo.name)
            .attr("data-event-name", eventInfo.name);
        return $newBox.insertAfter($sessionBox.last()).show();
    };

    const addVisualBox = function (domainInfo, instanceInfo) {
        let $visualBox = $(".visual-box");
        let $newBox = $visualBox.first().hide().clone()
            .addClass("available")
            .attr("data-domain-index", domainInfo.index)
            .attr("data-instance-name", instanceInfo.name);
        return $newBox.insertBefore($(".console-box").first()).show();
    };

    const addChartBox = function ($visualBox, domainInfo, instanceInfo, eventInfo) {
        let $chartBox = $visualBox.find(".chart-box");
        let $newBox = $chartBox.first().hide().clone()
            .addClass("available col-lg-6")
            .attr("data-domain-index", domainInfo.index)
            .attr("data-instance-name", instanceInfo.name)
            .attr("data-event-name", eventInfo.name);
        $newBox.find(".chart-title")
            .text(eventInfo.name);
        return $newBox.appendTo($visualBox).show();
    }

    const addConsoleBox = function (domainInfo, instanceInfo, logInfo) {
        let $consoleBox = $(".console-box");
        let $newBox = $consoleBox.first().hide().clone()
            .addClass("available col-lg-6")
            .attr("data-domain-index", domainInfo.index)
            .attr("data-instance-name", instanceInfo.name)
            .attr("data-log-name", logInfo.name);
        $newBox.find(".status-bar h4")
            .text(domainInfo.title + " ›› " + logInfo.file);
        return $newBox.insertAfter($consoleBox.last());
    };
}

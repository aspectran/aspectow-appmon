function FrontBuilder() {

    const endpoints = [];
    const instances = [];
    const viewers = [];
    const clients = [];

    this.build = function (basePath, token, currentEndpoint, joinInstances) {
        $.ajax({
            url: basePath + "backend/" + token + "/config",
            type: 'get',
            dataType: "json",
            data: {
                joinInstances: joinInstances
            },
            success: function (data) {
                if (data) {
                    endpoints.length = 0;
                    instances.length = 0;
                    viewers.length = 0;
                    clients.length = 0;
                    let index = 0;
                    for (let key in data.endpoints) {
                        let endpoint = data.endpoints[key];
                        endpoint['index'] = index;
                        endpoint['basePath'] = basePath;
                        endpoint['token'] = data.token;
                        if (!currentEndpoint || currentEndpoint === endpoint.name) {
                            endpoint.active = true;
                            endpoints.push(endpoint);
                            viewers[index] = new FrontViewer();
                        }
                        index++;
                        console.log('endpoint', endpoint);
                    }
                    for (let key in data.instances) {
                        let instance = data.instances[key];
                        instances.push(instance);
                        console.log('instance', instance);
                    }
                    buildView();
                    if (endpoints.length) {
                        establish(0, joinInstances);
                    }
                }
            }
        });
    };

    const establish = function (endpointIndex, joinInstances) {
        console.log('endpointIndex', endpointIndex);
        function onJoined(endpoint, payload) {
            if (endpoint.established) {
                clearConsole(endpoint.index);
                return;
            }
            if (payload) {
                for (let key in payload.messages) {
                    let msg = payload.messages[key];
                    viewers[endpointIndex].processMessage(msg);
                }
            }
        }
        function onEstablished(endpoint) {
            if (endpoint.established) {
                console.log("Reconnection established");
                return;
            }
            console.log("Connection established");
            endpoint['established'] = true;
            if (endpoint.index < endpoints.length - 1) {
                establish(endpoint.index + 1, joinInstances);
            } else if (endpoint.index === endpoints.length - 1) {
                initView();
                let instanceName = changeInstance();
                if (instanceName && location.hash) {
                    let instanceName2 = location.hash.substring(1);
                    if (instanceName !== instanceName2) {
                        changeInstance(instanceName2);
                    }
                }
            }
        }
        function onFailed(endpoint) {
            setTimeout(function () {
                let client = new PollingClient(endpoint, viewers[endpoint.index], onJoined, onEstablished);
                clients[endpoint.index] = client;
                client.start(joinInstances);
            }, (endpoint.index - 1) * 1000);
        }

        let endpoint = endpoints[endpointIndex];
        let viewer = viewers[endpointIndex];
        let client = new WebsocketClient(endpoint, viewer, onJoined, onEstablished, onFailed);
        clients[endpointIndex] = client;
        client.start(joinInstances);
    };

    const clearView = function () {
        $(".endpoint.tabs .tabs-title.available").remove();
        $(".endpoint.tabs .tabs-title").show();
        $(".display-box.available").remove();
        $(".track-box-box.available").remove();
        $(".sessions-box-box.available").remove();
        $(".console-box.available").remove();
    };

    const clearConsole = function (endpointIndex) {
        $(".console-box[data-endpoint-index=" + endpointIndex + "] .console").empty();
    };

    const changeEndpoint = function (endpointIndex) {
        let availableTabs = $(".endpoint.tabs .tabs-title.available").length;
        if (availableTabs <= 1) {
            return;
        }
        let activeTabs = $(".endpoint.tabs .tabs-title.available.is-active").length;
        let endpoint = endpoints[endpointIndex];
        if (activeTabs === 0) {
            for (let key in endpoints) {
                if (!!endpoints[key].active) {
                    endpoints[key].active = false;
                    viewEndpoint(endpoints[key]);
                }
            }
            endpoint.active = true;
            viewEndpoint(endpoint);
        } else if (activeTabs === 1 && endpoint.active) {
            for (let key in endpoints) {
                if (endpoints[key].index !== endpoint.index) {
                    endpoints[key].active = true;
                    viewEndpoint(endpoints[key]);
                }
            }
        } else if (activeTabs === 1 && !endpoint.active) {
            for (let key in endpoints) {
                if (endpoints[key].index !== endpoint.index) {
                    endpoints[key].active = false;
                    viewEndpoint(endpoints[key]);
                }
            }
            endpoint.active = true;
            viewEndpoint(endpoint);
        } else {
            endpoint.active = !!!endpoint.active;
            viewEndpoint(endpoint);
        }
        let activeEndpoints = 0;
        for (let key in endpoints) {
            if (endpoints[key].active) {
                activeEndpoints++;
            }
        }
        $(".endpoint.tabs .tabs-title.available").removeClass("is-active");
        if (availableTabs > activeEndpoints) {
            for (let key in endpoints) {
                if (endpoints[key].active) {
                    $(".endpoint.tabs .tabs-title[data-endpoint-index=" + endpoints[key].index + "]").addClass("is-active");
                }
            }
        }
    };

    const viewEndpoint = function (endpoint) {
        if (endpoint.active) {
            for (let key in instances) {
                let instance = instances[key];
                if (instance.active) {
                    $(".display-box[data-endpoint-index=" + endpoint.index + "][data-instance-name=" + instance.name + "]").show();
                    $(".console-box[data-endpoint-index=" + endpoint.index + "][data-instance-name=" + instance.name + "]").show();
                }
            }
            viewers[endpoint.index].setVisible(true);
            viewers[endpoint.index].refreshConsole();
        } else {
            viewers[endpoint.index].setVisible(false);
            for (let key in instances) {
                let instance = instances[key];
                if (instance.active) {
                    $(".display-box[data-endpoint-index=" + endpoint.index + "][data-instance-name=" + instance.name + "]").hide();
                    $(".console-box[data-endpoint-index=" + endpoint.index + "][data-instance-name=" + instance.name + "]").hide();
                }
            }
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
                if (!!!instance.active) {
                    instance.active = true;
                    $tabTitle.addClass("is-active");
                    viewEndpointInstance(instanceName);
                }
                exists = true;
            } else {
                instance.active = false;
                $tabTitle.removeClass("is-active");
            }
        }
        if (!exists && instanceName) {
            return changeInstance();
        } else {
            return instanceName;
        }
    }

    const viewEndpointInstance = function (instanceName) {
        for (let key in endpoints) {
            let endpoint = endpoints[key];
            if (endpoint.active) {
                $(".track-box.available[data-endpoint-index=" + endpoint.index + "] .bullet").remove();
                $(".display-box.available[data-endpoint-index=" + endpoint.index + "][data-instance-name!=" + instanceName + "]").hide();
                $(".display-box.available[data-endpoint-index=" + endpoint.index + "][data-instance-name=" + instanceName + "]").show();
                $(".console-box.available[data-endpoint-index=" + endpoint.index + "][data-instance-name!=" + instanceName + "]").hide();
                $(".console-box.available[data-endpoint-index=" + endpoint.index + "][data-instance-name=" + instanceName + "]").show().each(function () {
                    let $console = $(this).find(".console");
                    if (!$console.data("pause")) {
                        viewers[endpoint.index].refreshConsole($console);
                    }
                });
            }
        }
    };

    const initView = function () {
        for (let key in endpoints) {
            let endpoint = endpoints[key];
            if (endpoint.active) {
                viewers[endpoint.index].setVisible(true);
            }
        }
        $("ul.speed-options").hide();
        let pollingMode = false;
        for (let key in endpoints) {
            let endpoint = endpoints[key];
            if (endpoint.mode === "polling") {
                pollingMode = true;
            }
        }
        if (pollingMode) {
            $("ul.speed-options").show();
        } else {
            $("ul.speed-options").hide();
        }
    };

    const buildView = function () {
        clearView();
        for (let key in endpoints) {
            let endpoint = endpoints[key];
            let $titleTab = addEndpointTab(endpoint);
            let $endpointIndicator = $titleTab.find(".indicator");
            viewers[endpoint.index].putIndicator("endpoint", "event", "", $endpointIndicator);
        }
        for (let key in instances) {
            let instance = instances[key];
            let $titleTab = addInstanceTab(instance);
            let $instanceIndicator = $titleTab.find(".indicator");
            for (let key in endpoints) {
                let endpoint = endpoints[key];
                viewers[endpoint.index].putIndicator("instance", "event", instance.name, $instanceIndicator);
                if (instance.events && instance.events.length) {
                    let $displayBox = addDisplayBox(endpoint, instance);
                    for (let key in instance.events) {
                        let event = instance.events[key];
                        if (event.name === "activity") {
                            let $trackBox = addTrackBox($displayBox, endpoint, instance, event);
                            let $activities = $trackBox.find(".activities");
                            viewers[endpoint.index].putDisplay(instance.name, event.name, $trackBox);
                            viewers[endpoint.index].putIndicator(instance.name, "event", event.name, $activities);
                        } else if (event.name === "session") {
                            let $sessionsBox = addSessionsBox($displayBox, endpoint, instance, event);
                            viewers[endpoint.index].putDisplay(instance.name, event.name, $sessionsBox);
                        }
                    }
                }
                for (let key in instance.logs) {
                    let logInfo = instance.logs[key];
                    let $consoleBox = addConsoleBox(endpoint, instance, logInfo);
                    let $console = $consoleBox.find(".console").data("tailing", true);
                    $consoleBox.find(".tailing-status").addClass("on");
                    viewers[endpoint.index].putConsole(instance.name, logInfo.name, $console);
                    let $logIndicator = $consoleBox.find(".status-bar");
                    viewers[endpoint.index].putIndicator(instance.name, "log", logInfo.name, $logIndicator);
                }
            }
        }
        $(".endpoint.tabs .tabs-title.available a").on("click", function() {
            let endpointIndex = $(this).closest(".tabs-title").data("endpoint-index");
            changeEndpoint(endpointIndex);
        });
        $(".instance.tabs .tabs-title.available a").on("click", function() {
            let instanceName = $(this).closest(".tabs-title").data("instance-name");
            changeInstance(instanceName);
        });
        $(".console-box .tailing-switch").on("click", function() {
            let $consoleBox = $(this).closest(".console-box");
            let $console = $consoleBox.find(".console");
            let endpointIndex = $consoleBox.data("endpoint-index");
            if ($console.data("tailing")) {
                $console.data("tailing", false);
                $consoleBox.find(".tailing-status").removeClass("on");
            } else {
                $console.data("tailing", true);
                $(this).find(".tailing-status").addClass("on");
                viewers[endpointIndex].refreshConsole($console);
            }
        });
        $(".console-box .pause-switch").on("click", function() {
            let $console = $(this).closest(".console-box").find(".console");
            if ($console.data("pause")) {
                $console.data("pause", false);
                $(this).removeClass("on");
            } else {
                $console.data("pause", true);
                $(this).addClass("on");
            }
        });
        $(".console-box .clear-screen").on("click", function() {
            let $consoleBox = $(this).closest(".console-box");
            let $console = $consoleBox.find(".console");
            let endpointIndex = $consoleBox.data("endpoint-index");
            viewers[endpointIndex].clearConsole($console);
        });
        $(".layout-options li a").on("click", function() {
            let $li = $(this).parent();
            if (!$li.hasClass("on")) {
                if ($li.hasClass("compact")) {
                    $li.addClass("on");
                    $(".console-box.available").addClass("large-6");
                }
            } else {
                if ($li.hasClass("compact")) {
                    $li.removeClass("on");
                    $(".console-box.available").removeClass("large-6");
                }
            }
        });
        $(".speed-options li").on("click", function() {
            for (let key in endpoints) {
                let endpoint = endpoints[key];
                if (endpoint.mode === "polling") {
                    let $liFast = $(".speed-options li.fast");
                    if ($liFast.hasClass("on")) {
                        $liFast.removeClass("on");
                        clients[endpoint.index].speed(0);
                    } else {
                        $liFast.addClass("on");
                        clients[endpoint.index].speed(1);
                    }
                    break;
                }
            }
        });
    };

    const addEndpointTab = function (endpointInfo) {
        let $tabs = $(".endpoint.tabs");
        let $tab = $tabs.find(".tabs-title").eq(0).hide().clone()
            .addClass("available")
            .attr("data-endpoint-index", endpointInfo.index)
            .attr("data-endpoint-name", endpointInfo.name)
            .attr("data-endpoint-title", endpointInfo.title)
            .attr("data-endpoint-url", endpointInfo.url);
        $tab.find("a .title").text(" " + endpointInfo.title + " ");
        $tab.show().appendTo($tabs);
        return $tab;
    };

    const addInstanceTab = function (instanceInfo) {
        let $tabs = $(".instance.tabs");
        let $tab0 = $tabs.find(".tabs-title").eq(0);
        let $tab = $tab0.hide().clone()
            .addClass("available")
            .attr("data-instance-name", instanceInfo.name)
            .attr("title", instanceInfo.title);
        $tab.find("a .title").text(" " + instanceInfo.title + " ");
        $tab.show().appendTo($tabs);
        return $tab;
    };

    const addDisplayBox = function (endpointInfo, instanceInfo) {
        let $displayBox = $(".display-box");
        let $newBox = $displayBox.eq(0).hide().clone()
            .addClass("available")
            .attr("data-endpoint-index", endpointInfo.index)
            .attr("data-instance-name", instanceInfo.name)
        $newBox.find(".status-bar h4")
            .text(endpointInfo.title);
        return $newBox.insertAfter($displayBox.last()).show();
    };

    const addTrackBox = function ($displayBox, endpointInfo, instanceInfo, eventInfo) {
        let $trackBox = $displayBox.find(".track-box");
        let $newBox = $trackBox.eq(0).hide().clone()
            .addClass("available")
            .attr("data-endpoint-index", endpointInfo.index)
            .attr("data-instance-name", instanceInfo.name)
            .attr("data-event-name", eventInfo.name);
        return $newBox.insertAfter($trackBox.last()).show();
    };

    const addSessionsBox = function ($displayBox, endpointInfo, instanceInfo, eventInfo) {
        let $sessionsBox = $displayBox.find(".sessions-box");
        let $newBox = $sessionsBox.eq(0).hide().clone()
            .addClass("available")
            .attr("data-endpoint-index", endpointInfo.index)
            .attr("data-instance-name", instanceInfo.name)
            .attr("data-event-name", eventInfo.name);
        return $newBox.insertAfter($sessionsBox.last()).show();
    };

    const addConsoleBox = function (endpointInfo, instanceInfo, logInfo) {
        let $consoleBox = $(".console-box");
        let $newBox = $consoleBox.eq(0).hide().clone()
            .addClass("available large-6")
            .attr("data-endpoint-index", endpointInfo.index)
            .attr("data-instance-name", instanceInfo.name)
            .attr("data-log-name", logInfo.name);
        $newBox.find(".status-bar h4")
            .text(endpointInfo.title + " ›› " + logInfo.file);
        return $newBox.insertAfter($consoleBox.last()).show();
    };
}

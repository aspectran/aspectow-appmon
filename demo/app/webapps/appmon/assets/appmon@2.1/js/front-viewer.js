function FrontViewer(sampleInterval) {
    const FLAGS_URL = "https://cdn.jsdelivr.net/gh/aspectran/aspectran-assets/app/webroot/assets/countries/flags/";
    const TEMP_RESIDENT_INACTIVE_SECS = 30;

    let client = null;
    let enable = false;
    let visible = false;
    let $displays = {};
    let $metrics = {};
    let $charts = {};
    let $consoles = {};
    let $indicators = {};
    let prevPosition = 0;
    let currentActivityCounts = {};

    this.setClient = function (newClient) {
        client = newClient;
    }

    this.setEnable = function (flag) {
        enable = !!flag;
        if (enable) {
            resetAllInterimTimers();
        }
    };

    this.setVisible = function (flag) {
        visible = !!flag;
        if (!visible) {
            clearBullets();
        }
    };

    this.putDisplay$ = function (instanceName, eventName, $display) {
        $displays[instanceName + ":event:" + eventName] = $display;
    };

    this.putMetric$ = function (instanceName, metricName, $metric) {
        $metrics[instanceName + ":metric:" + metricName] = $metric;
    };

    this.putChart$ = function (instanceName, eventName, $chart) {
        $charts[instanceName + ":data:" + eventName] = $chart;
    };

    this.putConsole$ = function (instanceName, logName, $console) {
        $consoles[instanceName + ":log:" + logName] = $console;
    };

    this.putIndicator$ = function (instanceName, exporterType, exporterName, $indicator) {
        $indicators[instanceName + ":" + exporterType + ":" + exporterName] = $indicator;
    };

    const getDisplay$ = function (key) {
        return ($displays && key ? $displays[key] : null);
    };

    const getMetric$ = function (key) {
        return ($metrics && key ? $metrics[key] : null);
    };

    const getChart$ = function (key) {
        return ($charts && key ? $charts[key] : null);
    };

    const getConsole$ = function (key) {
        return ($consoles && key ? $consoles[key] : null);
    };

    const getIndicator$ = function (key) {
        return ($indicators && key ? $indicators[key] : null);
    }

    this.refreshConsole = function ($console) {
        if ($console) {
            scrollToBottom($console);
        } else {
            for (let key in $consoles) {
                if (!$consoles[key].data("pause")) {
                    scrollToBottom($consoles[key]);
                }
            }
        }
    };

    this.clearConsole = function ($console) {
        if ($console) {
            $console.empty();
        }
    };

    const scrollToBottom = function ($console) {
        if ($console && $console.data("tailing")) {
            let timer = $console.data("timer");
            if (timer) {
                clearTimeout(timer);
            }
            timer = setTimeout(function () {
                $console.scrollTop($console.prop("scrollHeight"));
                if ($console.find("p").length > 11000) {
                    $console.find("p:gt(10000)").remove();
                }
            }, 300);
            $console.data("timer", timer);
        }
    };

    this.printMessage = function (message, consoleName) {
        if (consoleName) {
            let $console = getConsole$(consoleName);
            $("<p/>").addClass("event ellipses").html(message).appendTo($console);
            scrollToBottom($console);
        } else {
            for (let key in $consoles) {
                this.printMessage(message, key);
            }
        }
    };

    this.printErrorMessage = function (message, consoleName) {
        if (consoleName || !Object.keys($consoles).length) {
            let $console = getConsole$(consoleName);
            $("<p/>").addClass("event error").html(message).appendTo($console);
            scrollToBottom($console);
        } else {
            for (let key in $consoles) {
                this.printErrorMessage(message, key);
            }
        }
    };

    this.processMessage = function (message) {
        let idx1 = message.indexOf(":");
        let idx2 = message.indexOf(":", idx1 + 1);
        let idx3 = message.indexOf(":", idx2 + 1);
        let instanceName = message.substring(0, idx1);
        let exporterType = message.substring(idx1 + 1, idx2);
        let exporterName = message.substring(idx2 + 1, idx3);
        let messagePrefix = message.substring(0, idx3);
        let messageText = message.substring(idx3 + 1);
        switch (exporterType) {
            case "event":
                if (messageText.length) {
                    let eventData = JSON.parse(messageText);
                    processEventData(instanceName, exporterType, exporterName, messagePrefix, eventData);
                }
                break;
            case "data":
                if (messageText.length) {
                    let eventData = JSON.parse(messageText);
                    if (eventData.chartData) {
                        processChartData(instanceName, exporterType, exporterName, messagePrefix, eventData.chartData);
                    }
                }
                break;
            case "metric":
                if (messageText.length) {
                    let metricData = JSON.parse(messageText);
                    processMetricData(instanceName, exporterType, exporterName, messagePrefix, metricData);
                }
                break;
            case "log":
                printLogMessage(instanceName, exporterType, exporterName, messagePrefix, messageText);
                break;
        }
    };

    const printLogMessage = function (instanceName, exporterType, logName , messagePrefix, messageText) {
        indicate(instanceName, exporterType, logName);
        let $console = getConsole$(messagePrefix);
        if ($console && !$console.data("pause")) {
            $("<p/>").text(messageText).appendTo($console);
            scrollToBottom($console);
        }
    };

    const processEventData = function (instanceName, exporterType, eventName, messagePrefix, eventData) {
        switch (eventName) {
            case "activity":
                indicate(instanceName, exporterType, eventName);
                if (eventData.activities) {
                    printActivityStatus(messagePrefix, eventData.activities);
                }
                if (visible) {
                    let $track = getDisplay$(messagePrefix);
                    if ($track) {
                        let varName = messagePrefix.replace(':', '_');
                        if (!currentActivityCounts[varName]) {
                            currentActivityCounts[varName] = 0;
                            printCurrentActivityCount(messagePrefix, 0);
                        }
                        launchBullet($track, eventData, function () {
                            currentActivityCounts[varName]++;
                            printCurrentActivityCount(messagePrefix, currentActivityCounts[varName]);
                        }, function () {
                            if (currentActivityCounts[varName] > 0) {
                                currentActivityCounts[varName]--;
                            }
                            printCurrentActivityCount(messagePrefix, currentActivityCounts[varName]);
                        });
                    }
                } else {
                    printCurrentActivityCount(messagePrefix, 0);
                }
                updateActivityCount(
                    instanceName + ":" + exporterType + ":session",
                    eventData.sessionId,
                    eventData.activityCount||0);
                break;
            case "session":
                printSessionEventData(messagePrefix, eventData);
                break;
        }
    }

    const processMetricData = function (instanceName, exporterType, metricName, messagePrefix, metricData) {
        let $metric = getMetric$(messagePrefix);
        if ($metric) {
            let formatted = metricData.format;
            for (let key in metricData.data) {
                formatted = formatted.replace("{" + key + "}", metricData.data[key]);
            }
            $metric.find("dd")
                .text(formatted)
                .attr("title", JSON.stringify(metricData.data, null, 2));
        }
    }

    const launchBullet = function ($track, eventData, onLeaving, onArriving) {
        if (eventData.elapsedTime) {
            if (onLeaving) {
                onLeaving();
            }
            let position = generateRandom(3, 103);
            if (prevPosition) {
                if (Math.abs(position - prevPosition) <= 20) {
                    position = generateRandom(3, 103);
                    if (Math.abs(position - prevPosition) <= 20) {
                        position = generateRandom(3, 103);
                    }
                }
            }
            prevPosition = position;
            let $bullet = $("<div class='bullet'/>")
                .attr("sessionId", eventData.sessionId)
                .css("top", position + "px")
                .appendTo($track).show();
            if (eventData.error) {
                $bullet.addClass("error");
            }
            setTimeout(function () {
                $bullet.addClass("arrive");
                setTimeout(function () {
                    $bullet.fadeOut(1000);
                    setTimeout(function () {
                        $bullet.remove();
                        if (onArriving) {
                            onArriving();
                        }
                    }, 500);
                }, eventData.elapsedTime + 350);
            }, 900);
        }
    };

    const clearBullets = function () {
        for (let key in $displays) {
            if ($displays[key].hasClass("track-box")) {
                $displays[key].find(".bullet").remove();
            }
        }
    }

    const generateRandom = function (min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    };

    const indicate = function (instanceName, exporterType, exporterName) {
        let $indicator1 = getIndicator$("domain:event:");
        blink($indicator1);
        if (visible) {
            let $indicator2 = getIndicator$("instance:event:" + instanceName);
            blink($indicator2);
            if (exporterType === "log") {
                let $indicator3 = getIndicator$(instanceName + ":log:" + exporterName);
                blink($indicator3);
            }
        }
    };

    const blink = function ($indicator) {
        if ($indicator && !$indicator.hasClass("on")) {
            $indicator.addClass("blink on");
            setTimeout(function () {
                $indicator.removeClass("blink on");
            }, 500);
        }
    }

    const printActivityStatus = function (messagePrefix, activities) {
        let $activityStatus = getIndicator$(messagePrefix);
        if ($activityStatus) {
            let separator = (activities.errors > 0 ? " / " : (activities.interim > 0 ? "+" : "-"));
            $activityStatus.find(".interim .separator").text(separator);
            $activityStatus.find(".interim .total").text(activities.interim > 0 ? activities.interim : "");
            $activityStatus.find(".interim .errors").text(activities.errors > 0 ? activities.errors : "");
            $activityStatus.find(".cumulative .total").text(activities.total);
        }
    }

    const resetInterimActivityStatus = function (messagePrefix) {
        let $activityStatus = getIndicator$(messagePrefix);
        if ($activityStatus) {
            $activityStatus.find(".interim .separator").text("");
            $activityStatus.find(".interim .total").text(0);
            $activityStatus.find(".interim .errors").text("");
        }
    }

    const resetInterimTimer = function (messagePrefix) {
        if (sampleInterval) {
            let $activityStatus = getIndicator$(messagePrefix);
            if ($activityStatus) {
                let $samplingTimerBar = $activityStatus.find(".sampling-timer-bar");
                let $samplingTimerStatus = $activityStatus.find(".sampling-timer-status");
                if ($samplingTimerBar.length) {
                    let timer = $samplingTimerBar.data("timer");
                    if (timer) {
                        clearInterval(timer);
                        $samplingTimerBar.removeData("timer");
                    }
                    let second = (dayjs().minute() * 60 + dayjs().second()) % sampleInterval;
                    $samplingTimerBar.animate({height: 0}, 600);
                    $samplingTimerBar.animate({height: (second++ / sampleInterval * 100).toFixed(2) + "%"}, 400);
                    $samplingTimerStatus.text(second + "/" + sampleInterval);
                    timer = setInterval(function () {
                        if (!enable) {
                            clearInterval(timer);
                            $samplingTimerBar.removeData("timer");
                            return;
                        }
                        let percent = second++ / sampleInterval * 100;
                        $samplingTimerBar.css("height", percent.toFixed(2) + "%");
                        $samplingTimerStatus.text(second + "/" + sampleInterval);
                        if (second > 300) {
                            second = 0;
                        } else if (second % 10 === 0) {
                            second = (dayjs().minute() * 60 + dayjs().second()) % sampleInterval;
                        }
                    }, 1000);
                    $samplingTimerBar.data("timer", timer);
                }
            }
        }
    }

    const resetAllInterimTimers = function () {
        for (let key in $indicators) {
            let $activityStatus = getIndicator$(key);
            if ($activityStatus.hasClass("activity-status")) {
                resetInterimTimer(key);
            }
        }
    }

    const printCurrentActivityCount = function (messagePrefix, count) {
        let $activityStatus = getIndicator$(messagePrefix);
        if ($activityStatus) {
            $activityStatus.find(".current .total").text(count);
        }
    }

    const printSessionEventData = function (messagePrefix, eventData) {
        let $display = getDisplay$(messagePrefix);
        if ($display) {
            $display.find(".numberOfCreated").text(eventData.numberOfCreated);
            $display.find(".numberOfExpired").text(eventData.numberOfExpired);
            $display.find(".numberOfActives").text(eventData.numberOfActives);
            $display.find(".highestNumberOfActives").text(eventData.highestNumberOfActives);
            $display.find(".numberOfUnmanaged").text(eventData.numberOfUnmanaged);
            $display.find(".numberOfRejected").text(eventData.numberOfRejected);
            if (eventData.startTime) {
                $display.find(".startTime").text(dayjs.utc(eventData.startTime).local().format("LLL"));
            }
            let $sessions = $display.find("ul.sessions");
            if (eventData.createdSessions) {
                eventData.createdSessions.forEach(function (session) {
                    addSession($sessions, session);
                });
            }
            if (eventData.destroyedSessions) {
                eventData.destroyedSessions.forEach(function (sessionId) {
                    $sessions.find("li[data-sid='" + sessionId + "']").remove();
                });
            }
            if (eventData.evictedSessions) {
                eventData.evictedSessions.forEach(function (sessionId) {
                    let $item = $sessions.find("li[data-sid='" + sessionId + "']");
                    if (!$item.hasClass("inactive")) {
                        $item.addClass("inactive");
                        let inactiveInterval = Math.min($item.data("inactive-interval")||TEMP_RESIDENT_INACTIVE_SECS, TEMP_RESIDENT_INACTIVE_SECS);
                        setTimeout(function () {
                            $item.remove();
                        }, inactiveInterval * 1000);
                    }
                });
            }
            if (eventData.residedSessions) {
                eventData.residedSessions.forEach(function (session) {
                    addSession($sessions, session);
                });
            }
        }
    };

    const addSession = function ($sessions, session) {
        $sessions.find("li[data-sid='" + session.sessionId + "']").each(function () {
            let timer = $(this).data("timer");
            if (timer) {
                clearTimeout(timer);
            }
        }).remove();
        let $count = $("<div class='count'></div>").text(session.activityCount||0);
        if (session.activityCount > 1) {
            $count.addClass("counting");
        }
        if (session.username) {
            $count.addClass("active");
        }
        let $li = $("<li/>")
            .attr("data-sid", session.sessionId)
            .attr("data-inactive-interval", session.inactiveInterval)
            .append($count);
        if (session.tempResident) {
            $li.addClass("inactive");
            let inactiveInterval = Math.min(session.inactiveInterval||30, 30);
            let timer = setTimeout(function () {
                $li.remove();
            }, inactiveInterval * 1000);
            $li.data("timer", timer);
        }
        if (session.countryCode) {
            $("<img class='flag' alt=''/>")
                .attr("src", FLAGS_URL + session.countryCode.toLowerCase() + ".png")
                .attr("alt", session.countryCode)
                .attr("title", countries[session.countryCode].name)
                .appendTo($li);
        }
        if (session.username) {
            $("<div class='username'/>")
                .text(session.username)
                .appendTo($li);
        }
        let $detail = $("<div class='detail'/>")
            .append($("<p/>").text(session.sessionId))
            .append($("<p/>").text(dayjs.utc(session.createAt).local().format("LLL")));
        if (session.ipAddress) {
            $detail.append($("<p/>").text(session.ipAddress));
        }
        $detail.appendTo($li);
        if (session.tempResident) {
            $li.appendTo($sessions);
        } else {
            $li.prependTo($sessions);
        }
    };

    const updateActivityCount = function (messagePrefix, sessionId, activityCount) {
        let $display = getDisplay$(messagePrefix);
        if ($display) {
            let $li = $display.find("ul.sessions li[data-sid='" + sessionId + "']");
            let $count = $li.find(".count").text(activityCount);
            if (activityCount > 1) {
                $count.addClass("counting");
            }
            $li.stop().hide().fadeIn(250);
        }
    };

    const processChartData = function (instanceName, exporterType, eventName, messagePrefix, chartData) {
        let $chart = getChart$(messagePrefix);
        if (!$chart) {
            return;
        }
        let chart = $chart.data("chart");
        if (eventName === "activity") {
            let prefix = instanceName + ":event:" + eventName;
            if (!chart) {
                resetInterimTimer(prefix);
            } else if (chartData.rolledUp) {
                resetInterimTimer(prefix);
                resetInterimActivityStatus(prefix);
            }
        }
        let dateUnit = (chartData.rolledUp ? $chart.data("dateUnit") : chartData.dateUnit);
        let dateOffset = (chartData.rolledUp ? $chart.data("dateOffset") : chartData.dateOffset);
        let labels = chartData.labels;
        let data1 = chartData.data1;
        let data2 = chartData.data2.map(n => (eventName === "activity" ? n : null));
        if (!chart || !chartData.rolledUp) {
            if (chart) {
                chart.destroy();
            }
            let $canvas = $chart.find("canvas");
            if (!$canvas.length) {
                $canvas = $("<canvas/>");
                $canvas.appendTo($chart);
            }
            let maxLabels = adjustLabelCount(eventName, labels, data1, data2);
            let autoSkip = (maxLabels === 0);
            let newChart = drawChart(eventName, $canvas[0], dateUnit, labels, data1, data2, autoSkip);
            $chart.data("chart", newChart);
            if (dateUnit) {
                $chart.data("dateUnit", dateUnit);
            } else {
                $chart.removeData("dateUnit");
            }
            if (dateOffset) {
                $chart.data("dateOffset", dateOffset);
            } else {
                $chart.removeData("dateOffset");
            }
        } else if (!dateOffset) {
            if (!dateUnit) {
                updateChartAfterRolledUp(eventName, chart, labels, data1, data2);
            } else if (client) {
                setTimeout(function () {
                    let options = [];
                    options.push("instance:" + instanceName);
                    options.push("dateUnit:" + dateUnit);
                    client.refresh(options);
                }, 900);
            }
        }
    };

    const updateChartAfterRolledUp = function (eventName, chart, labels, data1, data2) {
        if (chart.data.labels.length > 0) {
            let lastIndex = chart.data.labels.length - 1;
            if (chart.data.labels[lastIndex] >= labels[0]) {
                chart.data.labels.splice(lastIndex, lastIndex + 1);
                chart.data.datasets[0].data.splice(lastIndex, lastIndex + 1);
                chart.data.datasets[1].data.splice(lastIndex, lastIndex + 1);
            }
        }
        chart.data.labels.push(...labels);
        chart.data.datasets[0].data.push(...data1);
        chart.data.datasets[1].data.push(...data2);
        adjustLabelCount(eventName, chart.data.labels, chart.data.datasets[0].data, chart.data.datasets[1].data);
        chart.update();
    };

    const adjustLabelCount = function (eventName, labels, data1, data2) {
        let canvasWidth = 0;
        for (let key in $charts) {
            if (key.endsWith(":" + eventName)) {
                let $chart = $charts[key];
                if ($chart) {
                    canvasWidth = $chart.find("canvas").width();
                    if (canvasWidth > 0) {
                        canvasWidth -= 90;
                        break;
                    }
                }
            }
        }
        let maxLabels = (canvasWidth > 0 ? Math.floor(canvasWidth / 21) : 0);
        if (maxLabels > 0) {
            let cnt = labels.length - maxLabels;
            if (cnt > 0) {
                labels.splice(0, cnt);
                data1.splice(0, cnt);
                data2.splice(0, cnt);
            }
        }
        return maxLabels;
    };

    this.getMaxStartDatetime = function (instanceName) {
        let result = "";
        for (let key in $charts) {
            if (key.startsWith(instanceName + ":")) {
                let $chart = $charts[key];
                let chart = $chart.data("chart");
                if (chart) {
                    let labels = chart.data.labels;
                    if (labels.length) {
                        if (labels[0] > result) {
                            result = labels[0];
                        }
                    }
                }
            }
        }
        return result;
    }

    const drawChart = function (eventName, canvas, dateUnit, labels, data1, data2, autoSkip) {
        let dataLabel1;
        let borderColor1;
        let backgroundColor1;
        switch (eventName) {
            case "activity":
                dataLabel1 = "Activities";
                borderColor1 = "#4493c8";
                backgroundColor1 = "#cce0fa";
                break;
            case "session":
                dataLabel1 = "Sessions";
                borderColor1 = "#44c577";
                backgroundColor1 = "#bcefd0";
                break;
            default:
                dataLabel1 = "";
        }
        const chartType = (!dateUnit ? "line" : "bar");
        const chart = new Chart(
            canvas,
            {
                type: chartType,
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: false,
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            enabled: true,
                            reverse: true,
                            mode: 'x',
                            intersect: false,
                            callbacks: {
                                title: function (tooltip) {
                                    let datetime = dayjs.utc(labels[tooltip[0].dataIndex], "YYYYMMDDHHmm").local();
                                    switch (dateUnit) {
                                        case "hour":
                                            return datetime.format("LL HH:00");
                                        case "day":
                                            return datetime.format("LL");
                                        case "month":
                                            return datetime.date(1).format("LL");
                                        case "year":
                                            return datetime.format("YYYY");
                                        default:
                                            return datetime.format("LLL");
                                    }
                                }
                            }
                        },
                        zoom: {
                            zoom: {
                                wheel: {
                                    enabled: false
                                },
                                pinch: {
                                    enabled: true
                                },
                                drag: {
                                    enabled: true,
                                    threshold: 21,
                                    backgroundColor: "rgba(225,225,225,0.35)",
                                    borderColor: "rgba(225,225,225)",
                                    borderWidth: 1
                                },
                                mode: "x",
                                onZoomComplete: function () {
                                    if (chart.isZoomedOrPanned()) {
                                        $(canvas).parent().find(".reset-zoom")
                                            .off("click")
                                            .on("click", function () {
                                                chart.resetZoom();
                                            }).show();
                                    } else {
                                        $(canvas).parent().find(".reset-zoom").hide();
                                    }
                                }
                            },
                            pan: {
                                enabled: true,
                                mode: "x",
                                modifierKey: "ctrl"
                            }
                        },
                        crosshair: {
                            line: {
                                color: "#ccc",
                                width: 1
                            },
                            zoom: {
                                enabled: false
                            }
                        }
                    },
                    scales: {
                        x: {
                            display: true,
                            title: {
                                display: false
                            },
                            ticks: {
                                autoSkip: autoSkip,
                                includeBounds: false,
                                callback: function (value, index) {
                                    let datetime =  dayjs.utc(labels[value], "YYYYMMDDHHmm").local();
                                    let datetime2 = (value > 0 ? dayjs.utc(labels[value - 1], "YYYYMMDDHHmm").local() : null);
                                    switch (dateUnit) {
                                        case "hour":
                                            if (index === 0 || datetime2 && datetime.isAfter(datetime2, "day")) {
                                                return datetime.format("M/D HH:00");
                                            } else {
                                                return datetime.format("HH:00");
                                            }
                                        case "day":
                                            if (index === 0 || datetime2 && datetime.isAfter(datetime2, "year")) {
                                                return datetime.format("YYYY M/D");
                                            } else {
                                                return datetime.format("M/D");
                                            }
                                        case "month":
                                            return datetime.format("YYYY/M");
                                        case "year":
                                            return datetime.format("YYYY");
                                        default:
                                            if (index === 0 || datetime2 && datetime.isAfter(datetime2, "day")) {
                                                return datetime.format("M/D HH:mm");
                                            } else {
                                                return datetime.format("HH:mm");
                                            }
                                    }
                                }
                            },
                            tooltip: {
                                enabled: true
                            },
                            stacked: true,
                            grid: chartType === "line" ? {
                                color: function (ctx) {
                                    return (data2[ctx.tick.value] > 0 ? "#ff6384" : "#e4e4e4");
                                }
                            } : {}
                        },
                        y: {
                            display: true,
                            title: {
                                display: true,
                                text: dataLabel1
                            },
                            suggestedMin: 0,
                            suggestedMax: 5,
                            stacked: true,
                            grid: {
                                color: "#e4e4e4"
                            }
                        }
                    }
                },
                data: {
                    labels: labels,
                    datasets: [
                        chartType === "line" ? {
                            label: dataLabel1,
                            data: data1,
                            fill: true,
                            borderColor: borderColor1,
                            backgroundColor: backgroundColor1,
                            borderWidth: 1.4,
                            tension: 0.1,
                            pointStyle: false,
                            order: 2
                        } : {
                            label: dataLabel1,
                            data: data1,
                            minBarLength: 2,
                            fill: true,
                            borderWidth: 1,
                            borderColor: borderColor1,
                            backgroundColor: borderColor1,
                            order: 2
                        },
                        {
                            label: "Errors",
                            data: data2,
                            type: chartType,
                            fill: true,
                            borderWidth: 1,
                            borderColor: "#ff6384",
                            backgroundColor: "#ff6384",
                            showLine: false,
                            pointStyle: false,
                            order: 1
                        }
                    ]
                }
            }
        );
        if (!chart.isZoomedOrPanned()) {
            $(canvas).parent().find(".reset-zoom").hide();
        }
        return chart;
    };
}

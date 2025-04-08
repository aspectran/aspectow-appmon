function FrontViewer() {
    const FLAGS_URL = "https://aspectran.com/assets/countries/flags/";
    const TEMP_RESIDENT_INACTIVE_SECS = 30;

    let $displays = {};
    let $charts = {};
    let $consoles = {};
    let $indicators = {};
    let visible = false;
    let prevPosition = 0;
    let currentActivities = [];

    this.putDisplay = function (instanceName, eventName, $display) {
        $displays[instanceName + ":event:" + eventName] = $display;
    };

    this.putChart = function (instanceName, eventName, $chart) {
        $charts[instanceName + ":data:" + eventName] = $chart;
    };

    this.putConsole = function (instanceName, logName, $console) {
        $consoles[instanceName + ":log:" + logName] = $console;
    };

    this.putIndicator = function (instanceName, messageType, nameOfEventOrLog, $indicator) {
        $indicators[instanceName + ":" + messageType + ":" + nameOfEventOrLog] = $indicator;
    };

    const getDisplay = function (key) {
        return ($displays && key ? $displays[key] : null);
    };

    const getChart = function (key) {
        return ($charts && key ? $charts[key] : null);
    };

    const getConsole = function (key) {
        return ($consoles && key ? $consoles[key] : null);
    };

    const getIndicator = function (key) {
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

    this.setVisible = function (flag) {
        visible = !!flag;
        if (!visible) {
            for (let key in $displays) {
                if ($displays[key].hasClass("track-box")) {
                    $displays[key].find(".bullet").remove();
                }
            }
        }
    };

    this.printMessage = function (message, consoleName) {
        if (consoleName) {
            let $console = getConsole(consoleName);
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
            let $console = getConsole(consoleName);
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
        let messageType = message.substring(idx1 + 1, idx2);
        let nameOfEventOrLog = message.substring(idx2 + 1, idx3);
        let messagePrefix = message.substring(0, idx3);
        let messageText = message.substring(idx3 + 1);
        switch (messageType) {
            case "event":
                if (messageText.length) {
                    let eventData = JSON.parse(messageText);
                    processEventData(instanceName, messageType, nameOfEventOrLog, messagePrefix, eventData);
                }
                break;
            case "data":
                if (messageText.length) {
                    let eventData = JSON.parse(messageText);
                    if (eventData.chartData) {
                        processChartData(instanceName, messageType, nameOfEventOrLog, messagePrefix, eventData.chartData);
                    }
                }
                break;
            case "log":
                printLogMessage(instanceName, messageType, nameOfEventOrLog, messagePrefix, messageText);
                break;
        }
    };

    const printLogMessage = function (instanceName, messageType, logName , messagePrefix, messageText) {
        indicate(instanceName, messageType, logName);
        let $console = getConsole(messagePrefix);
        if ($console && !$console.data("pause")) {
            $("<p/>").text(messageText).appendTo($console);
            scrollToBottom($console);
        }
    };

    const processEventData = function (instanceName, messageType, eventName, messagePrefix, eventData) {
        switch (eventName) {
            case "activity":
                indicate(instanceName, messageType, eventName);
                if (eventData.activities) {
                    printActivities(messagePrefix, eventData.activities);
                }
                if (visible) {
                    let $track = getDisplay(messagePrefix);
                    if ($track) {
                        let varName = messagePrefix.replace(':', '_');
                        if (!currentActivities[varName]) {
                            currentActivities[varName] = 0;
                            printCurrentActivities(messagePrefix, 0);
                        }
                        launchBullet($track, eventData, function () {
                            currentActivities[varName]++;
                            printCurrentActivities(messagePrefix, currentActivities[varName]);
                        }, function () {
                            if (currentActivities[varName] > 0) {
                                currentActivities[varName]--;
                            }
                            printCurrentActivities(messagePrefix, currentActivities[varName]);
                        });
                    }
                } else {
                    printCurrentActivities(messagePrefix, 0);
                }
                updateActivityCount(
                    instanceName + ":" + messageType + ":session",
                    eventData.sessionId,
                    eventData.activityCount||0);
                break;
            case "session":
                printSessionEventData(messagePrefix, eventData);
                break;
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

    const generateRandom = function (min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    };

    const indicate = function (instanceName, messageType, nameOfEventOrLog) {
        let $indicator1 = getIndicator("domain:event:");
        blink($indicator1);
        if (visible) {
            let $indicator2 = getIndicator("instance:event:" + instanceName);
            blink($indicator2);
            if (messageType === "log") {
                let $indicator3 = getIndicator(instanceName + ":log:" + nameOfEventOrLog);
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

    const printActivities = function (messagePrefix, activities) {
        let $activities = getIndicator(messagePrefix);
        if ($activities) {
            $activities.find(".tallied").text(activities.tallied > 0 ? "+" + activities.tallied : "-");
            $activities.find(".total").text(activities.total);
        }
    }

    const resetTalliedActivities = function (messagePrefix) {
        let $activities = getIndicator(messagePrefix);
        if ($activities) {
            $activities.find(".tallied").text(0);
        }
    }

    const printCurrentActivities = function (messagePrefix, current) {
        let $activities = getIndicator(messagePrefix);
        if ($activities) {
            $activities.find(".current").text(current);
        }
    }

    const printSessionEventData = function (messagePrefix, eventData) {
        let $display = getDisplay(messagePrefix);
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
        if (session.activityCount > 0) {
            $count.addClass("counting");
        }
        if (session.username) {
            $count.addClass("active");
        }
        let $li = $("<li/>")
            .attr("data-sid", session.sessionId)
            .attr("data-temp-resident", session.tempResident)
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
        let $display = getDisplay(messagePrefix);
        if ($display) {
            let $li = $display.find("ul.sessions li[data-sid='" + sessionId + "']");
            let $count = $li.find(".count").text(activityCount);
            if (activityCount > 0) {
                $count.addClass("counting");
            }
            $li.stop().hide().fadeIn(250);
        }
    };

    const processChartData = function (instanceName, messageType, eventName, messagePrefix, chartData) {
        let $chart = getChart(messagePrefix);
        if (!$chart) {
            return;
        }
        if (eventName === "activity" && chartData.rolledUp) {
            resetTalliedActivities(instanceName + ":event:" + eventName);
        }
        let labels = chartData.labels;
        let data1 = chartData.data1;
        let data2 = chartData.data2.map(n => (eventName === "activity" ? n : null));
        let chart = $chart.data("chart");
        if (chart) {
            updateChart(eventName, chart, toDatetime(labels), data1, data2);
        } else {
            let $canvas = $chart.find("canvas");
            if (!$canvas.length) {
                $canvas = $("<canvas/>");
                $canvas.appendTo($chart);
            }
            let maxLabels = adjustLabelCount(eventName, labels, data1, data2);
            let autoSkip = (maxLabels === 0);
            let chart = drawChart(eventName, $canvas[0], toDatetime(labels), data1, data2, autoSkip);
            $chart.data("chart", chart);
        }
    };

    const updateChart = function (eventName, chart, labels, data1, data2) {
        if (chart.data.labels.length > 0) {
            if (labels.length > 1) {
                chart.data.labels.length = 0;
                chart.data.datasets[0].data.length = 0;
                chart.data.datasets[1].data.length = 0;
            } else if (labels.length === 1) {
                let lastIndex = chart.data.labels.length - 1;
                if (chart.data.labels[lastIndex] >= labels[0]) {
                    chart.data.labels.splice(lastIndex, 1);
                    chart.data.datasets[0].data.splice(lastIndex, 1);
                    chart.data.datasets[1].data.splice(lastIndex, 1);
                }
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
        if (maxLabels > 0 && labels.length > maxLabels) {
            let cnt = labels.length - maxLabels;
            labels.splice(0, cnt);
            data1.splice(0, cnt);
            data2.splice(0, cnt);
        }
        return maxLabels;
    };

    const toDatetime = function (labels) {
        let arr = []
        labels.forEach(label => {
            arr.push(dayjs.utc(label, "YYYYMMDDHHmm").local());
        });
        return arr;
    };

    const drawChart = function (eventName, canvas, labels, data1, data2, autoSkip) {
        let dataLabel1;
        let borderColor1;
        let backgroundColor1;
        switch (eventName) {
            case "activity":
                dataLabel1 = "Activities";
                borderColor1 = "#5267d1";
                backgroundColor1 = "#ccd7fa";
                break;
            case "session":
                dataLabel1 = "Sessions";
                borderColor1 = "#476b80";
                backgroundColor1 = "#cbe3f4";
                break;
            default:
                dataLabel1 = "";
        }
        return new Chart(
            canvas,
            {
                type: 'line',
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
                            callbacks: {
                                title: function (tooltip) {
                                    return labels[tooltip[0].dataIndex].format("LLL");
                                }
                            },
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
                                callback: function (value, index) {
                                    let datetime = labels[index];
                                    let datetime2 = (index > 0 ? labels[index - 1] : null);
                                    if (datetime.isAfter(datetime2, 'day')) {
                                        return datetime.format("M/D HH:mm");
                                    } else {
                                        return datetime.format("HH:mm");
                                    }
                                }
                            },
                            tooltip: {
                                enabled: true,
                            },
                            grid: {
                                color: function (context) {
                                    return (data2[context.tick.value] > 0 ? "#fd5b5b" : "#e4e4e4");
                                },
                            },

                        },
                        y: {
                            display: true,
                            title: {
                                display: true,
                                text: dataLabel1
                            },
                            suggestedMin: 0,
                            suggestedMax: 5,
                            grid: {
                                color: "#e4e4e4"
                            }
                        }
                    }
                },
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: dataLabel1,
                            data: data1,
                            fill: true,
                            borderColor: borderColor1,
                            backgroundColor: backgroundColor1,
                            borderWidth: 1.4,
                            tension: 0.1,
                            pointStyle: false,
                            order: 2
                        },
                        {
                            label: "Errors",
                            data: data2,
                            type: "line",
                            fill: true,
                            backgroundColor: "#f83636",
                            borderWidth: 0,
                            tension: 0.1,
                            pointStyle: false,
                            order: 1
                        }
                    ]
                }
            }
        );
    };
}

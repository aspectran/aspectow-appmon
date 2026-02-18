/**
 * The viewer component for the AppMon dashboard.
 * Responsible for rendering monitoring data, including logs, metrics, and charts.
 */
class DashboardViewer {
    constructor(sampleInterval) {
        this.flagsUrl = "https://cdn.jsdelivr.net/gh/aspectran/aspectran-assets/app/webroot/assets/countries/flags/";
        this.tempResidentInactiveSecs = 30;
        this.sampleInterval = sampleInterval;

        this.client = null;
        this.enable = false;
        this.visible = false;
        this.displays = {};
        this.metrics = {};
        this.charts = {};
        this.consoles = {};
        this.indicators = {};
        this.currentActivityCounts = {};
        this.cachedCanvasWidth = 0;
        this.activeBulletCount = 0;
        this.maxBullets = 500;
        this.painters = {};
    }

    setClient(client) {
        this.client = client;
    }

    setEnable(flag) {
        this.enable = !!flag;
        if (this.enable) {
            this.resetAllInterimTimers();
        }
    }

    setVisible(flag) {
        this.visible = !!flag;
        if (!this.visible) {
            this.clearBullets();
        }
    }

    putDisplay$(instanceName, eventName, $display) {
        const key = instanceName + ":event:" + eventName;
        this.displays[key] = $display;
        if ($display.hasClass("track-box")) {
            const canvas = $display.find(".traffic-canvas")[0];
            if (canvas) {
                this.painters[key] = new TrafficPainter(canvas);
            }
        }
    }

    putMetric$(instanceName, metricName, $metric) {
        this.metrics[instanceName + ":metric:" + metricName] = $metric;
    }

    putChart$(instanceName, eventName, $chart) {
        const key = instanceName + ":data:" + eventName;
        this.charts[key] = new DashboardChart($chart, eventName);
    }

    putConsole$(instanceName, logName, $console) {
        this.consoles[instanceName + ":log:" + logName] = $console;
    }

    putIndicator$(instanceName, exporterType, exporterName, $indicator) {
        this.indicators[instanceName + ":" + exporterType + ":" + exporterName] = $indicator;
    }

    getDisplay$(key) {
        return this.displays[key] || null;
    }

    getMetric$(key) {
        return this.metrics[key] || null;
    }

    getChart$(key) {
        return this.charts[key] || null;
    }

    getConsole$(key) {
        return this.consoles[key] || null;
    }

    getIndicator$(key) {
        return this.indicators[key] || null;
    }

    updateCanvasWidth() {
        this.cachedCanvasWidth = 0;
    }

    resetCurrentActivityCounts() {
        this.currentActivityCounts = {};
        for (let key in this.indicators) {
            if (key.includes(":event:activity")) {
                this.printCurrentActivityCount(key, 0);
            }
        }
        this.clearBullets();
    }

    clearAllSessions() {
        for (let key in this.displays) {
            if (key.includes(":event:session")) {
                const $sessions = this.displays[key].find("ul.sessions");
                $sessions.find("li").each(function () {
                    const timer = $(this).data("timer");
                    if (timer) clearTimeout(timer);
                });
                $sessions.empty();
            }
        }
    }

    setLoading(instanceName, isLoading) {
        for (let key in this.charts) {
            if (key.startsWith(instanceName + ":")) {
                const dashboardChart = this.charts[key];
                const $chartBox = dashboardChart.$container.closest(".chart-box");
                const $overlay = $chartBox.find(".loading-overlay");
                if (isLoading) {
                    $overlay.css("display", "flex");
                } else {
                    $overlay.hide();
                }
            }
        }
    }

    refreshConsole($console) {
        if ($console) {
            this.scrollToBottom($console);
        } else {
            for (let key in this.consoles) {
                if (!this.consoles[key].data("pause")) {
                    this.scrollToBottom(this.consoles[key]);
                }
            }
        }
    }

    clearConsole($console) {
        if ($console) {
            $console.empty();
        }
    }

    scrollToBottom($console) {
        if (!$console) return;
        let timer = $console.data("timer");
        if (timer) {
            clearTimeout(timer);
        }
        timer = setTimeout(() => {
            const el = $console[0];
            if (!el) return;

            // Process Buffered Messages
            const buffer = $console.data("log-buffer");
            if (buffer && buffer.length > 0) {
                const fragment = document.createDocumentFragment();
                while (buffer.length > 0) {
                    const item = buffer.shift();
                    const p = document.createElement("p");
                    if (typeof item === "string") {
                        p.textContent = item;
                    } else {
                        if (item.html) p.innerHTML = item.html;
                        else p.textContent = item.text;
                        if (item.className) p.className = item.className;
                    }
                    fragment.appendChild(p);
                }
                el.appendChild(fragment);
            }

            // Scroll to bottom if tailing
            if ($console.data("tailing")) {
                el.scrollTop = el.scrollHeight;
            }

            // Truncate old messages
            const pList = el.getElementsByTagName("p");
            if (pList.length > 11000) {
                const removeCount = pList.length - 10000;
                for (let i = 0; i < removeCount; i++) {
                    el.removeChild(pList[0]);
                }
            }
        }, 300);
        $console.data("timer", timer);
    }

    prependToConsole($console, noAnchoring) {
        if (!$console) return;
        let timer = $console.data("prev-timer");
        if (timer) {
            clearTimeout(timer);
        }
        timer = setTimeout(() => {
            const el = $console[0];
            if (!el) return;

            const buffer = $console.data("log-prev-buffer");
            if (buffer && buffer.length > 0) {
                const oldScrollHeight = el.scrollHeight;
                const oldScrollTop = el.scrollTop;

                const fragment = document.createDocumentFragment();
                while (buffer.length > 0) {
                    const item = buffer.shift();
                    const p = document.createElement("p");
                    if (typeof item === "string") {
                        p.textContent = item;
                    } else {
                        if (item.html) p.innerHTML = item.html;
                        else p.textContent = item.text;
                        if (item.className) p.className = item.className;
                    }
                    fragment.appendChild(p);
                }
                el.prepend(fragment);

                // Maintain scroll position (anchoring)
                if (noAnchoring) {
                    el.scrollTop = 0;
                } else {
                    el.scrollTop = oldScrollTop + (el.scrollHeight - oldScrollHeight);
                }
            }
        }, 100);
        $console.data("prev-timer", timer);
    }

    printMessage(message, consoleName) {
        if (consoleName) {
            const $console = this.getConsole$(consoleName);
            if ($console) {
                let buffer = $console.data("log-buffer");
                if (!buffer) {
                    buffer = [];
                    $console.data("log-buffer", buffer);
                }
                buffer.push({ html: message, className: "event ellipses" });
                this.scrollToBottom($console);
            }
        } else {
            for (let key in this.consoles) {
                this.printMessage(message, key);
            }
        }
    }

    printErrorMessage(message, consoleName) {
        if (consoleName || !Object.keys(this.consoles).length) {
            const $console = this.getConsole$(consoleName);
            if ($console) {
                let buffer = $console.data("log-buffer");
                if (!buffer) {
                    buffer = [];
                    $console.data("log-buffer", buffer);
                }
                buffer.push({ html: message, className: "event error" });
                this.scrollToBottom($console);
            }
        } else {
            for (let key in this.consoles) {
                this.printErrorMessage(message, key);
            }
        }
    }

    processMessage(message) {
        const idx1 = message.indexOf(":");
        const idx2 = (idx1 !== -1 ? message.indexOf(":", idx1 + 1) : -1);
        const idx3 = (idx2 !== -1 ? message.indexOf(":", idx2 + 1) : -1);
        if (idx3 === -1) {
            return;
        }

        const instanceName = message.substring(0, idx1);
        let exporterType = message.substring(idx1 + 1, idx2);
        const exporterName = message.substring(idx2 + 1, idx3);

        let subType = "";
        if (exporterType.includes("/")) {
            const parts = exporterType.split("/");
            exporterType = parts[0];
            subType = parts[1];
        }

        const exporterKey = instanceName + ":" + exporterType + ":" + exporterName;
        const messageContent = message.substring(idx3 + 1);

        switch (exporterType) {
            case "event":
                if (messageContent.length) {
                    const eventData = JSON.parse(messageContent);
                    this.processEventData(instanceName, exporterType, exporterName, exporterKey, eventData);
                }
                break;
            case "data":
                if (messageContent.length) {
                    if (subType === "chart") {
                        const chartData = JSON.parse(messageContent);
                        this.processChartData(instanceName, exporterType, exporterName, exporterKey, chartData);
                    }
                }
                break;
            case "metric":
                if (messageContent.length) {
                    const metricData = JSON.parse(messageContent);
                    this.processMetricData(instanceName, exporterType, exporterName, exporterKey, metricData);
                }
                break;
            case "log":
                this.printLogMessage(instanceName, exporterType, exporterName, exporterKey, messageContent, subType);
                break;
        }
    }

    printLogMessage(instanceName, exporterType, logName, exporterKey, messageContent, subType) {
        this.indicate(instanceName, exporterType, logName);
        const $console = this.getConsole$(exporterKey);
        if ($console) {
            if (subType === "p") {
                if (messageContent) {
                    let prevBuffer = $console.data("log-prev-buffer");
                    if (!prevBuffer) {
                        prevBuffer = [];
                        $console.data("log-prev-buffer", prevBuffer);
                    }
                    prevBuffer.push(messageContent);
                    this.prependToConsole($console);
                } else {
                    let prevBuffer = $console.data("log-prev-buffer");
                    if (!prevBuffer) {
                        prevBuffer = [];
                        $console.data("log-prev-buffer", prevBuffer);
                    }
                    prevBuffer.push({ html: "No more logs to load.", className: "event ellipses" });
                    this.prependToConsole($console, true);
                    $console.closest(".console-box").find(".load-previous").hide();
                }
            } else if (!$console.data("pause")) {
                let buffer = $console.data("log-buffer");
                if (!buffer) {
                    buffer = [];
                    $console.data("log-buffer", buffer);
                }
                buffer.push(messageContent);
                this.scrollToBottom($console);
            }
        }
    }

    processEventData(instanceName, exporterType, eventName, exporterKey, eventData) {
        switch (eventName) {
            case "activity":
                this.indicate(instanceName, exporterType, eventName);
                if (eventData.activities) {
                    this.printActivityStatus(exporterKey, eventData.activities);
                }
                if (this.visible) {
                    const $track = this.getDisplay$(exporterKey);
                    if ($track) {
                        const varName = exporterKey.replace(/:/g, '_');
                        if (!this.currentActivityCounts[varName]) {
                            this.currentActivityCounts[varName] = 0;
                            this.printCurrentActivityCount(exporterKey, 0);
                        }
                        this.launchBullet($track, eventData, () => {
                            this.currentActivityCounts[varName]++;
                            this.printCurrentActivityCount(exporterKey, this.currentActivityCounts[varName]);
                        }, () => {
                            if (this.currentActivityCounts[varName] > 0) {
                                this.currentActivityCounts[varName]--;
                            }
                            this.printCurrentActivityCount(exporterKey, this.currentActivityCounts[varName]);
                        });
                    }
                } else {
                    this.printCurrentActivityCount(exporterKey, 0);
                }
                this.updateActivityCount(
                    instanceName + ":" + exporterType + ":session",
                    eventData.sessionId,
                    eventData.activityCount || 0);
                break;
            case "session":
                this.printSessionEventData(exporterKey, eventData);
                break;
        }
    }

    processMetricData(instanceName, exporterType, metricName, exporterKey, metricData) {
        const $metric = this.getMetric$(exporterKey);
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

    launchBullet($track, eventData, onLeaving, onArriving) {
        if (eventData.elapsedTime === undefined || eventData.elapsedTime === null) return;

        // Skip visualization and counting if tab is hidden
        if (document.hidden) return;

        if (onLeaving) onLeaving();

        // Find the painter associated with this track-box
        let painter = null;
        for (let key in this.displays) {
            if (this.displays[key][0] === $track[0]) {
                painter = this.painters[key];
                break;
            }
        }

        if (painter) {
            if (this.activeBulletCount < this.maxBullets) {
                this.activeBulletCount++;
                painter.addBullet(eventData, () => {
                    this.activeBulletCount--;
                    if (onArriving) onArriving();
                });
            } else {
                // Still update counts via timer even if capped
                setTimeout(() => {
                    if (onArriving) onArriving();
                }, eventData.elapsedTime + 900);
            }
        }
    }

    clearBullets() {
        for (let key in this.painters) {
            this.painters[key].clear();
        }
        this.activeBulletCount = 0;
    }

    indicate(instanceName, exporterType, exporterName) {
        this.blink(this.getIndicator$("domain:event:"));
        if (this.visible) {
            this.blink(this.getIndicator$("instance:event:" + instanceName));
            if (exporterType === "log") {
                this.blink(this.getIndicator$(instanceName + ":log:" + exporterName));
            }
        }
    }

    blink($indicator) {
        if ($indicator && !$indicator.hasClass("on")) {
            $indicator.addClass("blink on");
            setTimeout(() => {
                $indicator.removeClass("blink on");
            }, 500);
        }
    }

    printActivityStatus(exporterKey, activities) {
        const $activityStatus = this.getIndicator$(exporterKey);
        if ($activityStatus) {
            const separator = (activities.errors > 0 ? " / " : (activities.interim > 0 ? "+" : "-"));
            $activityStatus.find(".interim .separator").text(separator);
            $activityStatus.find(".interim .total").text(activities.interim > 0 ? activities.interim : "");
            $activityStatus.find(".interim .errors").text(activities.errors > 0 ? activities.errors : "");
            $activityStatus.find(".cumulative .total").text(activities.total);
        }
    }

    resetInterimActivityStatus(exporterKey) {
        const $activityStatus = this.getIndicator$(exporterKey);
        if ($activityStatus) {
            $activityStatus.find(".interim .separator").text("");
            $activityStatus.find(".interim .total").text(0);
            $activityStatus.find(".interim .errors").text("");
        }
    }

    resetInterimTimer(exporterKey) {
        if (this.sampleInterval) {
            const $activityStatus = this.getIndicator$(exporterKey);
            if ($activityStatus) {
                const $samplingTimerBar = $activityStatus.find(".sampling-timer-bar");
                const $samplingTimerStatus = $activityStatus.find(".sampling-timer-status");
                if ($samplingTimerBar.length) {
                    let timer = $samplingTimerBar.data("timer");
                    if (timer) {
                        clearInterval(timer);
                        $samplingTimerBar.removeData("timer");
                    }
                    let second = (dayjs().minute() * 60 + dayjs().second()) % this.sampleInterval;
                    $samplingTimerBar.animate({ height: 0 }, 600);
                    $samplingTimerBar.animate({ height: (second++ / this.sampleInterval * 100).toFixed(2) + "%" }, 400);
                    $samplingTimerStatus.text(second + "/" + this.sampleInterval);
                    timer = setInterval(() => {
                        if (!this.enable) {
                            clearInterval(timer);
                            $samplingTimerBar.removeData("timer");
                            return;
                        }
                        const percent = second++ / this.sampleInterval * 100;
                        $samplingTimerBar.css("height", percent.toFixed(2) + "%");
                        $samplingTimerStatus.text(second + "/" + this.sampleInterval);
                        if (second > 300) second = 0;
                        else if (second % 10 === 0) {
                            second = (dayjs().minute() * 60 + dayjs().second()) % this.sampleInterval;
                        }
                    }, 1000);
                    $samplingTimerBar.data("timer", timer);
                }
            }
        }
    }

    resetAllInterimTimers() {
        for (let key in this.indicators) {
            const $activityStatus = this.getIndicator$(key);
            if ($activityStatus.hasClass("activity-status")) {
                this.resetInterimTimer(key);
            }
        }
    }

    printCurrentActivityCount(exporterKey, count) {
        const $activityStatus = this.getIndicator$(exporterKey);
        if ($activityStatus) {
            $activityStatus.find(".current .total").text(count);
        }
    }

    printSessionEventData(exporterKey, eventData) {
        const $display = this.getDisplay$(exporterKey);
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
            const $sessions = $display.find("ul.sessions");

            if (eventData.fullSync && eventData.createdSessions) {
                const newSids = eventData.createdSessions.map(s => {
                    const session = (typeof s === "string" ? JSON.parse(s) : s);
                    return session.sessionId;
                });
                $sessions.find("li").each(function () {
                    const sid = $(this).data("sid");
                    if (sid && !newSids.includes(sid)) {
                        const timer = $(this).data("timer");
                        if (timer) clearTimeout(timer);
                        $(this).remove();
                    }
                });
            }

            if (eventData.createdSessions) {
                eventData.createdSessions.forEach(session => this.addSession($sessions, typeof session === "string" ? JSON.parse(session) : session));
            }
            if (eventData.destroyedSessions) {
                eventData.destroyedSessions.forEach(sessionId => $sessions.find("li[data-sid='" + sessionId + "']").remove());
            }
            if (eventData.evictedSessions) {
                eventData.evictedSessions.forEach(sessionId => {
                    const $item = $sessions.find("li[data-sid='" + sessionId + "']");
                    if (!$item.hasClass("inactive")) {
                        $item.addClass("inactive");
                        const inactiveInterval = Math.min($item.data("inactive-interval") || this.tempResidentInactiveSecs, this.tempResidentInactiveSecs);
                        setTimeout(() => $item.remove(), inactiveInterval * 1000);
                    }
                });
            }
            if (eventData.residedSessions) {
                eventData.residedSessions.forEach(session => this.addSession($sessions, typeof session === "string" ? JSON.parse(session) : session));
            }
        }
    }

    addSession($sessions, session) {
        $sessions.find("li[data-sid='" + session.sessionId + "']").each(function () {
            const timer = $(this).data("timer");
            if (timer) clearTimeout(timer);
        }).remove();

        const $count = $("<div class='count'></div>").text(session.activityCount || 0);
        if (session.activityCount > 1 || !session.countryCode) $count.addClass("counting");
        if (session.username) $count.addClass("active");

        const $li = $("<li/>")
            .attr("data-sid", session.sessionId)
            .attr("data-inactive-interval", session.inactiveInterval)
            .append($count);

        if (session.tempResident) {
            $li.addClass("inactive");
            const inactiveInterval = Math.min(session.inactiveInterval || 30, 30);
            const timer = setTimeout(() => $li.remove(), inactiveInterval * 1000);
            $li.data("timer", timer);
        }

        if (session.countryCode) {
            $("<img class='flag' alt=''/>")
                .attr("src", this.flagsUrl + session.countryCode.toLowerCase() + ".png")
                .attr("alt", session.countryCode)
                .attr("title", countries[session.countryCode].name)
                .appendTo($li);
        }
        if (session.username) {
            $("<div class='username'/>").text(session.username).appendTo($li);
        }

        const $detail = $("<div class='detail'/>")
            .append($("<p/>").text(session.sessionId))
            .append($("<p/>").text(dayjs.utc(session.createAt).local().format("LLL")));
        if (session.ipAddress) $detail.append($("<p/>").text(session.ipAddress));
        $detail.appendTo($li);

        if (session.tempResident) $li.appendTo($sessions);
        else $li.prependTo($sessions);
    }

    updateActivityCount(exporterKey, sessionId, activityCount) {
        const $display = this.getDisplay$(exporterKey);
        if ($display) {
            const $li = $display.find("ul.sessions li[data-sid='" + sessionId + "']");
            const $count = $li.find(".count").text(activityCount);
            if (activityCount > 1) $count.addClass("counting");
            $li.show();
        }
    }

    processChartData(instanceName, exporterType, eventName, exporterKey, chartData) {
        const dashboardChart = this.getChart$(exporterKey);
        if (!dashboardChart) return;
        this.setLoading(instanceName, false);

        if (eventName === "activity") {
            const prefix = instanceName + ":event:" + eventName;
            if (!dashboardChart.isDrawn()) this.resetInterimTimer(prefix);
            else if (chartData.rolledUp) {
                this.resetInterimTimer(prefix);
                this.resetInterimActivityStatus(prefix);
            }
        }
        const dateUnit = (chartData.rolledUp ? dashboardChart.dateUnit : chartData.dateUnit);
        const dateOffset = (chartData.rolledUp ? dashboardChart.dateOffset : chartData.dateOffset);
        const labels = chartData.labels;
        const data1 = chartData.data1;
        const data2 = chartData.data2.map(n => (eventName === "activity" ? n : null));

        if (!dashboardChart.isDrawn() || !chartData.rolledUp) {
            dashboardChart.ensureCanvas();
            this.pruneDataPoints(labels, data1, data2, dashboardChart.$container);
            dashboardChart.draw(dateUnit, labels, data1, data2);
            dashboardChart.dateOffset = dateOffset;
        } else if (!dateOffset) {
            if (!dateUnit) {
                dashboardChart.rollup(labels, data1, data2);
                this.pruneDataPoints(dashboardChart.getLabels(), dashboardChart.getDataset(0), dashboardChart.getDataset(1), dashboardChart.$container);
                dashboardChart.update();
            } else if (this.client) {
                setTimeout(() => {
                    const options = [
                        "instance:" + instanceName,
                        "dateUnit:" + dateUnit,
                        "timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone
                    ];
                    this.client.refresh(options);
                }, 900);
            }
        }
    }

    pruneDataPoints(labels, data1, data2, $container) {
        if (this.cachedCanvasWidth === 0) {
            let w = 0;
            if ($container) {
                w = $container.find("canvas").width();
                if (w === 0) w = $container.width();
            }
            if (w === 0) {
                for (let key in this.charts) {
                    const dashboardChart = this.charts[key];
                    if (dashboardChart) {
                        w = dashboardChart.$container.find("canvas").width();
                        if (w === 0) w = dashboardChart.$container.width();
                        if (w > 0) break;
                    }
                }
            }
            if (w > 0) {
                this.cachedCanvasWidth = w - 90;
            }
        }
        const maxLabels = (this.cachedCanvasWidth > 0 ? Math.floor(this.cachedCanvasWidth / 21) : 0);
        if (maxLabels > 0) {
            const cnt = labels.length - maxLabels;
            if (cnt > 0) {
                labels.splice(0, cnt);
                data1.splice(0, cnt);
                data2.splice(0, cnt);
            }
        }
        return maxLabels;
    }

    getMaxStartDatetime(instanceName) {
        let result = "";
        for (let key in this.charts) {
            if (key.startsWith(instanceName + ":")) {
                const dashboardChart = this.charts[key];
                if (dashboardChart && dashboardChart.isDrawn()) {
                    const labels = dashboardChart.getLabels();
                    if (labels.length && labels[0] > result) {
                        result = labels[0];
                    }
                }
            }
        }
        return result;
    }
}

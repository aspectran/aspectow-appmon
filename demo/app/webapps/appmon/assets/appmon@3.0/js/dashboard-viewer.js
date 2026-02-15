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
        this.charts[instanceName + ":data:" + eventName] = $chart;
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
                const $chartBox = this.charts[key].closest(".chart-box");
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
        const exporterType = message.substring(idx1 + 1, idx2);
        const exporterName = message.substring(idx2 + 1, idx3);
        const messagePrefix = message.substring(0, idx3);
        const messageText = message.substring(idx3 + 1);

        switch (exporterType) {
            case "event":
                if (messageText.length) {
                    const eventData = JSON.parse(messageText);
                    this.processEventData(instanceName, exporterType, exporterName, messagePrefix, eventData);
                }
                break;
            case "data":
                if (messageText.length) {
                    const eventData = JSON.parse(messageText);
                    if (eventData.chartData) {
                        this.processChartData(instanceName, exporterType, exporterName, messagePrefix, eventData.chartData);
                    }
                }
                break;
            case "metric":
                if (messageText.length) {
                    const metricData = JSON.parse(messageText);
                    this.processMetricData(instanceName, exporterType, exporterName, messagePrefix, metricData);
                }
                break;
            case "log":
                this.printLogMessage(instanceName, exporterType, exporterName, messagePrefix, messageText);
                break;
        }
    }

    printLogMessage(instanceName, exporterType, logName, messagePrefix, messageText) {
        this.indicate(instanceName, exporterType, logName);
        const $console = this.getConsole$(messagePrefix);
        if ($console && !$console.data("pause")) {
            let buffer = $console.data("log-buffer");
            if (!buffer) {
                buffer = [];
                $console.data("log-buffer", buffer);
            }
            buffer.push(messageText);
            this.scrollToBottom($console);
        }
    }

    processEventData(instanceName, exporterType, eventName, messagePrefix, eventData) {
        switch (eventName) {
            case "activity":
                this.indicate(instanceName, exporterType, eventName);
                if (eventData.activities) {
                    this.printActivityStatus(messagePrefix, eventData.activities);
                }
                if (this.visible) {
                    const $track = this.getDisplay$(messagePrefix);
                    if ($track) {
                        const varName = messagePrefix.replace(':', '_');
                        if (!this.currentActivityCounts[varName]) {
                            this.currentActivityCounts[varName] = 0;
                            this.printCurrentActivityCount(messagePrefix, 0);
                        }
                        this.launchBullet($track, eventData, () => {
                            this.currentActivityCounts[varName]++;
                            this.printCurrentActivityCount(messagePrefix, this.currentActivityCounts[varName]);
                        }, () => {
                            if (this.currentActivityCounts[varName] > 0) {
                                this.currentActivityCounts[varName]--;
                            }
                            this.printCurrentActivityCount(messagePrefix, this.currentActivityCounts[varName]);
                        });
                    }
                } else {
                    this.printCurrentActivityCount(messagePrefix, 0);
                }
                this.updateActivityCount(
                    instanceName + ":" + exporterType + ":session",
                    eventData.sessionId,
                    eventData.activityCount || 0);
                break;
            case "session":
                this.printSessionEventData(messagePrefix, eventData);
                break;
        }
    }

    processMetricData(instanceName, exporterType, metricName, messagePrefix, metricData) {
        const $metric = this.getMetric$(messagePrefix);
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

    generateRandom(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
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

    printActivityStatus(messagePrefix, activities) {
        const $activityStatus = this.getIndicator$(messagePrefix);
        if ($activityStatus) {
            const separator = (activities.errors > 0 ? " / " : (activities.interim > 0 ? "+" : "-"));
            $activityStatus.find(".interim .separator").text(separator);
            $activityStatus.find(".interim .total").text(activities.interim > 0 ? activities.interim : "");
            $activityStatus.find(".interim .errors").text(activities.errors > 0 ? activities.errors : "");
            $activityStatus.find(".cumulative .total").text(activities.total);
        }
    }

    resetInterimActivityStatus(messagePrefix) {
        const $activityStatus = this.getIndicator$(messagePrefix);
        if ($activityStatus) {
            $activityStatus.find(".interim .separator").text("");
            $activityStatus.find(".interim .total").text(0);
            $activityStatus.find(".interim .errors").text("");
        }
    }

    resetInterimTimer(messagePrefix) {
        if (this.sampleInterval) {
            const $activityStatus = this.getIndicator$(messagePrefix);
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

    printCurrentActivityCount(messagePrefix, count) {
        const $activityStatus = this.getIndicator$(messagePrefix);
        if ($activityStatus) {
            $activityStatus.find(".current .total").text(count);
        }
    }

    printSessionEventData(messagePrefix, eventData) {
        const $display = this.getDisplay$(messagePrefix);
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

    updateActivityCount(messagePrefix, sessionId, activityCount) {
        const $display = this.getDisplay$(messagePrefix);
        if ($display) {
            const $li = $display.find("ul.sessions li[data-sid='" + sessionId + "']");
            const $count = $li.find(".count").text(activityCount);
            if (activityCount > 1) $count.addClass("counting");
            $li.show();
        }
    }

    processChartData(instanceName, exporterType, eventName, messagePrefix, chartData) {
        const $chart = this.getChart$(messagePrefix);
        if (!$chart) return;
        this.setLoading(instanceName, false);

        const chart = $chart.data("chart");
        if (eventName === "activity") {
            const prefix = instanceName + ":event:" + eventName;
            if (!chart) this.resetInterimTimer(prefix);
            else if (chartData.rolledUp) {
                this.resetInterimTimer(prefix);
                this.resetInterimActivityStatus(prefix);
            }
        }
        const dateUnit = (chartData.rolledUp ? $chart.data("dateUnit") : chartData.dateUnit);
        const dateOffset = (chartData.rolledUp ? $chart.data("dateOffset") : chartData.dateOffset);
        const labels = chartData.labels;
        const data1 = chartData.data1;
        const data2 = chartData.data2.map(n => (eventName === "activity" ? n : null));

        if (!chart || !chartData.rolledUp) {
            if (chart) chart.destroy();
            let $canvas = $chart.find("canvas");
            if (!$canvas.length) {
                $canvas = $("<canvas/>").appendTo($chart);
            }
            const maxLabels = this.adjustLabelCount(eventName, labels, data1, data2);
            const autoSkip = (maxLabels === 0);
            const newChart = this.drawChart(eventName, $canvas[0], dateUnit, labels, data1, data2, autoSkip);
            $chart.data("chart", newChart);
            if (dateUnit) $chart.data("dateUnit", dateUnit);
            else $chart.removeData("dateUnit");
            if (dateOffset) $chart.data("dateOffset", dateOffset);
            else $chart.removeData("dateOffset");
        } else if (!dateOffset) {
            if (!dateUnit) {
                this.updateChartAfterRolledUp(eventName, chart, labels, data1, data2);
            } else if (this.client) {
                setTimeout(() => {
                    const options = ["instance:" + instanceName, "dateUnit:" + dateUnit];
                    this.client.refresh(options);
                }, 900);
            }
        }
    }

    updateChartAfterRolledUp(eventName, chart, labels, data1, data2) {
        if (chart.data.labels.length > 0) {
            const lastIndex = chart.data.labels.length - 1;
            if (chart.data.labels[lastIndex] >= labels[0]) {
                chart.data.labels.splice(lastIndex, 1);
                chart.data.datasets[0].data.splice(lastIndex, 1);
                chart.data.datasets[1].data.splice(lastIndex, 1);
            }
        }
        chart.data.labels.push(...labels);
        chart.data.datasets[0].data.push(...data1);
        chart.data.datasets[1].data.push(...data2);
        this.adjustLabelCount(eventName, chart.data.labels, chart.data.datasets[0].data, chart.data.datasets[1].data);
        chart.update();
    }

    adjustLabelCount(eventName, labels, data1, data2) {
        if (this.cachedCanvasWidth === 0) {
            for (let key in this.charts) {
                if (key.endsWith(":" + eventName)) {
                    const $chart = this.charts[key];
                    if ($chart) {
                        const w = $chart.find("canvas").width();
                        if (w > 0) {
                            this.cachedCanvasWidth = w - 90;
                            break;
                        }
                    }
                }
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
                const chart = this.charts[key].data("chart");
                if (chart) {
                    const labels = chart.data.labels;
                    if (labels.length && labels[0] > result) {
                        result = labels[0];
                    }
                }
            }
        }
        return result;
    }

    drawChart(eventName, canvas, dateUnit, labels, data1, data2, autoSkip) {
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
        const chart = new Chart(canvas, {
            type: chartType,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        enabled: true,
                        reverse: true,
                        mode: 'x',
                        intersect: false,
                        callbacks: {
                            title: (tooltip) => {
                                const datetime = dayjs.utc(labels[tooltip[0].dataIndex], "YYYYMMDDHHmm").local();
                                switch (dateUnit) {
                                    case "hour": return datetime.format("LL HH:00");
                                    case "day": return datetime.format("LL");
                                    case "month": return datetime.date(1).format("LL");
                                    case "year": return datetime.format("YYYY");
                                    default: return datetime.format("LLL");
                                }
                            }
                        }
                    },
                    zoom: {
                        zoom: {
                            wheel: { enabled: false },
                            pinch: { enabled: true },
                            drag: {
                                enabled: true,
                                threshold: 21,
                                backgroundColor: "rgba(225,225,225,0.35)",
                                borderColor: "rgba(225,225,225)",
                                borderWidth: 1
                            },
                            mode: "x",
                            onZoomComplete: () => {
                                const $resetZoom = $(canvas).parent().find(".reset-zoom");
                                if (chart.isZoomedOrPanned()) {
                                    $resetZoom.off("click").on("click", () => chart.resetZoom()).show();
                                } else {
                                    $resetZoom.hide();
                                }
                            }
                        },
                        pan: { enabled: true, mode: "x", modifierKey: "ctrl" }
                    }
                },
                scales: {
                    x: {
                        display: true,
                        ticks: {
                            autoSkip: autoSkip,
                            includeBounds: false,
                            callback: (value, index) => {
                                const datetime = dayjs.utc(labels[value], "YYYYMMDDHHmm").local();
                                const datetime2 = (value > 0 ? dayjs.utc(labels[value - 1], "YYYYMMDDHHmm").local() : null);
                                switch (dateUnit) {
                                    case "hour": return (index === 0 || (datetime2 && datetime.isAfter(datetime2, "day"))) ? datetime.format("M/D HH:00") : datetime.format("HH:00");
                                    case "day": return (index === 0 || (datetime2 && datetime.isAfter(datetime2, "year"))) ? datetime.format("YYYY M/D") : datetime.format("M/D");
                                    case "month": return datetime.format("YYYY/M");
                                    case "year": return datetime.format("YYYY");
                                    default: return (index === 0 || (datetime2 && datetime.isAfter(datetime2, "day"))) ? datetime.format("M/D HH:mm") : datetime.format("HH:mm");
                                }
                            }
                        },
                        stacked: true,
                        grid: chartType === "line" ? {
                            color: (ctx) => (data2[ctx.tick.value] > 0 ? "#ff6384" : "#e4e4e4")
                        } : {}
                    },
                    y: {
                        display: true,
                        title: { display: true, text: dataLabel1 },
                        suggestedMin: 0,
                        suggestedMax: 5,
                        stacked: true,
                        grid: { color: "#e4e4e4" }
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
        });
        if (!chart.isZoomedOrPanned()) {
            $(canvas).parent().find(".reset-zoom").hide();
        }
        return chart;
    }
}

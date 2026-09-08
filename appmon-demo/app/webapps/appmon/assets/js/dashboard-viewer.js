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
 * The viewer component for the AppMon dashboard.
 * Responsible for rendering monitoring data, including logs, metrics, and charts.
 *
 * @version 4.1
 * @last-modified 2026-09-08
 */
class DashboardViewer {
    constructor(sampleInterval, options = {}) {
        this.flagsUrl = options.flagsUrl || "https://cdn.jsdelivr.net/gh/aspectran/aspectran-assets@main/assets/countries/flags/";
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
        this.metricHistories = {};
        this.maxMetricHistory = 60;
        this.activePopoverKey = null;
        this.activePopoverMetric$ = null;
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

    putDisplay$(appId, eventId, $display) {
        const key = appId + ":event:" + eventId;
        this.displays[key] = $display;
        if ($display.hasClass("track-box")) {
            const canvas = $display.find(".traffic-canvas")[0];
            if (canvas) {
                this.painters[key] = new TrafficPainter(canvas);
            }
        }
    }

    putMetric$(appId, metricId, $metric) {
        this.metrics[appId + ":metric:" + metricId] = $metric;
    }

    putChart$(appId, eventId, $chart) {
        const key = appId + ":data:" + eventId;
        this.charts[key] = new DashboardChart($chart, eventId);
    }

    putConsole$(appId, logId, $console) {
        this.consoles[appId + ":log:" + logId] = $console;
    }

    putIndicator$(appId, exporterType, exporterName, $indicator) {
        this.indicators[appId + ":" + exporterType + ":" + exporterName] = $indicator;
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

    setLoading(appId, isLoading) {
        for (let key in this.charts) {
            if (key.startsWith(appId + ":")) {
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

        const appId = message.substring(0, idx1);
        let exporterType = message.substring(idx1 + 1, idx2);
        const exporterName = message.substring(idx2 + 1, idx3);

        let subType = "";
        if (exporterType.includes("/")) {
            const parts = exporterType.split("/");
            exporterType = parts[0];
            subType = parts[1];
        }

        const exporterKey = appId + ":" + exporterType + ":" + exporterName;
        const messageContent = message.substring(idx3 + 1);

        switch (exporterType) {
            case "event":
                if (messageContent.length) {
                    const eventData = JSON.parse(messageContent);
                    this.processEventData(appId, exporterType, exporterName, exporterKey, eventData);
                }
                break;
            case "data":
                if (messageContent.length) {
                    if (subType === "chart") {
                        const chartData = JSON.parse(messageContent);
                        this.processChartData(appId, exporterType, exporterName, exporterKey, chartData);
                    }
                }
                break;
            case "metric":
                if (messageContent.length) {
                    const metricData = JSON.parse(messageContent);
                    this.processMetricData(appId, exporterType, exporterName, exporterKey, metricData);
                }
                break;
            case "log":
                this.printLogMessage(appId, exporterType, exporterName, exporterKey, messageContent, subType);
                break;
        }
    }

    printLogMessage(appId, exporterType, logId, exporterKey, messageContent, subType) {
        this.indicate(appId, exporterType, logId);
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

    processEventData(appId, exporterType, eventId, exporterKey, eventData) {
        switch (eventId) {
            case "activity":
                this.indicate(appId, exporterType, eventId);
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
                    appId + ":" + exporterType + ":session",
                    eventData.sessionId,
                    eventData.activityCount || 0);
                break;
            case "session":
                this.printSessionEventData(exporterKey, eventData);
                break;
        }
    }

    processMetricData(appId, exporterType, metricId, exporterKey, metricData) {
        const $metric = this.getMetric$(exporterKey);
        if ($metric) {
            const $dd = $metric.find("dd").not(".sparkline-wrap");
            let $val = $dd.find(".value");
            if (!$val.length) {
                $dd.empty();
                $val = $("<span class=\"value\"></span>").appendTo($dd);
                if (metricData.unit) {
                    $("<small class=\"unit\"></small>").text(metricData.unit).appendTo($dd);
                }
            } else {
                const $unit = $dd.find(".unit");
                if ($unit.length && !$unit.text() && metricData.unit) {
                    $unit.text(metricData.unit);
                }
            }
            let formatted = metricData.format;
            for (let key in metricData.data) {
                formatted = formatted.replace("{" + key + "}", metricData.data[key]);
            }
            $val.text(formatted);
            $metric.attr("title", JSON.stringify(metricData.data, null, 2));

            // Record history and render sparkline
            this.recordMetricHistory(exporterKey, metricId, metricData, formatted);
            const $sparkline = $metric.find("canvas.sparkline");
            if ($sparkline.length) {
                this.renderSparkline($sparkline[0], exporterKey, metricId);
            }

            // Update popover chart if this metric is currently displayed
            if (this.activePopoverKey === exporterKey) {
                this.renderPopoverChart();
            }
        }
    }

    recordMetricHistory(exporterKey, metricId, metricData, formatted) {
        if (!this.metricHistories[exporterKey]) {
            this.metricHistories[exporterKey] = [];
        }
        const history = this.metricHistories[exporterKey];
        const data = metricData.data || {};

        let val1 = 0;
        let val2 = null;
        let label1 = "Value";
        let label2 = null;

        if (metricId === "cpu") {
            val1 = (typeof data.processCpu === "number" ? data.processCpu : 0);
            val2 = (typeof data.systemCpu === "number" && data.systemCpu >= 0 ? data.systemCpu : null);
            label1 = "Process CPU";
            label2 = "System CPU";
        } else if (metricId === "heap") {
            val1 = (typeof data.used === "number" ? data.used : 0);
            val2 = (typeof data.max === "number" && data.max > 0 ? data.max : (typeof data.committed === "number" ? data.committed : null));
            label1 = "Used Memory";
            label2 = (typeof data.max === "number" && data.max > 0 ? "Max Memory" : "Committed");
        } else if (metricId.endsWith("-tp") || metricId === "tp") {
            val1 = (typeof data.active === "number" ? data.active : 0);
            val2 = (typeof data.total === "number" ? data.total : null);
            label1 = "Active Threads";
            label2 = "Total Threads";
        } else if (metricId.startsWith("cp") || metricId.endsWith("-cp") || metricId === "cp" || data.poolName) {
            val1 = (typeof data.used === "number" ? data.used : (typeof data.active === "number" ? data.active : 0));
            val2 = (typeof data.total === "number" ? data.total : (typeof data.max === "number" ? data.max : null));
            label1 = "Used Connections";
            label2 = "Total Connections";
        } else {
            // Determine primary and secondary values from format string if available
            const formatKeys = [];
            if (metricData.format) {
                const matches = metricData.format.match(/\{([a-zA-Z0-9_-]+)\}/g);
                if (matches) {
                    matches.forEach(m => {
                        const k = m.slice(1, -1);
                        if (typeof data[k] === "number" && !formatKeys.includes(k)) {
                            formatKeys.push(k);
                        }
                    });
                }
            }
            if (formatKeys.length > 0) {
                val1 = data[formatKeys[0]];
                label1 = formatKeys[0];
                if (formatKeys.length > 1) {
                    val2 = data[formatKeys[1]];
                    label2 = formatKeys[1];
                }
            } else {
                const priorityKeys = ["used", "active", "current", "count", "value", "total"];
                const numKeys = Object.keys(data).filter(k => typeof data[k] === "number");
                const pKey = priorityKeys.find(k => typeof data[k] === "number");
                if (pKey) {
                    val1 = data[pKey];
                    label1 = pKey;
                    const remKeys = numKeys.filter(k => k !== pKey);
                    if (remKeys.length > 0) {
                        val2 = data[remKeys[0]];
                        label2 = remKeys[0];
                    }
                } else if (numKeys.length > 0) {
                    val1 = data[numKeys[0]];
                    label1 = numKeys[0];
                    if (numKeys.length > 1) {
                        val2 = data[numKeys[1]];
                        label2 = numKeys[1];
                    }
                }
            }
        }

        history.push({
            time: Date.now(),
            val1,
            val2,
            label1,
            label2,
            formatted,
            unit: metricData.unit || "",
            title: metricData.title || metricId,
            metricId,
            data
        });

        if (history.length > this.maxMetricHistory) {
            history.shift();
        }
    }

    renderSparkline(canvas, exporterKey, metricId) {
        const history = this.metricHistories[exporterKey];
        if (!history || history.length < 2) return;

        const dpr = window.devicePixelRatio || 1;
        const width = 48;
        const height = 16;
        if (canvas.width !== width * dpr || canvas.height !== height * dpr) {
            canvas.width = width * dpr;
            canvas.height = height * dpr;
        }

        const ctx = canvas.getContext("2d");
        ctx.save();
        ctx.scale(dpr, dpr);
        ctx.clearRect(0, 0, width, height);

        let min = Infinity;
        let max = -Infinity;
        for (let i = 0; i < history.length; i++) {
            const v = history[i].val1;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (metricId === "cpu") {
            min = 0;
            max = Math.max(max, 100);
        } else if (max === min) {
            max += 1;
            min = Math.max(0, min - 1);
        }
        const range = max - min || 1;
        const paddingY = 2;
        const paddingX = 2;
        const usableH = height - (paddingY * 2);
        const usableW = width - (paddingX * 2);
        const stepX = usableW / (history.length - 1);

        const points = [];
        for (let i = 0; i < history.length; i++) {
            const x = paddingX + (i * stepX);
            const normY = (history[i].val1 - min) / range;
            const y = height - paddingY - (normY * usableH);
            points.push({ x, y });
        }

        const isHigh = (metricId === "cpu" && history[history.length - 1].val1 >= 85);
        const strokeColor = isHigh ? "#ef4444" : "#0284c7";
        const fillColor = isHigh ? "rgba(239, 68, 68, 0.3)" : "rgba(2, 132, 199, 0.25)";

        // Gradient Fill
        ctx.beginPath();
        ctx.moveTo(points[0].x, height);
        points.forEach(p => ctx.lineTo(p.x, p.y));
        ctx.lineTo(points[points.length - 1].x, height);
        ctx.closePath();
        ctx.fillStyle = fillColor;
        ctx.fill();

        // Stroke Line
        ctx.beginPath();
        points.forEach((p, idx) => {
            if (idx === 0) ctx.moveTo(p.x, p.y);
            else ctx.lineTo(p.x, p.y);
        });
        ctx.strokeStyle = strokeColor;
        ctx.lineWidth = 1.2;
        ctx.lineCap = "round";
        ctx.lineJoin = "round";
        ctx.stroke();

        // Last Point Dot
        const last = points[points.length - 1];
        ctx.beginPath();
        ctx.arc(last.x, last.y, 1.8, 0, Math.PI * 2);
        ctx.fillStyle = strokeColor;
        ctx.fill();

        ctx.restore();
    }

    toggleMetricPopover(exporterKey, $metric) {
        if (this.activePopoverKey === exporterKey) {
            this.hideMetricPopover();
        } else {
            this.showMetricPopover(exporterKey, $metric);
        }
    }

    showMetricPopover(exporterKey, $metric) {
        const history = this.metricHistories[exporterKey];
        if (!history || !history.length) return;

        this.activePopoverKey = exporterKey;
        this.activePopoverMetric$ = $metric;

        const $popover = $("#metric-popover");
        if (!$popover.length) return;

        const offset = $metric.offset();
        const mWidth = $metric.outerWidth();
        const mHeight = $metric.outerHeight();
        const pWidth = 310;

        let top = offset.top + mHeight + 7;
        let left = offset.left + (mWidth / 2) - (pWidth / 2);
        left = Math.max(10, Math.min(left, $(window).width() - pWidth - 10));

        const arrowLeft = offset.left + (mWidth / 2) - left;
        $popover.find(".popover-arrow").css("left", Math.max(14, Math.min(arrowLeft, pWidth - 18)) + "px");

        $popover.css({ top: top + "px", left: left + "px" }).fadeIn(120);
        this.renderPopoverChart();
    }

    hideMetricPopover() {
        this.activePopoverKey = null;
        this.activePopoverMetric$ = null;
        $("#metric-popover").hide();
    }

    renderPopoverChart() {
        if (!this.activePopoverKey) return;
        const history = this.metricHistories[this.activePopoverKey];
        if (!history || !history.length) return;

        const $popover = $("#metric-popover");
        const last = history[history.length - 1];

        $popover.find(".popover-title").text(last.title || last.metricId);
        $popover.find(".popover-current").text(last.formatted + (last.unit ? " " + last.unit : ""));

        // Calculate Min, Max, Avg for val1
        let min1 = Infinity;
        let max1 = -Infinity;
        let sum1 = 0;
        let count1 = 0;
        history.forEach(h => {
            if (typeof h.val1 === "number") {
                if (h.val1 < min1) min1 = h.val1;
                if (h.val1 > max1) max1 = h.val1;
                sum1 += h.val1;
                count1++;
            }
        });
        const avg1 = count1 > 0 ? (sum1 / count1) : 0;

        const formatVal = (v) => {
            if (last.metricId === "cpu") return v.toFixed(1) + "%";
            if (last.metricId === "heap") {
                if (v >= 1048576) return (v / 1048576).toFixed(1) + " GB";
                return (v / 1024).toFixed(0) + " MB";
            }
            return Number.isInteger(v) ? v : v.toFixed(1);
        };

        $popover.find(".stat-min").text(count1 > 0 ? formatVal(min1) : "-");
        $popover.find(".stat-max").text(count1 > 0 ? formatVal(max1) : "-");
        $popover.find(".stat-avg").text(count1 > 0 ? formatVal(avg1) : "-");

        // Details breakdown
        const $details = $popover.find(".popover-details").empty();
        if (last.metricId === "cpu") {
            if (typeof last.data.systemCpu === "number" && last.data.systemCpu >= 0) {
                $details.append(`<span><strong>System:</strong>${last.data.systemCpu}%</span>`);
            }
            if (last.data.systemLoad !== undefined && last.data.systemLoad >= 0) {
                $details.append(`<span><strong>Load:</strong>${last.data.systemLoad}</span>`);
            }
            if (last.data.processors) {
                $details.append(`<span><strong>Cores:</strong>${last.data.processors}</span>`);
            }
        } else if (last.metricId === "heap") {
            if (last.data.maxKB) {
                $details.append(`<span><strong>Max:</strong>${last.data.maxKB}</span>`);
            }
            if (last.data.usedKB) {
                $details.append(`<span><strong>Used:</strong>${last.data.usedKB}</span>`);
            }
        } else if (last.metricId.endsWith("-tp") || last.metricId === "tp") {
            if (last.data.max !== undefined) {
                $details.append(`<span><strong>Max Pool:</strong>${last.data.max < 0 ? 'Unbounded' : last.data.max}</span>`);
            }
            if (last.data.queued !== undefined) {
                $details.append(`<span><strong>Queued:</strong>${last.data.queued}</span>`);
            }
            if (last.data.workerName) {
                $details.append(`<span><strong>Worker:</strong>${last.data.workerName}</span>`);
            }
        } else if (last.metricId.startsWith("cp") || last.metricId.endsWith("-cp") || last.metricId === "cp" || last.data.poolName) {
            if (last.data.idle !== undefined) {
                $details.append(`<span><strong>Idle:</strong>${last.data.idle}</span>`);
            }
            if (last.data.awaiting !== undefined) {
                $details.append(`<span><strong>Awaiting:</strong>${last.data.awaiting}</span>`);
            }
            if (last.data.active !== undefined) {
                $details.append(`<span><strong>Active:</strong>${last.data.active}</span>`);
            }
            if (last.data.poolName) {
                $details.append(`<span><strong>Pool:</strong>${last.data.poolName}</span>`);
            }
        }

        // Render Canvas
        const canvas = $popover.find("canvas.popover-chart")[0];
        if (!canvas) return;

        const dpr = window.devicePixelRatio || 1;
        const width = 286;
        const height = 110;
        if (canvas.width !== width * dpr || canvas.height !== height * dpr) {
            canvas.width = width * dpr;
            canvas.height = height * dpr;
        }

        const ctx = canvas.getContext("2d");
        ctx.save();
        ctx.scale(dpr, dpr);
        ctx.clearRect(0, 0, width, height);

        // Overall scale calculation
        let chartMin = min1;
        let chartMax = max1;
        if (last.val2 !== null) {
            history.forEach(h => {
                if (typeof h.val2 === "number") {
                    if (h.val2 < chartMin) chartMin = h.val2;
                    if (h.val2 > chartMax) chartMax = h.val2;
                }
            });
        }
        if (last.metricId === "cpu") {
            chartMin = 0;
            chartMax = Math.max(chartMax, 100);
        } else if (chartMax === chartMin) {
            chartMax += 1;
            chartMin = Math.max(0, chartMin - 1);
        }
        const range = chartMax - chartMin || 1;
        const padTop = 14;
        const padBottom = 16;
        const padLeft = 36;
        const padRight = 8;
        const plotW = width - padLeft - padRight;
        const plotH = height - padTop - padBottom;

        // Grid lines (3 horizontal lines)
        const gridColor = "rgba(0, 0, 0, 0.08)";
        const textColor = "#64748b";

        ctx.strokeStyle = gridColor;
        ctx.lineWidth = 1;
        ctx.font = "9px tabular-nums, sans-serif";
        ctx.fillStyle = textColor;
        ctx.textAlign = "right";

        for (let i = 0; i <= 2; i++) {
            const y = padTop + (plotH * (i / 2));
            ctx.beginPath();
            ctx.moveTo(padLeft, y);
            ctx.lineTo(width - padRight, y);
            ctx.stroke();

            const gridVal = chartMax - (range * (i / 2));
            ctx.fillText(formatVal(gridVal), padLeft - 4, y + 3);
        }

        if (history.length >= 2) {
            const stepX = plotW / (history.length - 1);

            // Render Secondary line (val2) if present
            if (last.val2 !== null) {
                const p2 = [];
                history.forEach((h, i) => {
                    if (typeof h.val2 === "number") {
                        const x = padLeft + (i * stepX);
                        const normY = (h.val2 - chartMin) / range;
                        const y = padTop + plotH - (normY * plotH);
                        p2.push({ x, y });
                    }
                });
                if (p2.length >= 2) {
                    ctx.save();
                    ctx.setLineDash([3, 3]);
                    ctx.beginPath();
                    p2.forEach((p, idx) => {
                        if (idx === 0) ctx.moveTo(p.x, p.y);
                        else ctx.lineTo(p.x, p.y);
                    });
                    ctx.strokeStyle = "#94a3b8";
                    ctx.lineWidth = 1.2;
                    ctx.stroke();
                    ctx.restore();
                }
            }

            // Render Primary line (val1)
            const p1 = [];
            history.forEach((h, i) => {
                const x = padLeft + (i * stepX);
                const normY = (h.val1 - chartMin) / range;
                const y = padTop + plotH - (normY * plotH);
                p1.push({ x, y });
            });

            // Primary Gradient Fill
            ctx.beginPath();
            ctx.moveTo(p1[0].x, padTop + plotH);
            p1.forEach(p => ctx.lineTo(p.x, p.y));
            ctx.lineTo(p1[p1.length - 1].x, padTop + plotH);
            ctx.closePath();
            const grad = ctx.createLinearGradient(0, padTop, 0, padTop + plotH);
            grad.addColorStop(0, "rgba(2, 132, 199, 0.35)");
            grad.addColorStop(1, "rgba(2, 132, 199, 0.0)");
            ctx.fillStyle = grad;
            ctx.fill();

            // Primary Stroke
            ctx.beginPath();
            p1.forEach((p, idx) => {
                if (idx === 0) ctx.moveTo(p.x, p.y);
                else ctx.lineTo(p.x, p.y);
            });
            ctx.strokeStyle = "#0284c7";
            ctx.lineWidth = 1.8;
            ctx.lineCap = "round";
            ctx.lineJoin = "round";
            ctx.stroke();

            // Last Dot
            const lastP = p1[p1.length - 1];
            ctx.beginPath();
            ctx.arc(lastP.x, lastP.y, 2.5, 0, Math.PI * 2);
            ctx.fillStyle = "#0284c7";
            ctx.fill();
        }

        ctx.restore();
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

    indicate(appId, exporterType, exporterName) {
        this.blink(this.getIndicator$("group:event:"));
        this.blink(this.getIndicator$("node:event:"));
        if (this.visible) {
            this.blink(this.getIndicator$("app:event:" + appId));
            if (exporterType === "log") {
                this.blink(this.getIndicator$(appId + ":log:" + exporterName));
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
                //console.log("Created sessions:", eventData.createdSessions);
                eventData.createdSessions.forEach(session => this.addSession($sessions, typeof session === "string" ? JSON.parse(session) : session));
            }
            if (eventData.destroyedSessions) {
                eventData.destroyedSessions.forEach(sessionId => $sessions.find("li[data-sid='" + sessionId + "']").remove());
            }
            if (eventData.evictedSessions) {
                eventData.evictedSessions.forEach(sessionId => {
                    const $li = $sessions.find("li[data-sid='" + sessionId + "']");
                    let timer = $(this).data("timer");
                    if (timer) clearTimeout(timer);
                    if ($li.data("temp-resident")) {
                        $li.remove(); // Temp resident session removed immediately upon eviction
                        return;
                    }
                    $li.addClass("inactive");
                    let inactiveInterval = $li.data("inactive-interval") || 0;
                    inactiveInterval = (inactiveInterval <= 0 ? this.tempResidentInactiveSecs : Math.min(inactiveInterval, this.tempResidentInactiveSecs)) * 1000;
                    timer = setTimeout(() => $li.remove(), inactiveInterval);
                    $li.data("timer", timer);
                });
            }
            if (eventData.residedSessions) {
                //console.log("Resided sessions:", eventData.residedSessions);
                eventData.residedSessions.forEach(session => this.addSession($sessions, typeof session === "string" ? JSON.parse(session) : session));
            }
        }
    }

    addSession($sessions, session) {
        //console.log("Adding session:", session);
        $sessions.find("li[data-sid='" + session.sessionId + "']").each(function () {
            const timer = $(this).data("timer");
            if (timer) clearTimeout(timer);
        }).remove();

        const $count = $("<div class='count'></div>").text(session.activityCount || 0);
        if (session.activityCount > 0) $count.text(session.activityCount);
        if (session.activityCount > 0 && !session.tempResident || !session.countryCode) $count.addClass("counting");
        if (session.username) $count.addClass("active");

        const $li = $("<li/>")
            .attr("data-sid", session.sessionId)
            .attr("data-inactive-interval", session.inactiveInterval)
            .append($count);

        const inactiveInterval = session.inactiveInterval;
        if (inactiveInterval && inactiveInterval > 0) {
            $li.attr("data-inactive-interval", inactiveInterval)
            const timer = setTimeout(() => $li.remove(), inactiveInterval * 1000);
            $li.data("timer", timer);
        }

        if (session.tempResident) {
            $li.attr("data-temp-resident", true).addClass("inactive");
        }

        if (session.countryCode) {
            const code = session.countryCode.toLowerCase();
            const countryInfo = (typeof countries !== "undefined" && countries[session.countryCode])
                ? countries[session.countryCode]
                : null;
            $("<img class='flag' alt=''/>")
                .attr("src", this.flagsUrl + code + ".png")
                .attr("alt", session.countryCode)
                .attr("title", countryInfo ? countryInfo.name : session.countryCode)
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
            const inactiveInterval = $li.data("inactive-interval");
            if (inactiveInterval) {
                let timer = $li.data("timer");
                if (timer) clearTimeout(timer);
                timer = setTimeout(() => $li.remove(), inactiveInterval * 1000);
                $li.data("timer", timer);
            }
        }
    }

    processChartData(appId, exporterType, eventId, exporterKey, chartData) {
        const dashboardChart = this.getChart$(exporterKey);
        if (!dashboardChart) return;
        this.setLoading(appId, false);

        if (eventId === "activity") {
            const prefix = appId + ":event:" + eventId;
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
        const data2 = chartData.data2.map(n => (eventId === "activity" ? n : null));

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
                        "appId:" + appId,
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

    getMaxStartDatetime(appId) {
        let result = "";
        for (let key in this.charts) {
            if (key.startsWith(appId + ":")) {
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

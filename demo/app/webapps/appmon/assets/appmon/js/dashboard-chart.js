/**
 * The chart component for the AppMon dashboard.
 * Responsible for rendering and updating individual charts using Chart.js.
 */
class DashboardChart {
    constructor($container, eventName) {
        this.$container = $container;
        this.eventName = eventName;
        this.chart = null;
        this.dateUnit = null;
        this.dateOffset = null;
    }

    isDrawn() {
        return (this.chart !== null);
    }

    getLabels() {
        return (this.chart ? this.chart.data.labels : []);
    }

    getDataset(index) {
        return (this.chart ? this.chart.data.datasets[index].data : []);
    }

    ensureCanvas() {
        let $canvas = this.$container.find("canvas");
        if (!$canvas.length) {
            $canvas = $("<canvas/>").appendTo(this.$container);
        }
        return $canvas;
    }

    draw(dateUnit, labels, data1, data2) {
        this.destroy();
        const $canvas = this.ensureCanvas();

        let dataLabel1;
        let borderColor1;
        let backgroundColor1;
        switch (this.eventName) {
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
        this.chart = new Chart($canvas[0], {
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
                                const datetime = dayjs(labels[tooltip[0].dataIndex]);
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
                                const $resetZoom = this.$container.find(".reset-zoom");
                                if (this.isZoomedOrPanned()) {
                                    $resetZoom.off("click").on("click", () => this.resetZoom()).show();
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
                            autoSkip: false,
                            includeBounds: false,
                            callback: (value, index) => {
                                const datetime = dayjs(labels[value]);
                                const datetime2 = (value > 0 ? dayjs(labels[value - 1]) : null);
                                switch (dateUnit) {
                                    case "hour":
                                        return (index === 0 || (datetime2 && !datetime.isSame(datetime2, "day")))
                                            ? datetime.format("M/D HH:00")
                                            : datetime.format("HH:00");
                                    case "day":
                                        return (index === 0 || (datetime2 && !datetime.isSame(datetime2, "year")))
                                            ? datetime.format("YYYY M/D")
                                            : datetime.format("M/D");
                                    case "month":
                                        return datetime.format("YYYY/M");
                                    case "year":
                                        return datetime.format("YYYY");
                                    default: // 5m.
                                        return (index === 0 || (datetime2 && !datetime.isSame(datetime2, "day")))
                                            ? datetime.format("M/D HH:mm")
                                            : datetime.format("HH:mm");
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

        this.dateUnit = dateUnit;
        const $resetZoom = this.$container.find(".reset-zoom");
        if (this.isZoomedOrPanned()) {
            $resetZoom.show();
        } else {
            $resetZoom.hide();
        }
    }

    setData(labels, data1, data2) {
        if (this.chart) {
            this.chart.data.labels = labels;
            this.chart.data.datasets[0].data = data1;
            this.chart.data.datasets[1].data = data2;
            this.update();
        }
    }

    rollup(labels, data1, data2) {
        if (this.chart) {
            const chartLabels = this.chart.data.labels;
            const chartData1 = this.chart.data.datasets[0].data;
            const chartData2 = this.chart.data.datasets[1].data;
            if (chartLabels.length > 0) {
                const lastIndex = chartLabels.length - 1;
                if (chartLabels[lastIndex] >= labels[0]) {
                    chartLabels.splice(lastIndex, 1);
                    chartData1.splice(lastIndex, 1);
                    chartData2.splice(lastIndex, 1);
                }
            }
            chartLabels.push(...labels);
            chartData1.push(...data1);
            chartData2.push(...data2);
        }
    }

    isZoomedOrPanned() {
        return (this.chart && typeof this.chart.isZoomedOrPanned === "function" && this.chart.isZoomedOrPanned());
    }

    resetZoom() {
        if (this.chart && typeof this.chart.resetZoom === "function") {
            this.chart.resetZoom();
        }
    }

    update() {
        if (this.chart) {
            this.chart.update();
        }
    }

    destroy() {
        if (this.chart) {
            this.chart.destroy();
            this.chart = null;
        }
    }
}

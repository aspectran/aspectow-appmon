<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<link rel="stylesheet" href="<aspectran:token type='bean' expression='appmonAssets^url'/>/css/appmon.css?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>">
<link rel="stylesheet" href="<aspectran:token type='bean' expression='appmonAssets^url'/>/css/appmon-dark.css?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>">
<script>const contextPath = "<aspectran:url value="/"/>";</script>
<script src="<aspectran:token type='bean' expression='appmonAssets^url'/>/js/dashboard-builder.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>"></script>
<script src="<aspectran:token type='bean' expression='appmonAssets^url'/>/js/dashboard-viewer.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>"></script>
<script src="<aspectran:token type='bean' expression='appmonAssets^url'/>/js/dashboard-chart.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>"></script>
<script src="<aspectran:token type='bean' expression='appmonAssets^url'/>/js/traffic-painter.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>"></script>
<script src="<aspectran:token type='bean' expression='appmonAssets^url'/>/js/base-client.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>"></script>
<script src="<aspectran:token type='bean' expression='appmonAssets^url'/>/js/websocket-client.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>"></script>
<script src="<aspectran:token type='bean' expression='appmonAssets^url'/>/js/polling-client.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>"></script>
<script>
    if (typeof DashboardBuilder === "undefined") {
        document.write('<link rel="stylesheet" href="https://appmon-assets.aspectran.com/appmon/css/appmon.css?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>">');
        document.write('<link rel="stylesheet" href="https://appmon-assets.aspectran.com/appmon/css/appmon-dark.css?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>">');
        document.write('<script src="https://appmon-assets.aspectran.com/appmon/js/dashboard-builder.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>">\x3C/script>');
        document.write('<script src="https://appmon-assets.aspectran.com/appmon/js/dashboard-viewer.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>">\x3C/script>');
        document.write('<script src="https://appmon-assets.aspectran.com/appmon/js/traffic-painter.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>">\x3C/script>');
        document.write('<script src="https://appmon-assets.aspectran.com/appmon/js/base-client.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>">\x3C/script>');
        document.write('<script src="https://appmon-assets.aspectran.com/appmon/js/websocket-client.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>">\x3C/script>');
        document.write('<script src="https://appmon-assets.aspectran.com/appmon/js/polling-client.js?v=<aspectran:token type='bean' expression='appmonAssets^cacheBustingVersion'/>">\x3C/script>');
    }
</script>
<div class="container">
    <div class="row">
        <div class="domain metrics-bar">
            <div class="title">
                <i class="bi bi-pc-display-horizontal"></i><span class="number"></span>
            </div>
            <div class="metric">
                <dl>
                    <dt></dt>
                    <dd></dd>
                </dl>
            </div>
        </div>
    </div>
    <ul class="instance tabs nav nav-tabs mt-3">
        <li class="tabs-title nav-item">
            <a class="nav-link"><i class="bi bi-box"></i> <span class="title">Instance</span> <i class="indicator bi bi-lightning-charge-fill"></i></a>
        </li>
    </ul>
    <div class="control-bar">
        <div class="options">
            <i class="bi bi-columns-gap d-none d-lg-inline-block"></i>
            <div class="layout-options btn-group d-none d-lg-inline-block" title="Layout options">
                <a class="btn compact on">Compact</a>
            </div>
            <i class="bi bi-bar-chart-line"></i>
            <div class="date-unit-options btn-group" title="Date unit options">
                <a class="btn default on">Default</a><a class="btn hour" data-unit="hour">Hour</a><a class="btn day" data-unit="day">Day</a><a class="btn month" data-unit="month">Month</a><a class="btn year" data-unit="year">Year</a>
            </div>
            <div class="date-offset-options btn-group" title="Date offset options">
                <a class="btn previous on" data-offset="previous" title="Previous"><i class="bi bi-rewind-fill"></i></a><a class="btn current" data-offset="current" title="Latest"><i class="bi bi-skip-forward-fill"></i></a>
            </div>
            <div class="speed-options btn-group d-none" title="Speed options">
                <a class="btn bi bi-fast-forward faster" title="Set to poll every second. Turn this option on only when absolutely necessary.">Faster polling interval</a>
            </div>
        </div>
    </div>
    <div class="row g-0">
        <div class="col-12 col-lg-6 event-box">
            <div class="title-bar">
                <i class="bi bi-pc-display-horizontal"></i><span class="number"></span>
                <h4 class="ellipses"></h4>
            </div>
            <div class="track-box">
                <canvas class="traffic-canvas"></canvas>
                <div class="track-stack">
                    <div class="activity-status-plate">
                        <div class="bottom-plate-left"></div>
                        <div class="bottom-plate-right"></div>
                    </div>
                    <div class="activity-status">
                        <p class="current" title="Current activities"><span class="total"></span></p>
                        <p class="interim" title="Activities tallied during the sampling period"><span class="errors"></span><span class="separator">-</span><span class="total"></span></p>
                        <p class="cumulative" title="Total cumulative activities recorded"><span class="total"></span></p>
                        <div class="sampling-timer-bar"></div>
                        <div class="sampling-timer-status" title="Sampling interval"></div>
                    </div>
                </div>
            </div>
            <div class="instance metrics-bar">
                <div class="metric">
                    <dl>
                        <dt></dt>
                        <dd></dd>
                    </dl>
                </div>
            </div>
            <div class="session-box">
                <div class="row g-0">
                    <div class="col-sm-12 col-md-4">
                        <div class="panel status">
                            <dl class="session-stats">
                                <dt title="The number of active sessions">Active Sessions</dt>
                                <dd><span class="number numberOfActives">0</span></dd>
                                <dt title="The highest number of sessions that have been active at a single time">Peak Active Sessions</dt>
                                <dd><span class="number highestNumberOfActives">0</span></dd>
                                <dt title="The number of sessions created since system bootup">Created Sessions</dt>
                                <dd><span class="number numberOfCreated">0</span></dd>
                                <dt title="The number of expired sessions">Expired Sessions</dt>
                                <dd><span class="number numberOfExpired">0</span></dd>
                                <dt title="The number of sessions that are inactive or managed by another server in a cluster">Unmanaged Sessions</dt>
                                <dd><span class="number numberOfUnmanaged">0</span></dd>
                                <dt title="The number of rejected sessions due to reaching the maximum session limit">Rejected Sessions</dt>
                                <dd><span class="number numberOfRejected">0</span></dd>
                            </dl>
                            <p class="since"><i>Since <span class="startTime"></span></i></p>
                            <div class="knob-bar"><div class="knob"></div></div>
                        </div>
                    </div>
                    <div class="col-sm-12 col-md-8">
                        <div class="panel ground">
                            <ul class="sessions"></ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-12 col-lg-6 visual-box">
            <div class="chart-box" style="display: none;">
                <div class="chart">
                    <button class="bi bi-fullscreen-exit reset-zoom" type="button"></button>
                </div>
                <div class="loading-overlay">
                    <div class="spinner-border" role="status"></div>
                </div>
            </div>
            <div class="chart-box available">
                <div class="chart">
                </div>
            </div>
            <div class="chart-box available">
                <div class="chart">
                </div>
            </div>
        </div>
        <div class="col-12 console-box">
            <div class="status-bar">
                <h4 class="ellipses"></h4>
                <a class="load-previous" title="Load previous logs" style="display: none;">
                    <i class="icon bi bi-arrow-up-circle"></i>
                </a>
                <a class="clear-screen" title="Clear screen">
                    <i class="icon bi bi-trash"></i>
                </a>
                <a class="pause-switch" title="Pause log output" data-title-pause="Pause log output" data-title-resume="Resume log output">
                    <i class="icon bi bi-pause" data-icon-pause="bi-pause" data-icon-resume="bi-play-fill"></i>
                </a>
                <a class="expand-switch" title="Expand" data-title-expand="Expand" data-title-compress="Compress">
                    <i class="icon bi bi-arrows-fullscreen" data-icon-expand="bi-arrows-fullscreen" data-icon-compress="bi-fullscreen-exit"></i>
                </a>
                <a class="tailing-switch" title="Disable Auto Scroll" data-title-on="Disable Auto Scroll" data-title-off="Enable Auto Scroll">
                    <i class="tailing-status"></i>
                </a>
            </div>
            <pre class="console"></pre>
        </div>
    </div>
</div>
<script>
    $(function () {
        const BASE_PATH = "${pageContext.request.contextPath}";
        const INSTANCES = "${page.instances}";
        new DashboardBuilder().build(BASE_PATH, INSTANCES);
    });
</script>

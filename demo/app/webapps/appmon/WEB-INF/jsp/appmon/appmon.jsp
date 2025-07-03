<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<link rel="stylesheet" href="<aspectran:url value="/assets/appmon/css/appmon.css?20250703"/>">
<script src="<aspectran:url value="/assets/appmon/js/front-builder.js?20250703"/>"></script>
<script src="<aspectran:url value="/assets/appmon/js/front-viewer.js?20250703"/>"></script>
<script src="<aspectran:url value="/assets/appmon/js/websocket-client.js?20250703"/>"></script>
<script src="<aspectran:url value="/assets/appmon/js/polling-client.js?20250703"/>"></script>
<div class="container">
    <div class="row g-0">
        <div class="domain status-bar">
            <div class="title">
                <i class="bi bi-pc-display-horizontal"></i><span class="number"></span>
            </div>
            <div class="status">
                <dl>
                    <dt></dt>
                    <dd></dd>
                </dl>
            </div>
        </div>
    </div>
    <ul class="instance tabs nav nav-tabs mt-3">
        <li class="tabs-title nav-item">
            <a class="nav-link"><i class="bullet bi bi-modem"></i><span class="title">JPetStore</span> <i class="indicator bi bi-lightning-charge-fill"></i></a>
        </li>
        <li class="tabs-title nav-item available active">
            <a class="nav-link"><i class="bullet bi bi-modem"></i><span class="title">PetClinic</span> <i class="indicator bi bi-lightning-charge-fill"></i></a>
        </li>
    </ul>
    <div class="control-bar">
        <div class="options">
            <i class="bi bi-layout-wtf d-none d-lg-inline-block"></i>
            <div class="layout-options btn-group d-none d-lg-inline-block" title="Layout options">
                <a class="btn compact on"> Compact</a>
            </div>
            <i class="bi bi-bar-chart-line"></i>
            <div class="date-unit-options btn-group" title="Date unit options">
                <a class="btn default on">Default</a><a class="btn hour" data-unit="hour">Hour</a><a class="btn day" data-unit="day">Day</a><a class="btn month" data-unit="month">Month</a><a class="btn year" data-unit="year">Year</a>
            </div>
            <div class="date-offset-options btn-group" title="Date offset options">
                <a class="btn previous on" data-offset="previous" title="Previous"><i class="bi bi-rewind-fill"></i></a><a class="btn current" data-offset="current" title="Next"><i class="bi bi-skip-forward-fill"></i></a>
            </div>
            <div class="speed-options btn-group d-none" title="Speed options">
                <a class="btn bi bi-fast-forward faster" title="Set to poll every second. Turn this option on only when absolutely necessary."> Faster polling interval</a>
            </div>
        </div>
    </div>
    <div class="row g-0">
        <div class="col-lg-6 event-box">
            <div class="title-bar">
                <i class="bi bi-pc-display-horizontal"></i><span class="number"></span>
                <h4 class="ellipses"></h4>
            </div>
            <div class="track-box">
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
            <div class="instance status-bar">
                <div class="status">
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
                                <dt title="The number of active sessions">Current Active Sessions</dt>
                                <dd><span class="number numberOfActives">0</span></dd>
                                <dt title="The highest number of sessions that have been active at a single time">Highest Active Sessions</dt>
                                <dd><span class="number highestNumberOfActives">0</span></dd>
                                <dt title="The number of sessions created since system bootup">Created Sessions</dt>
                                <dd><span class="number numberOfCreated">0</span></dd>
                                <dt title="The number of expired sessions">Expired Sessions</dt>
                                <dd><span class="number numberOfExpired">0</span></dd>
                                <dt title="This number of sessions includes sessions that are inactive or have been transferred to a session manager on another clustered server">Unmanaged Sessions</dt>
                                <dd><span class="number numberOfUnmanaged">0</span></dd>
                                <dt title="The number of rejected sessions">Rejected Sessions</dt>
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
        <div class="col-lg-6 visual-box">
            <div class="chart-box" style="display: none;">
                <div class="chart">
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
        <div class="col console-box">
            <div class="status-bar">
                <h4 class="ellipses"></h4>
                <a class="tailing-switch" title="Scroll to End of Log">
                    <i class="tailing-status"></i>
                </a>
                <a class="pause-switch" title="Pause log output">
                    <i class="icon bi bi-pause"></i>
                </a>
                <a class="clear-screen" title="Clear screen">
                    <i class="icon bi bi-trash"></i>
                </a>
            </div>
            <pre class="console"></pre>
        </div>
    </div>
</div>
<script>
    $(function () {
        const BASE_PATH = "${pageContext.request.contextPath}";
        const TOKEN = "${page.token}";
        const INSTANCES = "${page.instances}";
        new FrontBuilder().build(BASE_PATH, TOKEN, INSTANCES);
    });
</script>

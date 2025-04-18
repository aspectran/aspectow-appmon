<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<link rel="stylesheet" href="<aspectran:url value="/assets/appmon/css/appmon.css?20250418"/>">
<script src="<aspectran:url value="/assets/appmon/js/front-builder.js?20250418"/>"></script>
<script src="<aspectran:url value="/assets/appmon/js/front-viewer.js?20250418"/>"></script>
<script src="<aspectran:url value="/assets/appmon/js/websocket-client.js?20250418"/>"></script>
<script src="<aspectran:url value="/assets/appmon/js/polling-client.js?20250418"/>"></script>
<div class="grid-x">
    <dl class="cell instance tabs t10 b0">
        <dd class="tabs-title"><a><span class="bullet fi-marker"></span>
            <span class="title"> </span> <span class="indicator fi-loop"></span></a>
        </dd>
    </dl>
    <div class="cell control-bar">
        <div class="options">
            <div class="layout-options button-group tiny show-for-large" title="Layout options">
                <span class="fi-layout"></span>
                <a class="button compact on"> Compact</a>
            </div>
            <div class="date-unit-options button-group tiny" title="Date unit options">
                <span class="fi-graph-bar"></span>
                <a class="button default on">Default</a><a class="button hour" data-unit="hour">Hour</a><a class="button day" data-unit="day">Day</a><a class="button month" data-unit="month">Month</a><a class="button year" data-unit="year">Year</a>
            </div>
            <div class="date-offset-options button-group tiny" title="Date offset options">
                <a class="button previous on" data-offset="previous" title="Previous"><span class="fi-previous"></span></a><a class="button current" data-offset="current" title="Next"><span class="fi-next"></span></a>
            </div>
            <div class="speed-options button-group tiny hide" title="Speed options">
                <a class="button fi-fast-forward faster" title="Set to poll every second. Turn this option on only when absolutely necessary."> Faster polling interval</a>
            </div>
        </div>
    </div>
    <div class="cell event-box large-6">
        <div class="grid-x">
            <div class="cell title-bar">
                <h4 class="ellipses"></h4>
            </div>
            <div class="cell track-box">
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
            <div class="cell session-box">
                <div class="grid-x">
                    <div class="cell small-12 medium-4">
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
                    <div class="cell small-12 medium-8">
                        <div class="panel ground">
                            <ul class="sessions"></ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="cell visual-box large-6">
        <div class="chart-box">
            <div class="chart">
            </div>
        </div>
    </div>
    <div class="cell console-box">
        <div class="status-bar">
            <h4 class="ellipses"></h4>
            <a class="tailing-switch" title="Scroll to End of Log">
                <span class="tailing-status"></span>
            </a>
            <a class="clear-screen" title="Clear screen">
                <span class="icon fi-x"></span>
            </a>
            <a class="pause-switch" title="Pause log output">
                <span class="icon fi-pause"></span>
            </a>
        </div>
        <pre class="console"></pre>
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

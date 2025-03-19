<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<link rel="stylesheet" href="<aspectran:url value="/assets/appmon/css/appmon.css?20250317-5"/>">
<script src="<aspectran:url value="/assets/appmon/js/front-builder.js?20250316"/>"></script>
<script src="<aspectran:url value="/assets/appmon/js/front-viewer.js?20250317-6"/>"></script>
<script src="<aspectran:url value="/assets/appmon/js/websocket-client.js?20250316"/>"></script>
<script src="<aspectran:url value="/assets/appmon/js/polling-client.js?20250316"/>"></script>
<div class="grid-x">
    <div class="cell options t10 b5">
        <ul class="speed-options">
            <li class="fi-fast-forward fast" title="Set to poll every second. Turn this option on only when absolutely necessary."></li>
        </ul>
        <ul class="layout-options hide-for-small-only">
            <li class="fi-layout compact on"><a> Compact</a></li>
        </ul>
    </div>
    <dl class="cell instance tabs b0">
        <dd class="tabs-title"><a><span class="bullet fi-list-bullet"></span>
            <span class="title"> </span> <span class="indicator fi-loop"></span></a>
        </dd>
    </dl>
    <div class="cell event-box large-6">
        <div class="grid-x">
            <div class="cell status-bar">
                <h4 class="ellipses"></h4>
            </div>
            <div class="cell track-box">
                <div class="track-stack"><div class="activities">
                    <p class="current" title="Current activities"></p>
                    <p class="tally" title="Activities tallied during the sampling period"></p>
                    <p class="total" title="Total activities recorded"></p>
                </div>
                </div>
            </div>
            <div class="cell session-box">
                <div class="grid-x">
                    <div class="cell small-12 medium-4">
                        <div class="panel">
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
                            <p class="text-right"><i>Elapsed <span class="elapsed"></span></i></p>
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
            <a href="#" class="tailing-switch" title="Scroll to End of Log">
                <span class="tailing-status"></span>
            </a>
            <a href="#" class="clear-screen" title="Clear screen">
                <span class="icon fi-x"></span>
            </a>
            <a href="#" class="pause-switch" title="Pause log output">
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

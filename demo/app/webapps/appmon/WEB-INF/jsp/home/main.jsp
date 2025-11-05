<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>

<style>
    .card-body .row .col {
        margin-bottom: 0.75rem;
    }
    .card-body a {
        display: flex;
        align-items: center;
        width: 100%;
        height: 100%;
        padding: 0.5rem 0.75rem;
        border: 1px solid #dee2e6;
        border-radius: 0.25rem;
        text-decoration: none;
        transition: all 0.2s ease-in-out;
    }
    .card-body a:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 8px rgba(0,0,0,0.05);
    }
    .card-body a i {
        margin-right: 0.5rem;
        margin-bottom: 0;
        font-size: 1rem;
    }

    /* For large screens and up */
    @media (min-width: 992px) {
        .card-body a {
            flex-direction: column;
            justify-content: center;
            text-align: center;
            padding-top: 1.5rem;
            padding-bottom: 1.5rem;
            font-size: 1.05rem;
        }
        .card-body a i {
            margin-right: 0;
            margin-bottom: 0.5rem;
            font-size: 1.75rem;
        }
    }

    /* Blink effect */
    .highlight-blink {
        background-color: #85898d !important; /* Bootstrap primary blue */
        color: #fff !important;
        transition: background-color 0.1s ease-in-out, color 0.1s ease-in-out;
    }
</style>

<div class="container-fluid pt-3">
    <div class="row g-3">
        <!-- Domains Panel -->
        <div class="col-lg-5">
            <div class="card h-100">
                <div class="card-header">
                    <h5 class="mb-0"><i class="bi bi-hdd-stack"></i> Domains</h5>
                </div>
                <div class="card-body">
                    <p class="card-text text-muted">List of servers in the cluster. Click a domain to see related instances.</p>
                    <c:choose>
                        <c:when test="${not empty page.domainInfoList}">
                            <div class="row row-cols-1 row-cols-sm-2 mt-3">
                                <c:forEach items="${page.domainInfoList}" var="domainInfo">
                                    <div class="col">
                                        <a href="#" class="domain-link">
                                            <i class="bi bi-server"></i> ${domainInfo.title}
                                        </a>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <p class="text-muted mt-3">No domains found.</p>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <!-- Instance Configurations Panel -->
        <div class="col-lg-7">
            <div class="card h-100">
                <div class="card-header">
                    <h5 class="mb-0"><i class="bi bi-display"></i> Instance Configurations</h5>
                </div>
                <div class="card-body">
                    <p class="card-text text-muted">Common instance configurations running on each domain.</p>
                    <c:choose>
                        <c:when test="${not empty page.instanceInfoList}">
                            <div class="row row-cols-1 row-cols-sm-2 row-cols-md-3 mt-3">
                                <c:forEach items="${page.instanceInfoList}" var="instanceInfo">
                                    <div class="col">
                                        <a href="<aspectran:url value="/dashboard/#${instanceInfo.name}"/>" class="instance-link">
                                            <i class="bi bi-box"></i> ${instanceInfo.title}
                                        </a>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <p class="text-muted mt-3">No instance configurations found.</p>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <!-- Framework Anatomy Panel -->
        <div class="col-lg-12 mt-3">
            <div class="card h-100">
                <div class="card-header">
                    <h5 class="mb-0"><i class="bi bi-diagram-3"></i> Framework Anatomy</h5>
                </div>
                <div class="card-body">
                    <p class="card-text text-muted">Explore the internal structure and components of the loaded framework contexts.</p>
                    <c:choose>
                        <c:when test="${not empty page.allContextNames}">
                            <div class="row row-cols-1 row-cols-sm-2 row-cols-md-4 row-cols-lg-6 mt-3">
                                <c:forEach items="${page.allContextNames}" var="contextName">
                                    <div class="col">
                                        <a href="<aspectran:url value="/anatomy/${contextName}"/>">
                                            <i class="bi bi-pc-display-horizontal"></i> ${contextName}
                                        </a>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <p class="text-muted mt-3">No framework contexts found.</p>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
$(function() {
    $('.domain-link').on('click', function(e) {
        e.preventDefault();

        const $instances = $('.instance-link');
        if ($instances.length === 0) {
            return;
        }

        let delay = 0;
        const blinkDuration = 300; // The duration of the highlight
        const blinkInterval = 150; // The time between the start of each blink

        $instances.each(function() {
            const $instance = $(this);
            setTimeout(function() {
                $instance.addClass('highlight-blink');
                setTimeout(function() {
                    $instance.removeClass('highlight-blink');
                }, blinkDuration);
            }, delay);
            delay += blinkInterval;
        });
    });
});
</script>
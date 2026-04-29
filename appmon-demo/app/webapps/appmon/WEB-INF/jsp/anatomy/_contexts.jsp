<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div class="container py-2 pb-md-0 py-lg-0">
    <ul class="anatomy tabs nav nav-tabs">
        <c:forEach items="${page.allContextNames}" var="name">
            <li class="tabs-title nav-item<c:if test="${page.contextName eq name}"> active</c:if>"><a class="nav-link" href="<aspectran:url value="/anatomy/${name}"/>"><i class="bi bi-pc-display-horizontal"></i> <span class="title">${name}</span></a></li>
        </c:forEach>
    </ul>
</div>

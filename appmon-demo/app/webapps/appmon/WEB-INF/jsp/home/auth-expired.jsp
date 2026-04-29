<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div class="container text-center py-5 my-5">
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <div class="display-1 text-danger mb-4">
                <i class="bi bi-shield-lock"></i>
            </div>
            <h1 class="display-4 fw-bold">Authentication Expired</h1>
            <p class="lead text-muted mt-3 mb-4">
                Your access token has expired or is invalid.
                Please return to the main page to get a new authentication token.
            </p>
            <a href="<aspectran:url value="/"/>" class="btn btn-primary btn-lg">
                <i class="bi bi-house-door me-2"></i>Go to Main Page
            </a>
        </div>
    </div>
</div>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="forgot.title"/></title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />
  <link rel="icon" href="<c:url value='/favicon.ico'/>" />
  <link rel="icon" type="image/png" sizes="32x32" href="<c:url value='/favicon-32.png'/>" />
  <link rel="icon" type="image/png" sizes="16x16" href="<c:url value='/favicon-16.png'/>" />
  <link rel="apple-touch-icon" href="<c:url value='/apple-touch-icon.png'/>" />
  <meta name="theme-color" content="#0b1220" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
</head>
<body>

<div class="container">
  <div class="topbar">
    <div class="brand">
      <img class="logo" src="<c:url value='/assets/logo.png'/>" alt="SubscriptionOverview" />
      <div>
        <h1>SubscriptionOverview</h1>
        <div class="sub"><fmt:message key="forgot.subtitle"/></div>
      </div>
    </div>

    <div class="nav">
      <a href="<%=request.getContextPath()%>/login"><fmt:message key="forgot.backToLogin"/></a>
    </div>
  </div>

  <div class="card" style="max-width:520px; margin:14px auto 0;">
    <h3><fmt:message key="forgot.header"/></h3>
    <div class="muted"><fmt:message key="forgot.lead"/></div>
    <hr class="sep"/>

    <c:if test="${not empty flashMsg}">
      <div class="notice flash"><b><c:out value="${flashMsg}"/></b></div>
      <div style="height:10px;"></div>
    </c:if>

    <form class="form" method="post" action="<c:url value='/auth/forgot-password'/>">
      <div class="field">
        <label for="email"><fmt:message key="forgot.email"/></label>
        <input id="email" type="email" name="email" autocomplete="email" required />
      </div>

      <div class="row" style="justify-content:flex-end;">
        <button class="btn btn-primary" type="submit"><fmt:message key="forgot.submit"/></button>
      </div>
    </form>
  </div>
</div>

</body>
</html>
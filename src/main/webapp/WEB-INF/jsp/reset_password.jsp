<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="reset.title"/></title>
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
        <div class="sub"><fmt:message key="reset.subtitle"/></div>
      </div>
    </div>

    <div class="nav">
      <a href="<%=request.getContextPath()%>/login"><fmt:message key="forgot.backToLogin"/></a>
    </div>
  </div>

  <div class="card" style="max-width:520px; margin:14px auto 0;">
    <h3><fmt:message key="reset.header"/></h3>

    <c:if test="${not empty flashError}">
      <div class="notice error"><b><c:out value="${flashError}"/></b></div>
      <div style="height:10px;"></div>
    </c:if>

    <c:choose>
      <c:when test="${valid}">
        <div class="muted"><fmt:message key="reset.lead"/></div>
        <hr class="sep"/>

        <form class="form" method="post" action="<c:url value='/auth/reset-password'/>">
          <input type="hidden" name="token" value="${token}" />

          <div class="field">
            <label for="password"><fmt:message key="reset.password"/></label>
            <input id="password" type="password" name="password" required />
          </div>

          <div class="field">
            <label for="password2"><fmt:message key="reset.password2"/></label>
            <input id="password2" type="password" name="password2" required />
          </div>

          <div class="row" style="justify-content:flex-end;">
            <button class="btn btn-primary" type="submit"><fmt:message key="reset.submit"/></button>
          </div>
        </form>
      </c:when>

      <c:otherwise>
        <div class="muted"><fmt:message key="reset.invalidLead"/></div>
        <hr class="sep"/>
        <a class="btn btn-primary" href="<c:url value='/auth/forgot-password'/>"><fmt:message key="reset.requestNew"/></a>
      </c:otherwise>
    </c:choose>
  </div>
</div>

</body>
</html>
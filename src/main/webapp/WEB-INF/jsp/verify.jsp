<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html>
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="verify.title"/></title>
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
        <div class="sub"><fmt:message key="verify.title"/></div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/'/>"><fmt:message key="login.home"/></a>
      <a href="<c:url value='/login'/>"><fmt:message key="login.submit"/></a>
    </div>
  </div>

  <div class="card" style="max-width:640px; margin:18px auto;">
    <c:choose>
      <c:when test="${ok}">
        <h3><fmt:message key="verify.okTitle"/></h3>
        <div class="muted"><fmt:message key="verify.okText"/></div>
        <hr class="sep"/>
        <a class="btn btn-primary" href="<c:url value='/login'/>"><fmt:message key="verify.toLogin"/></a>
      </c:when>
      <c:otherwise>
        <h3><fmt:message key="verify.failTitle"/></h3>
        <div class="muted"><fmt:message key="verify.failText"/></div>
        <hr class="sep"/>
        <a class="btn btn-primary" href="<c:url value='/login'/>"><fmt:message key="verify.toLogin"/></a>
      </c:otherwise>
    </c:choose>
  </div>
</div>

</body>
</html>
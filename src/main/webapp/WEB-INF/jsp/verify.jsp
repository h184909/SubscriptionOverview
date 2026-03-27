<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html>
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="verify.title"/></title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
</head>
<body>

<div class="container">
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
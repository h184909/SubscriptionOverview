<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
  <title>Login</title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
</head>
<body>

<div class="container">
  <div class="topbar">
    <div class="brand">
      <div class="logo"></div>
      <div>
        <h1>SubscriptionOverview</h1>
        <div class="sub">Logg inn</div>
      </div>
    </div>
    <div class="nav">
      <a href="<%=request.getContextPath()%>/auth/register">Registrer</a>
    </div>
  </div>

  <div class="card" style="max-width:520px; margin:14px auto 0;">
    <h3>Logg inn</h3>
    <div class="muted">Skriv inn e-post og passord. :)</div>
    <hr class="sep"/>

    <c:if test="${not empty loginError}">
      <div class="notice error"><b>${loginError}</b></div>
      <div style="height:10px;"></div>
    </c:if>

    <form class="form" method="post" action="<c:url value='/login'/>">
      <div class="field">
        <label>E-post</label>
        <input type="text" name="email" value="${loginForm.email}" />
      </div>

      <div class="field">
        <label>Passord</label>
        <input type="password" name="passord" />
      </div>

      <div class="row" style="justify-content:space-between;">
        <a class="muted" href="<%=request.getContextPath()%>/auth/register">Ingen bruker? Registrer</a>
        <button class="btn btn-primary" type="submit">Logg inn</button>
      </div>
    </form>
  </div>
</div>

</body>
</html>

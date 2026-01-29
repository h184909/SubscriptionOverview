<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
  <title>Registrer</title>
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
        <div class="sub">Registrer bruker</div>
      </div>
    </div>
    <div class="nav">
      <a class="btn" href="<%=request.getContextPath()%>/">Forside</a>
      <a class="btn btn-primary" href="<%=request.getContextPath()%>/login">Logg inn</a>
    </div>
  </div>

  <div class="card" style="max-width:560px; margin:14px auto 0;">
    <h3>Opprett konto</h3>
    <div class="muted">Fyll inn e-post og passord.</div>

    <c:if test="${not empty feilmeldinger}">
      <hr class="sep"/>
      <div class="notice error">
        <b>Feil:</b>
        <ul style="margin:8px 0 0 18px;">
          <c:forEach var="feilmelding" items="${feilmeldinger}">
            <li>${feilmelding}</li>
          </c:forEach>
        </ul>
      </div>
    </c:if>

    <hr class="sep"/>

    <form:form class="form" method="post" modelAttribute="form">
      <div class="field">
        <label for="email">E-post</label>
        <form:input path="email" id="email" />
        <form:errors path="email" cssClass="muted" />
      </div>

      <div class="field">
        <label for="passord">Passord</label>
        <form:password path="passord" id="passord" />
        <form:errors path="passord" cssClass="muted" />
      </div>

      <div class="field">
        <label for="passordRep">Gjenta passord</label>
        <form:password path="passordRep" id="passordRep" />
        <form:errors path="passordRep" cssClass="muted" />
      </div>

      <div style="display:flex; justify-content:flex-end;">
        <button class="btn btn-primary" type="submit">Opprett bruker</button>
      </div>
    </form:form>

    <hr class="sep"/>
    <div class="muted">Har du allerede konto? <a href="<%=request.getContextPath()%>/login">Logg inn</a></div>
  </div>
</div>

</body>
</html>

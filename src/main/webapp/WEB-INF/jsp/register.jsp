<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="register.title"/></title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css?v=mobile-v2" />
  <c:url var="favIco" value="/favicon.ico"/>
  <c:url var="fav16" value="/favicon-16.png"/>
  <c:url var="fav32" value="/favicon-32.png"/>
  <c:url var="appleTouch" value="/apple-touch-icon.png"/>

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
        <div class="sub"><fmt:message key="register.subtitle"/></div>
      </div>
    </div>

    <div class="nav">
      <!-- språk -->
      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English" aria-label="English">🇬🇧</a>
      <a href="${toNb}" title="Norsk" aria-label="Norsk">🇳🇴</a>

      <span class="muted" style="margin:0 6px;">|</span>

      <a class="btn" href="<%=request.getContextPath()%>/"><fmt:message key="register.home"/></a>
      <a class="btn btn-primary" href="<%=request.getContextPath()%>/login"><fmt:message key="register.login"/></a>
    </div>
  </div>

  <div class="card" style="max-width:560px; margin:14px auto 0;">
    <h3><fmt:message key="register.header"/></h3>
    <div class="muted"><fmt:message key="register.lead"/></div>

    <!-- dine egne feilmeldinger-liste -->
    <c:if test="${not empty feilmeldinger}">
      <hr class="sep"/>
      <div class="notice error">
        <b><fmt:message key="register.errorsTitle"/></b>
        <ul style="margin:8px 0 0 18px;">
          <c:forEach var="feilmelding" items="${feilmeldinger}">
            <li><c:out value="${feilmelding}"/></li>
          </c:forEach>
        </ul>
      </div>
    </c:if>

    <hr class="sep"/>

    <form:form id="regForm" class="form" method="post" modelAttribute="form">
      <div class="field">
        <label for="email"><fmt:message key="register.email"/></label>
        <form:input path="email" id="email" type="email" autocomplete="email" inputmode="email" required="required" />
        <form:errors path="email" cssClass="muted" />
      </div>

      <div class="field">
        <label for="passord"><fmt:message key="register.password"/></label>
        <div style="display:flex; gap:8px; align-items:center;">
          <form:password path="passord" id="passord" autocomplete="new-password" required="required" style="flex:1;" />
          <button type="button" class="btn" id="togglePw1" style="white-space:nowrap;">
            <fmt:message key="register.show"/>
          </button>
        </div>
        <form:errors path="passord" cssClass="muted" />
      </div>

      <div class="field">
        <label for="passordRep"><fmt:message key="register.passwordRep"/></label>
        <div style="display:flex; gap:8px; align-items:center;">
          <form:password path="passordRep" id="passordRep" autocomplete="new-password" required="required" style="flex:1;" />
          <button type="button" class="btn" id="togglePw2" style="white-space:nowrap;">
            <fmt:message key="register.show"/>
          </button>
        </div>
        <form:errors path="passordRep" cssClass="muted" />
      </div>

      <div class="row" style="justify-content:flex-end; align-items:center; gap:10px;">
        <span id="regSpinner" class="muted" style="display:none;">⏳ <fmt:message key="register.creating"/></span>
        <button id="regBtn" class="btn btn-primary" type="submit"><fmt:message key="register.submit"/></button>
      </div>
    </form:form>

    <hr class="sep"/>
    <div class="muted">
      <fmt:message key="register.haveAccount"/>
      <a href="<%=request.getContextPath()%>/login"><fmt:message key="register.loginLink"/></a>
    </div>
  </div>
</div>

<script>
(function(){
  const pw1 = document.getElementById("passord");
  const pw2 = document.getElementById("passordRep");
  const b1 = document.getElementById("togglePw1");
  const b2 = document.getElementById("togglePw2");

  function toggle(input, btn){
    if (!input || !btn) return;
    const show = input.type === "password";
    input.type = show ? "text" : "password";
    btn.textContent = show ? "Hide" : "Show";
  }

  if (b1) b1.addEventListener("click", () => toggle(pw1, b1));
  if (b2) b2.addEventListener("click", () => toggle(pw2, b2));

  const form = document.getElementById("regForm");
  const btn = document.getElementById("regBtn");
  const sp = document.getElementById("regSpinner");

  if (form) {
    form.addEventListener("submit", function(){
      if (btn) btn.disabled = true;
      if (sp) sp.style.display = "inline";
    });
  }
})();
</script>

<script src="<c:url value='/assets/mobile-nav.js?v=mobile-v2'/>"></script>
</body>
</html>

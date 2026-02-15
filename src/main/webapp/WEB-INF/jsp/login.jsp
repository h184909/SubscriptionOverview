<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<html>
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="login.title"/></title>
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
        <div class="sub"><fmt:message key="login.subtitle"/></div>
      </div>
    </div>

    <div class="nav">
      <!-- språk -->
      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English" aria-label="English">🇬🇧</a>
      <a href="${toNb}" title="Norsk" aria-label="Norsk">🇳🇴</a>

      <span class="muted" style="margin:0 6px;">|</span>

      <a class="btn" href="<%=request.getContextPath()%>/"><fmt:message key="login.home"/></a>
      <a class="btn btn-primary" href="<%=request.getContextPath()%>/auth/register"><fmt:message key="login.register"/></a>
    </div>
  </div>

  <div class="card" style="max-width:520px; margin:14px auto 0;">
    <h3><fmt:message key="login.header"/></h3>
    <div class="muted"><fmt:message key="login.lead"/></div>
    <hr class="sep"/>

    <c:if test="${not empty loginError}">
      <div class="notice error"><b><c:out value="${loginError}"/></b></div>
      <div style="height:10px;"></div>
    </c:if>

    <form id="loginForm" class="form" method="post" action="<c:url value='/login'/>">
      <div class="field">
        <label for="email"><fmt:message key="login.email"/></label>
        <input id="email"
               type="email"
               name="email"
               value="<c:out value='${loginForm.email}'/>"
               autocomplete="email"
               inputmode="email"
               required />
      </div>

      <div class="field">
        <label for="passord"><fmt:message key="login.password"/></label>

        <div style="display:flex; gap:8px; align-items:center;">
          <input id="passord"
                 type="password"
                 name="passord"
                 autocomplete="current-password"
                 required
                 style="flex:1;" />
          <button type="button" class="btn" id="togglePw" style="white-space:nowrap;">
            <fmt:message key="login.show"/>
          </button>
        </div>

        <div class="muted" style="margin-top:6px;">
          <label style="display:flex; gap:8px; align-items:center; cursor:pointer;">
            <input type="checkbox" id="showPwBox" />
            <fmt:message key="login.showHint"/>
          </label>
        </div>
      </div>

      <div class="row" style="justify-content:space-between; align-items:center; gap:10px;">
        <a class="muted" href="<%=request.getContextPath()%>/auth/register">
          <fmt:message key="login.noUser"/>
        </a>

        <div style="display:flex; gap:10px; align-items:center;">
          <span id="loginSpinner" class="muted" style="display:none;">⏳ <fmt:message key="login.signingIn"/></span>
          <button id="loginBtn" class="btn btn-primary" type="submit"><fmt:message key="login.submit"/></button>
        </div>
      </div>
    </form>
  </div>
</div>

<script>
(function(){
  const pw = document.getElementById("passord");
  const btn = document.getElementById("togglePw");
  const box = document.getElementById("showPwBox");

  function setShow(show){
    if (!pw) return;
    pw.type = show ? "text" : "password";
    if (btn) btn.textContent = show ? "Hide" : "Show";
  }

  // bruker både knapp og checkbox
  if (btn) btn.addEventListener("click", () => setShow(pw && pw.type === "password"));
  if (box) box.addEventListener("change", () => setShow(box.checked));

  // disable + spinner på submit
  const form = document.getElementById("loginForm");
  const submitBtn = document.getElementById("loginBtn");
  const spinner = document.getElementById("loginSpinner");
  if (form) {
    form.addEventListener("submit", function(){
      if (submitBtn) submitBtn.disabled = true;
      if (spinner) spinner.style.display = "inline";
    });
  }
})();
</script>

</body>
</html>
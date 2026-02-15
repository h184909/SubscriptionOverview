<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
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
      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English" aria-label="English">🇬🇧</a>
      <a href="${toNb}" title="Norsk" aria-label="Norsk">🇳🇴</a>

      <span class="muted" style="margin:0 6px;">|</span>

      <a href="<%=request.getContextPath()%>/auth/register"><fmt:message key="login.register"/></a>
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

    <form class="form" method="post" action="<c:url value='/login'/>">
      <div class="field">
        <label for="email"><fmt:message key="login.email"/></label>
        <input id="email"
               type="text"
               name="email"
               value="${loginForm.email}"
               autocomplete="email" />
      </div>

      <div class="field">
        <label for="password"><fmt:message key="login.password"/></label>

        <!-- Passord + show-knapp (samme høyde) -->
        <div style="display:flex; gap:10px; align-items:stretch;">
          <input id="password"
                 type="password"
                 name="passord"
                 autocomplete="current-password"
                 style="flex:1; min-width:0;" />

          <button type="button"
                  id="togglePwBtn"
                  class="btn"
                  style="white-space:nowrap; height:100%; align-self:stretch;">
            <fmt:message key="login.show"/>
          </button>
        </div>

        <label class="muted" style="display:flex; gap:8px; align-items:center; margin-top:8px;">
          <input type="checkbox" id="togglePwBox" />
          <fmt:message key="login.showHint"/>
        </label>
      </div>

      <div class="row" style="justify-content:space-between; align-items:center; gap:10px;">
        <a class="muted" href="<%=request.getContextPath()%>/auth/register">
          <fmt:message key="login.noUser"/>
        </a>
        <button class="btn btn-primary" type="submit">
          <fmt:message key="login.submit"/>
        </button>
      </div>
    </form>
  </div>
</div>

<script>
(function(){
  const input = document.getElementById("password");
  const btn = document.getElementById("togglePwBtn");
  const box = document.getElementById("togglePwBox");
  if (!input || !btn || !box) return;

  function syncUI() {
    const isPw = input.type === "password";
    btn.textContent = isPw ? "<fmt:message key='login.show'/>" : "<fmt:message key='login.hide'/>";
    box.checked = !isPw;
    btn.setAttribute("aria-pressed", String(!isPw));
  }

  function toggle() {
    input.type = (input.type === "password") ? "text" : "password";
    syncUI();
    input.focus();
  }

  btn.addEventListener("click", toggle);
  box.addEventListener("change", toggle);

  syncUI();
})();
</script>

</body>
</html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html lang="${pageContext.request.locale.language}">
<head>
  <meta charset="UTF-8" />
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="csv.title"/></title>
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
        <div class="sub"><fmt:message key="csv.subtitle"/></div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
      <a href="<c:url value='/app/subscriptions'/>"><fmt:message key="nav.subscriptions"/></a>
      <a href="<c:url value='/app/analytics'/>">
        <fmt:message key="nav.analytics"/>
      </a>
      <a href="<c:url value='/app/suggestions'/>"><fmt:message key="nav.suggestions"/></a>
      <a href="<c:url value='/app/profile'/>">Profile</a>

      <span class="muted" style="margin:0 6px;">|</span>

      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English" aria-label="English">🇬🇧</a>
      <a href="${toNb}" title="Norsk" aria-label="Norsk">🇳🇴</a>
    </div>
  </div>

  <div class="card">
    <h3><fmt:message key="csv.header"/></h3>
    <div class="muted"><fmt:message key="csv.lead"/></div>
    <hr class="sep"/>

    <c:if test="${not empty flashMsg}">
      <div class="notice flash" role="status" aria-live="polite">
        <b><c:out value="${flashMsg}"/></b>
      </div>
      <div style="height:10px;"></div>
    </c:if>

    <div class="notice">
      <b><fmt:message key="csv.supportedTitle"/></b>
      <div class="muted" style="margin-top:6px;">
        <fmt:message key="csv.supportedText"/>
      </div>

      <div style="margin-top:10px;">
        <fmt:message key="csv.exampleLabel"/>
        <div><code>date,description,amount,currency,reference</code></div>
      </div>
    </div>

    <div style="height:10px;"></div>

    <form id="csvForm"
          class="form"
          method="post"
          action="<c:url value='/app/transactions/import-csv'/>"
          enctype="multipart/form-data">

      <div class="field">
        <label for="csvFile"><fmt:message key="csv.pickFile"/></label>
        <input id="csvFile" type="file" name="file" accept=".csv,text/csv" required />
        <div id="fileName" class="muted" style="margin-top:6px; display:none;"></div>
      </div>

      <div class="row" style="justify-content:flex-end; align-items:center; gap:10px;">
        <span id="csvSpinner" class="muted" style="display:none;">⏳ <fmt:message key="csv.importing"/></span>
        <button id="csvBtn" class="btn btn-primary" type="submit">
          <fmt:message key="csv.importBtn"/>
        </button>
      </div>
    </form>

    <hr class="sep"/>
    <div class="muted">
      <fmt:message key="csv.tip"/> <code>;</code>
    </div>
  </div>
</div>

<script>
(function(){
  const form = document.getElementById("csvForm");
  const btn = document.getElementById("csvBtn");
  const sp = document.getElementById("csvSpinner");
  const file = document.getElementById("csvFile");
  const name = document.getElementById("fileName");

  if (file && name) {
    file.addEventListener("change", () => {
      const f = file.files && file.files[0];
      if (!f) {
        name.style.display = "none";
        name.textContent = "";
        return;
      }
      name.style.display = "block";
      name.textContent = "📄 " + f.name;
    });
  }

  if (!form) return;
  form.addEventListener("submit", function(){
    if (btn) btn.disabled = true;
    if (sp) sp.style.display = "inline";
  });
})();
</script>
<script src="<c:url value='/assets/mobile-nav.js?v=mobile-v2'/>"></script>
</body>
</html>

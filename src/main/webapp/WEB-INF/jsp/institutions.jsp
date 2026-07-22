<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<html>
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="bank.title"/></title>
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
        <div class="sub"><fmt:message key="bank.subtitle"/></div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
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
    <div class="row" style="display:flex; justify-content:space-between; gap:12px; flex-wrap:wrap; align-items:center;">
      <div>
        <h3 style="margin:0;"><fmt:message key="bank.header"/></h3>
        <div class="muted" style="margin-top:6px;">
          <fmt:message key="bank.env"/>:
          <b><c:out value="${env}" /></b>
        </div>
      </div>

      <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
        <span class="pill ok">🔒 <fmt:message key="bank.secure"/></span>
      </div>
    </div>

    <c:if test="${empty institutions}">
      <hr class="sep"/>
      <div class="muted"><fmt:message key="bank.none"/></div>
    </c:if>

    <c:if test="${not empty institutions}">
      <hr class="sep"/>

      <!-- ✅ Søkefilter -->
      <div class="row" style="display:flex; gap:10px; align-items:center; flex-wrap:wrap; margin-bottom:10px;">
        <input id="bankSearch"
               type="text"
               placeholder="<fmt:message key='bank.search'/>"
               style="max-width:320px;"
               aria-label="Search institutions" />
        <span class="muted" id="bankCount"></span>
      </div>

      <div class="tablewrap">
        <table>
          <thead>
          <tr>
            <th><fmt:message key="bank.colName"/></th>
            <th><fmt:message key="bank.colId"/></th>
            <th><fmt:message key="bank.colAction"/></th>
          </tr>
          </thead>
          <tbody id="bankTbody">
          <c:forEach var="i" items="${institutions}">
            <tr>
              <td><b><c:out value="${i.name}" /></b></td>
              <td class="muted"><c:out value="${i.id}" /></td>
              <td>
                <c:url var="connectUrl" value="/openbanking/connect">
                  <c:param name="institutionId" value="${i.id}" />
                </c:url>
                <a class="btn btn-primary" href="${connectUrl}">
                  <fmt:message key="bank.connect"/>
                </a>
              </td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
      </div>

      <div class="muted" style="margin-top:12px;">
        <fmt:message key="bank.hint"/>
      </div>
    </c:if>
  </div>
</div>

<script>
(function(){
  const input = document.getElementById("bankSearch");
  const tbody = document.getElementById("bankTbody");
  const count = document.getElementById("bankCount");
  if (!input || !tbody) return;

  function updateCount(){
    const rows = Array.from(tbody.querySelectorAll("tr"));
    const visible = rows.filter(r => r.style.display !== "none").length;
    if (count) count.textContent = visible + " / " + rows.length;
  }

  input.addEventListener("input", function(){
    const q = (input.value || "").trim().toLowerCase();
    const rows = Array.from(tbody.querySelectorAll("tr"));

    rows.forEach(r => {
      const txt = r.textContent.toLowerCase();
      r.style.display = (!q || txt.includes(q)) ? "" : "none";
    });

    updateCount();
  });

  updateCount();
})();
</script>

<script src="<c:url value='/assets/mobile-nav.js?v=mobile-v2'/>"></script>
</body>
</html>

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<html>
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="subnew.title"/></title>
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
        <div class="sub"><fmt:message key="subnew.subtitle"/></div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
      <a href="<c:url value='/app/subscriptions'/>"><fmt:message key="subnew.back"/></a>

      <span class="muted" style="margin:0 6px;">|</span>

      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English" aria-label="English">🇬🇧</a>
      <a href="${toNb}" title="Norsk" aria-label="Norsk">🇳🇴</a>
    </div>
  </div>

  <div class="card">
    <h3><fmt:message key="subnew.header"/></h3>
    <div class="muted"><fmt:message key="subnew.lead"/></div>
    <hr class="sep"/>

    <form id="subNewForm" class="form" method="post" action="<c:url value='/app/subscriptions/new'/>">
      <div class="field">
        <label for="name"><fmt:message key="subnew.name"/></label>
        <input id="name" type="text" name="name" value="<c:out value='${form.name}'/>" maxlength="80" required />
      </div>

      <div class="field">
        <label for="amount"><fmt:message key="subnew.amount"/></label>
        <input id="amount" type="number" step="0.01" min="0" name="amount" value="<c:out value='${form.amount}'/>" required />
      </div>

      <div class="field">
        <label for="currency"><fmt:message key="subnew.currency"/></label>
        <input id="currency"
               type="text"
               name="currency"
               value="<c:out value='${form.currency}'/>"
               placeholder="NOK"
               maxlength="8"
               style="text-transform:uppercase;"
               oninput="this.value=this.value.toUpperCase();"/>
        <div class="muted"><fmt:message key="subnew.currencyHint"/></div>
      </div>

      <div class="field">
        <label for="interval"><fmt:message key="subnew.interval"/></label>
        <select id="interval" name="interval">
          <option value="WEEKLY"  <c:if test="${form.interval == 'WEEKLY'}">selected</c:if>><fmt:message key="interval.weekly"/></option>
          <option value="MONTHLY" <c:if test="${form.interval == 'MONTHLY'}">selected</c:if>><fmt:message key="interval.monthly"/></option>
          <option value="YEARLY"  <c:if test="${form.interval == 'YEARLY'}">selected</c:if>><fmt:message key="interval.yearly"/></option>
        </select>
      </div>

      <div class="field">
        <label for="nextChargeDate"><fmt:message key="subnew.nextDate"/></label>
        <input id="nextChargeDate" type="date" name="nextChargeDate" value="<c:out value='${form.nextChargeDate}'/>" />
      </div>

      <div class="field">
        <label for="billingEmail"><fmt:message key="subnew.billingEmail"/></label>
        <input id="billingEmail"
               type="email"
               name="billingEmail"
               value="<c:out value='${form.billingEmail}'/>"
               placeholder="name@example.com"
               autocomplete="email" />
        <div class="muted"><fmt:message key="subnew.billingHint"/></div>
      </div>

      <c:set var="br" value="${requestScope['org.springframework.validation.BindingResult.form']}" />
      <c:if test="${not empty br && br.hasErrors()}">
        <div class="notice error">
          <b><fmt:message key="common.fixErrors"/></b>
          <ul style="margin:8px 0 0 18px;">
            <c:forEach var="e" items="${br.allErrors}">
              <li><c:out value="${e.defaultMessage}"/></li>
            </c:forEach>
          </ul>
        </div>
      </c:if>

      <div class="row" style="justify-content:flex-end; align-items:center; gap:10px;">
        <span id="saveSpinner" class="muted" style="display:none;">⏳ <fmt:message key="common.saving"/></span>
        <button id="saveBtn" class="btn btn-primary" type="submit"><fmt:message key="common.save"/></button>
      </div>
    </form>
  </div>
</div>

<script>
(function(){
  const form = document.getElementById("subNewForm");
  const btn = document.getElementById("saveBtn");
  const sp = document.getElementById("saveSpinner");
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

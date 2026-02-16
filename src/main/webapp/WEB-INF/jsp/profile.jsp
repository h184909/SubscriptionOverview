<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html>
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="profile.title"/></title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />
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
      <div class="logo" aria-hidden="true"></div>
      <div>
        <h1>SubscriptionOverview</h1>
        <div class="sub"><fmt:message key="profile.subtitle"/></div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
      <a href="<c:url value='/app/subscriptions'/>"><fmt:message key="nav.subscriptions"/></a>
      <a href="<c:url value='/app/suggestions'/>"><fmt:message key="nav.suggestions"/></a>
      <a href="<c:url value='/app/transactions/import-csv'/>"><fmt:message key="nav.importCsv"/></a>

      <span class="muted" style="margin:0 6px;">|</span>

      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English" aria-label="English">🇬🇧</a>
      <a href="${toNb}" title="Norsk" aria-label="Norsk">🇳🇴</a>
    </div>
  </div>

  <c:if test="${not empty flashMsg}">
    <div class="card"><div class="notice flash"><b><c:out value="${flashMsg}"/></b></div></div>
  </c:if>

  <c:if test="${not empty flashError}">
    <div class="card"><div class="notice error"><b><c:out value="${flashError}"/></b></div></div>
  </c:if>

  <div class="grid two">
    <div class="card">
      <h3><fmt:message key="profile.accountTitle"/></h3>
      <div class="muted"><fmt:message key="profile.signedInAs"/> <b><c:out value="${email}"/></b></div>

      <hr class="sep"/>

      <h4 style="margin:0 0 8px;"><fmt:message key="profile.languageTitle"/></h4>
      <div class="muted" style="margin-bottom:10px;"><fmt:message key="profile.languageLead"/></div>

      <form class="form" method="post" action="<c:url value='/app/profile/language'/>">
        <div class="field">
          <label for="lang"><fmt:message key="profile.languageLabel"/></label>
          <select id="lang" name="lang">
            <c:set var="langVal" value="${empty preferredLanguage ? pageContext.request.locale.language : preferredLanguage}" />
            <option value="en" <c:if test="${langVal == 'en'}">selected</c:if>>English</option>
            <option value="nb" <c:if test="${langVal == 'nb'}">selected</c:if>>Norsk (Bokmål)</option>
          </select>
        </div>

        <div class="row" style="justify-content:flex-end;">
          <button class="btn btn-primary" type="submit"><fmt:message key="common.save"/></button>
        </div>
      </form>

      <hr class="sep"/>

      <h4 style="margin:0 0 8px;"><fmt:message key="profile.passwordTitle"/></h4>

      <form class="form" method="post" action="<c:url value='/app/profile/change-password'/>">
        <div class="field">
          <label><fmt:message key="profile.currentPassword"/></label>
          <input type="password" name="currentPassword" required />
        </div>

        <div class="field">
          <label><fmt:message key="profile.newPassword"/></label>
          <input type="password" name="newPassword" required />
        </div>

        <div class="field">
          <label><fmt:message key="profile.newPassword2"/></label>
          <input type="password" name="newPassword2" required />
          <div class="muted"><fmt:message key="profile.passwordHint"/></div>
        </div>

        <div class="row" style="justify-content:flex-end;">
          <button class="btn btn-primary" type="submit"><fmt:message key="common.save"/></button>
        </div>
      </form>
    </div>

    <div class="card">
      <h3><fmt:message key="profile.dangerTitle"/></h3>
      <div class="muted"><fmt:message key="profile.dangerLead"/></div>

      <hr class="sep"/>

      <form class="form" method="post" action="<c:url value='/app/profile/delete'/>"
            onsubmit="return confirm('<fmt:message key="profile.deleteConfirmJs"/>');">
        <div class="field">
          <label><fmt:message key="profile.password"/></label>
          <input type="password" name="password" required />
        </div>

        <div class="field">
          <label><fmt:message key="profile.typeDelete"/></label>
          <input type="text" name="confirm" placeholder="DELETE" required />
        </div>

        <div class="row" style="justify-content:flex-end;">
          <button class="btn btn-danger" type="submit"><fmt:message key="profile.deleteBtn"/></button>
        </div>
      </form>
    </div>
  </div>

</div>

</body>
</html>
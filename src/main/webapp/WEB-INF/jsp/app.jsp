<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<html>
<head>
  <title><fmt:message key="dash.title"/></title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
</head>
<body>

<fmt:setBundle basename="messages" />

<div class="container">
  <div class="topbar">
    <div class="brand">
      <div class="logo"></div>
      <div>
        <h1>SubscriptionOverview</h1>
        <div class="sub"><fmt:message key="dash.title"/></div>
      </div>
    </div>

    <!-- Nav + språk -->
    <div class="nav">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
      <a href="<c:url value='/app/subscriptions'/>"><fmt:message key="nav.subscriptions"/></a>
      <a href="<c:url value='/app/suggestions'/>"><fmt:message key="nav.suggestions"/></a>
      <a href="<c:url value='/app/transactions/import-csv'/>"><fmt:message key="nav.importCsv"/></a>

      <span class="muted" style="margin:0 6px;">|</span>

      <!-- behold samme side, men sett lang (lagres i session pga LocaleChangeInterceptor) -->
      <c:url var="self" value="${pageContext.request.requestURI}" />
      <a href="${self}?lang=en"><fmt:message key="lang.en"/></a>
      <a href="${self}?lang=nb"><fmt:message key="lang.no"/></a>
    </div>
  </div>

  <div class="grid two">
    <div class="card">
      <h3><fmt:message key="dash.status"/></h3>
      <div class="muted"><fmt:message key="dash.loggedInAs"/> <b><c:out value="${email}"/></b></div>

      <hr class="sep"/>

      <c:choose>
        <c:when test="${bankConnected}">
          <div class="pill ok">✅ Bank tilkoblet</div>
          <div style="margin-top:10px;">
            <a class="btn" href="<c:url value='/openbanking/institutions'/>">Koble til på nytt</a>
          </div>
        </c:when>
        <c:otherwise>
          <div class="pill warn">⚠️ Bank ikke tilkoblet</div>
          <div style="margin-top:10px;">
            <a class="btn btn-primary" href="<c:url value='/openbanking/institutions'/>">Koble til bank</a>
          </div>
        </c:otherwise>
      </c:choose>

      <c:if test="${showDevLinks}">
        <hr class="sep"/>
        <div class="notice">
          <b>Dev / test</b><br/>
          <a href="<c:url value='/openbanking/test/accounts'/>">Test: hent kontoer</a> ·
          <a href="<c:url value='/openbanking/import-and-suggest'/>">Importer transaksjoner + finn forslag</a>
        </div>
      </c:if>

      <hr class="sep"/>
      <form method="post" action="<c:url value='/logout'/>">
        <button class="btn btn-danger" type="submit"><fmt:message key="dash.logout"/></button>
      </form>
    </div>

    <div class="card">
      <h3><fmt:message key="dash.dueSoon"/></h3>

      <c:if test="${empty dueSoon}">
        <div class="muted"><fmt:message key="dash.noDueSoon"/></div>
      </c:if>

      <c:if test="${not empty dueSoon}">
        <div class="tablewrap" style="margin-top:10px;">
          <table>
            <thead>
            <tr>
              <th>Navn</th>
              <th>Neste trekkdato</th>
              <th>Pris</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${dueSoon}">
              <tr>
                <td><b><c:out value="${s.name}"/></b></td>
                <td><c:out value="${s.nextChargeDate}"/></td>
                <td><c:out value="${s.amount}"/> <c:out value="${s.currency}"/></td>
              </tr>
            </c:forEach>
            </tbody>
          </table>
        </div>
      </c:if>
    </div>
  </div>

  <div class="card">
    <h3><fmt:message key="dash.activeSubs"/></h3>

    <c:if test="${empty subs}">
      <div class="muted"><fmt:message key="dash.noActiveSubs"/></div>
    </c:if>

    <c:if test="${not empty subs}">
      <div class="tablewrap" style="margin-top:10px;">
        <table>
          <thead>
          <tr>
            <th>Navn</th>
            <th>Pris</th>
            <th>Intervall</th>
            <th>Neste trekkdato</th>
            <th>Ca. per måned</th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="s" items="${subs}">
            <tr>
              <td><c:out value="${s.name}"/></td>
              <td><c:out value="${s.amount}"/> <c:out value="${s.currency}"/></td>
              <td><c:out value="${s.interval}"/></td>
              <td>
                <c:choose>
                  <c:when test="${empty s.nextChargeDate}">-</c:when>
                  <c:otherwise><c:out value="${s.nextChargeDate}"/></c:otherwise>
                </c:choose>
              </td>
              <td><b><c:out value="${s.monthlyCost}"/></b> <c:out value="${s.currency}"/></td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
      </div>

      <div style="margin-top:12px;">
        <span class="pill ok">
          <fmt:message key="dash.totalMonthly"/> <b><c:out value="${totalMonthlyNok}"/> NOK</b>
        </span>
      </div>
    </c:if>
  </div>
</div>

</body>
</html>
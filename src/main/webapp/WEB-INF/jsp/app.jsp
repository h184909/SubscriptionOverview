<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html lang="${pageContext.request.locale.language}">
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="dash.title"/></title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />

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
        <div class="sub"><fmt:message key="dash.title"/></div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
      <a href="<c:url value='/app/subscriptions'/>"><fmt:message key="nav.subscriptions"/></a>
      <a href="<c:url value='/app/suggestions'/>"><fmt:message key="nav.suggestions"/></a>
      <a href="<c:url value='/app/transactions/import-csv'/>"><fmt:message key="nav.importCsv"/></a>
      <a href="<c:url value='/app/profile'/>"><fmt:message key="nav.profile"/></a>

      <span class="muted" style="margin:0 6px;">|</span>

      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English" aria-label="English">🇬🇧</a>
      <a href="${toNb}" title="Norsk" aria-label="Norsk">🇳🇴</a>
    </div>
  </div>

  <c:if test="${not empty flashMsg}">
    <div class="card">
      <div class="notice flash"><b><c:out value="${flashMsg}"/></b></div>
    </div>
  </c:if>

  <div class="grid two">
    <div class="card">
      <h3><fmt:message key="dash.status"/></h3>
      <div class="muted"><fmt:message key="dash.loggedInAs"/> <b><c:out value="${email}"/></b></div>

      <hr class="sep"/>

      <c:choose>
        <c:when test="${bankConnected}">
          <div class="pill ok">✅ <fmt:message key="dash.bankConnected"/></div>

          <div style="margin-top:12px; display:flex; gap:10px; flex-wrap:wrap; align-items:center;">
            <form method="post" action="<c:url value='/app/import-again'/>" style="margin:0;">
              <button class="btn btn-primary" type="submit">
                <fmt:message key="dash.bankSync"/>
              </button>
            </form>

            <a class="btn" href="<c:url value='/openbanking/institutions'/>">
              <fmt:message key="dash.bankReconnect"/>
            </a>

            <a class="btn" href="<c:url value='/app/profile'/>">
              <fmt:message key="dash.bankManage"/>
            </a>
          </div>

          <div class="muted" style="margin-top:10px;">
            <fmt:message key="dash.bankConnectedHint"/>
          </div>
        </c:when>

        <c:otherwise>
          <div class="pill warn">⚠️ <fmt:message key="dash.bankNotConnected"/></div>

          <div style="margin-top:10px;">
            <a class="btn btn-primary" href="<c:url value='/openbanking/institutions'/>">
              <fmt:message key="dash.bankConnect"/>
            </a>
          </div>

          <div class="muted" style="margin-top:10px;">
            <fmt:message key="dash.bankNotConnectedHint"/>
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
              <th><fmt:message key="table.name"/></th>
              <th><fmt:message key="table.nextCharge"/></th>
              <th><fmt:message key="table.price"/></th>
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
            <th><fmt:message key="table.name"/></th>
            <th><fmt:message key="table.price"/></th>
            <th><fmt:message key="table.interval"/></th>
            <th><fmt:message key="table.nextCharge"/></th>
            <th><fmt:message key="table.monthlyApprox"/></th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="s" items="${subs}">
            <tr>
              <td><c:out value="${s.name}"/></td>

              <td>
                <c:out value="${s.amount}"/>
                <c:choose>
                  <c:when test="${pageContext.request.locale.language == 'nb'}"> NOK</c:when>
                  <c:otherwise> <c:out value="${s.currency}"/></c:otherwise>
                </c:choose>
              </td>

              <td><c:out value="${s.interval}"/></td>

              <td>
                <c:choose>
                  <c:when test="${empty s.nextChargeDate}">-</c:when>
                  <c:otherwise><c:out value="${s.nextChargeDate}"/></c:otherwise>
                </c:choose>
              </td>

              <td>
                <b><c:out value="${s.monthlyCost}"/></b>
                <c:choose>
                  <c:when test="${pageContext.request.locale.language == 'nb'}"> NOK</c:when>
                  <c:otherwise> <c:out value="${s.currency}"/></c:otherwise>
                </c:choose>
              </td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
      </div>

      <div style="margin-top:12px;">
        <span class="pill ok">
          <fmt:message key="dash.totalMonthly"/>
          <b>
            <c:out value="${totalMonthlyNok}"/>
            NOK
          </b>
        </span>
      </div>
    </c:if>
  </div>
</div>

</body>
</html>
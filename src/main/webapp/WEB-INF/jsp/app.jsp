<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
  <title>Dashboard</title>
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
        <div class="sub">Dashboard</div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>">Dashboard</a>
      <a href="<c:url value='/app/subscriptions'/>">Abonnement</a>
      <a href="<c:url value='/app/suggestions'/>">Forslag</a>
      <a href="<c:url value='/app/transactions/import-csv'/>">Importer CSV</a>
    </div>
  </div>

  <div class="grid two">
    <div class="card">
      <h3>Din status</h3>
      <div class="muted">Innlogget som: <b>${email}</b></div>

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
        <button class="btn btn-danger" type="submit">Logg ut</button>
      </form>
    </div>

    <div class="card">
      <h3>Trekkes snart (innen 7 dager)</h3>

      <c:if test="${empty dueSoon}">
        <div class="muted">Ingen trekk de neste 7 dagene.</div>
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
                <td><b>${s.name}</b></td>
                <td>${s.nextChargeDate}</td>
                <td>${s.amount} ${s.currency}</td>
              </tr>
            </c:forEach>
            </tbody>
          </table>
        </div>
      </c:if>
    </div>
  </div>

  <div class="card">
    <h3>Aktive abonnement:</h3>

    <c:if test="${empty subs}">
      <div class="muted">Ingen aktive abonnement 🎉</div>
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
              <td>${s.name}</td>
              <td>${s.amount} ${s.currency}</td>
              <td>${s.interval}</td>
              <td>
                <c:if test="${empty s.nextChargeDate}">-</c:if>
                <c:if test="${not empty s.nextChargeDate}">${s.nextChargeDate}</c:if>
              </td>
              <td><b>${s.monthlyCost}</b> ${s.currency}</td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
      </div>

      <div style="margin-top:12px;">
        <span class="pill ok">Total ca. per måned: <b>${totalMonthlyNok} NOK</b></span>
      </div>
    </c:if>
  </div>
</div>

</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
  <title>Abonnement</title>
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
        <div class="sub">Abonnement</div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>">Dashboard</a>
      <a href="<c:url value='/app/suggestions'/>">Forslag</a>
      <a href="<c:url value='/app/transactions/import-csv'/>">Importer CSV</a>
    </div>
  </div>

  <div class="card">
    <div class="row" style="display:flex; justify-content:space-between; gap:12px; flex-wrap:wrap; align-items:center;">
      <div>
        <h3 style="margin:0;">Dine abonnement</h3>
        <div class="muted" style="margin-top:6px;">Her kan du administrere aktive og avsluttede abonnement.</div>
      </div>

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <a class="btn btn-primary" href="<c:url value='/app/subscriptions/new'/>">+ Legg til abonnement</a>
        <a class="btn" href="<c:url value='/app/suggestions'/>">Se forslag</a>
      </div>
    </div>
  </div>

  <c:if test="${empty subs}">
    <div class="card">
      <h3>Ingen abonnement enda</h3>
      <div class="muted">Legg til manuelt eller godta forslag.</div>
    </div>
  </c:if>

  <c:if test="${not empty subs}">
    <div class="card">
      <div class="tablewrap">
        <table>
          <thead>
          <tr>
            <th>Navn</th>
            <th>Pris</th>
            <th>Intervall</th>
            <th>Neste trekkdato</th>
            <th>Status</th>
            <th>Avslutt hos leverandør</th>
            <th>Handling</th>
          </tr>
          </thead>
          <tbody>

          <c:forEach var="s" items="${subs}">
            <tr>
              <td><b><c:out value="${s.name}" /></b></td>
              <td><c:out value="${s.amount}" /> <c:out value="${s.currency}" /></td>
              <td><c:out value="${s.interval}" /></td>
              <td>
                <c:choose>
                  <c:when test="${empty s.nextChargeDate}">-</c:when>
                  <c:otherwise><c:out value="${s.nextChargeDate}" /></c:otherwise>
                </c:choose>
              </td>
              <td>
                <c:choose>
                  <c:when test="${s.active}"><span class="pill ok">Aktiv</span></c:when>
                  <c:otherwise><span class="pill warn">Avsluttet</span></c:otherwise>
                </c:choose>
              </td>

              <!-- ✅ Flyttet hit: bare hvis aktivt abonnement -->
              <td>
                <c:choose>
                  <c:when test="${s.active}">
                    <c:set var="cancelUrl" value="${cancelLinks[s.id]}" />
                    <c:if test="${not empty cancelUrl}">
                      <a href="${cancelUrl}" target="_blank" rel="noopener">Avslutt</a>
                    </c:if>
                    <c:if test="${empty cancelUrl}">-</c:if>
                  </c:when>
                  <c:otherwise>
                    -
                  </c:otherwise>
                </c:choose>
              </td>

              <td>
                <c:choose>
                  <c:when test="${s.active}">
                    <form method="post" action="<c:url value='/app/subscriptions/cancel'/>"
                          onsubmit="return confirm('Avslutte abonnementet: ${s.name}?');"
                          style="display:inline;">
                      <input type="hidden" name="id" value="${s.id}" />
                      <button type="submit" class="btn">Avslutt i app</button>
                    </form>
                  </c:when>
                  <c:otherwise>
                    <form method="post" action="<c:url value='/app/subscriptions/reactivate'/>"
                          onsubmit="return confirm('Aktivere abonnementet igjen: ${s.name}?');"
                          style="display:inline;">
                      <input type="hidden" name="id" value="${s.id}" />
                      <button type="submit" class="btn">Aktiver</button>
                    </form>
                  </c:otherwise>
                </c:choose>

                <form method="post" action="<c:url value='/app/subscriptions/delete'/>"
                      onsubmit="return confirm('Slette abonnementet PERMANENT: ${s.name}?');"
                      style="display:inline; margin-left:6px;">
                  <input type="hidden" name="id" value="${s.id}" />
                  <button type="submit" class="btn">Slett</button>
                </form>
              </td>
            </tr>
          </c:forEach>

          </tbody>
        </table>
      </div>
    </div>
  </c:if>

</div>

</body>
</html>

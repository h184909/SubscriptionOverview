<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<html>
<head>
  <title>OpenBanking</title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
</head>
<body>

<div class="container">
  <div class="topbar">
    <div class="brand">
logo">
      <div class="logo"></div>
      <div>
        <h1>SubscriptionOverview</h1>
        <div class="sub">Open Banking</div>
      </div>
    </div>
    <div class="nav">
      <a href="<c:url value='/app'/>">Dashboard</a>
      <a href="<c:url value='/openbanking/institutions'/>">Velg bank</a>
      <a href="<c:url value='/app/suggestions'/>">Forslag</a>
    </div>
  </div>

  <div class="card">
    <h3>Kontoer</h3>
    <div class="muted">Velg en konto for å importere transaksjoner.</div>

    <c:if test="${not empty msg}">
      <hr class="sep"/>
      <div class="notice flash"><b>${msg}</b></div>
    </c:if>

    <c:if test="${empty accounts}">
      <hr class="sep"/>
      <div class="muted">Ingen kontoer funnet.</div>
    </c:if>

    <c:if test="${not empty accounts}">
      <hr class="sep"/>
      <div class="tablewrap">
        <table>
          <thead>
          <tr>
            <th>ID</th>
            <th>Nickname</th>
            <th>Valuta</th>
            <th>Handling</th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="a" items="${accounts}">
            <tr>
              <td class="muted">${a.id}</td>
              <td><b>${a.nickname}</b></td>
              <td>${a.currency}</td>
              <td>
                <a class="btn btn-primary"
                   href="<c:url value='/openbanking/test/transactions'><c:param name='accountId' value='${a.id}'/></c:url>">
                  Importer transaksjoner
                </a>
              </td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
      </div>
    </c:if>

    <hr class="sep"/>
    <a class="btn" href="<c:url value='/app'/>">Tilbake til dashboard</a>
  </div>
</div>

</body>
</html>

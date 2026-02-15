<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
  <title>Ny transaksjon</title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />
  <link rel="icon" href="<c:url value='/assets/favicon.ico'/>" />
  <link rel="icon" type="image/png" sizes="32x32" href="<c:url value='/assets/favicon-32.png'/>" />
  <link rel="icon" type="image/png" sizes="16x16" href="<c:url value='/assets/favicon-16.png'/>" />
  <link rel="apple-touch-icon" sizes="180x180" href="<c:url value='/assets/apple-touch-icon.png'/>" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
</head>
<body>

<div class="container">
  <div class="topbar">
    <div class="brand">
      <div class="logo"></div>
      <div>
        <h1>SubscriptionOverview</h1>
        <div class="sub">Ny transaksjon</div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>">Dashboard</a>
      <a href="<c:url value='/app/suggestions'/>">Forslag</a>
      <a href="<c:url value='/app/transactions/import-csv'/>">Importer CSV</a>
    </div>
  </div>

  <div class="card">
    <h3>Legg inn transaksjon (MVP)</h3>
    <div class="muted">Brukes for enkel testing hvis du ikke har CSV.</div>

    <hr class="sep"/>

    <form class="form" method="post" action="<c:url value='/app/transactions/new'/>">
      <div class="field">
        <label>Dato (yyyy-MM-dd)</label>
        <input name="bookedDate" value="${form.bookedDate}" />
        <c:if test="${not empty errors && errors.bookedDate != null}">
          <div class="muted" style="margin-top:6px; color:#fecdd3;">${errors.bookedDate}</div>
        </c:if>
      </div>

      <div class="field">
        <label>Beskrivelse</label>
        <input name="description" value="${form.description}" />
      </div>

      <div class="field">
        <label>Beløp</label>
        <input name="amount" value="${form.amount}" />
        <div class="muted">Kan være negativ (trekk) eller positiv.</div>
      </div>

      <div class="field">
        <label>Valuta</label>
        <input name="currency" value="${form.currency}" placeholder="NOK" />
      </div>

      <div style="display:flex; justify-content:flex-end;">
        <button class="btn btn-primary" type="submit">Lagre</button>
      </div>
    </form>
  </div>
</div>

</body>
</html>

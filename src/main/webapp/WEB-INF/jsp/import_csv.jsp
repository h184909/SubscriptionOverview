<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html>
<head>
  <meta charset="UTF-8" />
  <title>CSV-import</title>
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
        <div class="sub">Importer transaksjoner</div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>">Dashboard</a>
      <a href="<c:url value='/app/suggestions'/>">Forslag</a>
      <a href="<c:url value='/app/subscriptions'/>">Abonnement</a>
    </div>
  </div>

  <div class="card">
    <h3>Importer transaksjoner (CSV)</h3>
    <div class="muted">Last opp CSV fra banken din for å finne abonnement automatisk.</div>
    <hr class="sep"/>

    <c:if test="${not empty flashMsg}">
      <div class="notice flash"><b><c:out value="${flashMsg}"/></b></div>
      <div style="height:10px;"></div>
    </c:if>

    <div class="notice">
      <b>Støttede kolonner</b>
      <div class="muted" style="margin-top:6px;">
        Appen støtter både “standard-format” og mange norske bank-varianter (dato/bokført, beløp/inn/ut, beskrivelse/tekst osv).
      </div>
      <div style="margin-top:10px;">
        Eksempel (standard):
        <div><code>date,description,amount,currency,reference</code></div>
      </div>
    </div>

    <div style="height:10px;"></div>

    <form id="csvForm" class="form" method="post" action="<c:url value='/app/transactions/import-csv'/>" enctype="multipart/form-data">
      <div class="field">
        <label>Velg CSV-fil</label>
        <input type="file" name="file" accept=".csv,text/csv" required />
      </div>

      <div class="row" style="justify-content:flex-end; align-items:center; gap:10px;">
        <span id="csvSpinner" class="muted" style="display:none;">⏳ Importerer…</span>
        <button id="csvBtn" class="btn btn-primary" type="submit">Importer CSV</button>
      </div>
    </form>

    <hr class="sep"/>
    <div class="muted">
      Tips: Hvis banken bruker semikolon (<code>;</code>) i stedet for komma, går det fint — appen gjetter separator automatisk.
    </div>
  </div>
</div>

<script>
(function(){
  const form = document.getElementById("csvForm");
  const btn = document.getElementById("csvBtn");
  const sp = document.getElementById("csvSpinner");

  if (!form) return;

  form.addEventListener("submit", function(){
    if (btn) btn.disabled = true;
    if (sp) sp.style.display = "inline";
  });
})();
</script>

</body>
</html>

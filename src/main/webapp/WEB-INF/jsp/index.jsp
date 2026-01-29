<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
  <title>SubscriptionOverview</title>
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
        <div class="sub">Hold oversikt over abonnementene dine</div>
      </div>
    </div>

    <div class="nav">
      <a class="btn" href="<%=request.getContextPath()%>/login">Logg inn</a>
      <a class="btn btn-primary" href="<%=request.getContextPath()%>/auth/register">Registrer</a>
    </div>
  </div>

  <div class="grid two">
    <div class="card">
      <h2 style="margin:0 0 8px 0;">Få kontroll på abonnement</h2>
      <div class="muted">
        Importer transaksjoner og få forslag til abonnement automatisk. Godta forslag og få oversikt i dashboardet.
      </div>

      <hr class="sep"/>

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <a class="btn btn-primary" href="<%=request.getContextPath()%>/login">Kom i gang</a>
        <a class="btn" href="<%=request.getContextPath()%>/auth/register">Opprett konto</a>
      </div>
    </div>

    <div class="card">
      <h3>Hvordan det funker</h3>
      <ul style="margin:8px 0 0 18px; color:var(--muted);">
        <li>Importer CSV fra nettbanken (nå)</li>
        <li>Se forslag og godta/avvis</li>
        <li>Se aktive abonnement og kostnad per måned</li>
        <li>Open Banking kobles på senere</li>
      </ul>
      <hr class="sep"/>
      <div class="notice">
        <b>Tips:</b> Start med CSV-import for å teste Netflix/Spotify osv.
      </div>
    </div>
  </div>
</div>

</body>
</html>

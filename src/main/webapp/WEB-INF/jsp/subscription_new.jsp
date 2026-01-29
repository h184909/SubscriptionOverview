<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
  <title>Nytt abonnement</title>
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
        <div class="sub">Legg til abonnement</div>
      </div>
    </div>
    <div class="nav">
      <a href="<c:url value='/app'/>">Dashboard</a>
      <a href="<c:url value='/app/subscriptions'/>">Tilbake</a>
    </div>
  </div>

  <div class="card">
    <h3>Legg til abonnement manuelt</h3>
    <div class="muted">Fyll inn detaljene under.</div>
    <hr class="sep"/>

    <form class="form" method="post" action="<c:url value='/app/subscriptions/new'/>">
      <div class="field">
        <label>Navn</label>
        <input type="text" name="name" value="${form.name}" />
      </div>

      <div class="field">
        <label>Pris per periode</label>
        <input type="number" step="0.01" name="amount" value="${form.amount}" />
      </div>

      <div class="field">
        <label>Valuta</label>
        <input type="text" name="currency" value="${form.currency}" placeholder="NOK" />
        <div class="muted">f.eks. NOK, EUR, USD</div>
      </div>

      <div class="field">
        <label>Intervall</label>
        <select name="interval">
          <option value="WEEKLY"  <c:if test="${form.interval == 'WEEKLY'}">selected</c:if>>Ukentlig</option>
          <option value="MONTHLY" <c:if test="${form.interval == 'MONTHLY'}">selected</c:if>>Månedlig</option>
          <option value="YEARLY"  <c:if test="${form.interval == 'YEARLY'}">selected</c:if>>Årlig</option>
        </select>
      </div>

      <div class="field">
        <label>Neste trekkdato (valgfri)</label>
        <input type="date" name="nextChargeDate" value="${form.nextChargeDate}" />
      </div>

      <div class="field">
        <label>Abonnements-/faktura-e-post (valgfri)</label>
        <input type="text" name="billingEmail" value="${form.billingEmail}" />
      </div>

      <c:set var="br" value="${requestScope['org.springframework.validation.BindingResult.form']}" />
      <c:if test="${not empty br && br.hasErrors()}">
        <div class="notice error">
          <b>Noe må fikses:</b>
          <ul style="margin:8px 0 0 18px;">
            <c:forEach var="e" items="${br.allErrors}">
              <li>${e.defaultMessage}</li>
            </c:forEach>
          </ul>
        </div>
      </c:if>

      <div class="row" style="justify-content:flex-end;">
        <button class="btn btn-primary" type="submit">Lagre</button>
      </div>
    </form>
  </div>
</div>

</body>
</html>

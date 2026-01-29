<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
  <title>Forslag</title>
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
        <div class="sub">Forslag</div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>">Dashboard</a>
      <a href="<c:url value='/app/subscriptions'/>">Abonnement</a>
      <a href="<c:url value='/app/transactions/import-csv'/>">Importer CSV</a>
    </div>
  </div>

  <c:set var="isImportRunning" value="${importState == 'RUNNING'}" />

  <c:if test="${not empty flashMsg}">
    <div class="card">
      <div class="notice flash"><b><c:out value="${flashMsg}"/></b></div>
    </div>
  </c:if>

  <!-- Import-status -->
  <div id="importBox" class="card">
    <div class="row" style="display:flex; justify-content:space-between; gap:12px; flex-wrap:wrap; align-items:center;">
      <div>
        <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
          <b>Import-status:</b>
          <span id="importState"><c:out value="${importState}" /></span>
          <span class="muted">–</span>
          <span id="importMsg"><c:out value="${importStatus}" /></span>
          <span id="importSpinner" style="display:none;">⏳</span>
        </div>
        <div class="muted" style="margin-top:6px;">
          Importen henter transaksjoner og oppdaterer forslag automatisk.
        </div>
      </div>

      <div style="display:flex; gap:10px; flex-wrap:wrap; align-items:center;">
        <c:choose>
          <c:when test="${bankConnected}">
            <span class="pill ok">✅ Bank tilkoblet</span>
            <form method="post" action="<c:url value='/app/import-again'/>" style="margin:0;">
              <button type="submit" id="importAgainBtn" class="btn btn-primary">Importer på nytt</button>
            </form>
          </c:when>
          <c:otherwise>
            <span class="pill warn">⚠️ Bank ikke tilkoblet</span>
            <a class="btn btn-primary" href="<c:url value='/openbanking/institutions'/>">Koble til bank</a>
          </c:otherwise>
        </c:choose>
      </div>
    </div>

    <c:if test="${isImportRunning}">
      <hr class="sep"/>
      <div class="notice">
        <b>Importerer transaksjoner…</b> Forslag blir tilgjengelige når importen er ferdig.
      </div>
    </c:if>

    <div id="actionsDisabledInfo" class="notice" style="display:none; margin-top:10px;">
      <b>Venter på import…</b> Du kan godta/avvise forslag når importen er ferdig.
    </div>
  </div>

  <c:if test="${empty suggestions}">
    <div class="card">
      <h3>Ingen forslag akkurat nå</h3>
      <div class="muted">
        Tips: importer en CSV med transaksjoner (Netflix/Spotify osv) for å teste abonnementsgjenkjenning.
      </div>
    </div>
  </c:if>

  <c:if test="${not empty suggestions}">

    <!-- KJENTE TJENESTER -->
    <div class="card">
      <h3>Kjente tjenester</h3>
      <div class="muted">Tjenester vi gjenkjenner.</div>

      <c:set var="hasKnown" value="false" />
      <c:forEach var="s" items="${suggestions}">
        <c:if test="${s.knownProvider}">
          <c:set var="hasKnown" value="true" />
        </c:if>
      </c:forEach>

      <c:if test="${not hasKnown}">
        <hr class="sep"/>
        <div class="muted">Ingen kjente tjenester funnet akkurat nå.</div>
      </c:if>

      <c:if test="${hasKnown}">
        <hr class="sep"/>
        <div class="tablewrap">
          <table>
            <thead>
            <tr>
              <th>Navn</th>
              <th>Beløp</th>
              <th>Intervall</th>
              <th>Sist</th>
              <th>Neste</th>
              <th>Obs.</th>
              <th>Confidence</th>
              <th>Handling</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${suggestions}">
              <c:if test="${s.knownProvider}">
                <tr>
                  <td>
                    <b><c:out value="${s.name}" /></b>
                    <span class="badge-popular">popular</span>
                  </td>
                  <td><c:out value="${s.amount}" /> <c:out value="${s.currency}" /></td>
                  <td><c:out value="${s.interval}" /></td>
                  <td><c:out value="${s.lastChargeDate}" /></td>
                  <td><c:out value="${s.nextExpectedDate}" /></td>
                  <td><c:out value="${s.occurrences}" /></td>
                  <td><c:out value="${s.confidence}" /></td>
                  <td>
                    <form method="post" action="<c:url value='/app/suggestions/accept'/>" style="display:inline;">
                      <input type="hidden" name="key" value="${s.key}" />
                      <button type="submit" class="btn suggestion-action">Godta</button>
                    </form>
                    <form method="post" action="<c:url value='/app/suggestions/reject'/>" style="display:inline; margin-left:6px;">
                      <input type="hidden" name="key" value="${s.key}" />
                      <button type="submit" class="btn suggestion-action">Avvis</button>
                    </form>
                  </td>
                </tr>
              </c:if>
            </c:forEach>
            </tbody>
          </table>
        </div>
      </c:if>
    </div>

    <!-- ANDRE -->
    <div class="card">
      <h3>Andre forslag</h3>
      <div class="muted">Mulige abonnement som vi ikke har en “kjent leverandør” for.</div>

      <c:set var="hasUnknown" value="false" />
      <c:forEach var="s" items="${suggestions}">
        <c:if test="${not s.knownProvider}">
          <c:set var="hasUnknown" value="true" />
        </c:if>
      </c:forEach>

      <c:if test="${not hasUnknown}">
        <hr class="sep"/>
        <div class="muted">Ingen andre forslag.</div>
      </c:if>

      <c:if test="${hasUnknown}">
        <hr class="sep"/>
        <div class="tablewrap">
          <table>
            <thead>
            <tr>
              <th>Navn</th>
              <th>Beløp</th>
              <th>Intervall</th>
              <th>Sist</th>
              <th>Neste</th>
              <th>Obs.</th>
              <th>Confidence</th>
              <th>Handling</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${suggestions}">
              <c:if test="${not s.knownProvider}">
                <tr>
                  <td><c:out value="${s.name}" /></td>
                  <td><c:out value="${s.amount}" /> <c:out value="${s.currency}" /></td>
                  <td><c:out value="${s.interval}" /></td>
                  <td><c:out value="${s.lastChargeDate}" /></td>
                  <td><c:out value="${s.nextExpectedDate}" /></td>
                  <td><c:out value="${s.occurrences}" /></td>
                  <td><c:out value="${s.confidence}" /></td>
                  <td>
                    <form method="post" action="<c:url value='/app/suggestions/accept'/>" style="display:inline;">
                      <input type="hidden" name="key" value="${s.key}" />
                      <button type="submit" class="btn suggestion-action">Godta</button>
                    </form>
                    <form method="post" action="<c:url value='/app/suggestions/reject'/>" style="display:inline; margin-left:6px;">
                      <input type="hidden" name="key" value="${s.key}" />
                      <button type="submit" class="btn suggestion-action">Avvis</button>
                    </form>
                  </td>
                </tr>
              </c:if>
            </c:forEach>
            </tbody>
          </table>
        </div>
      </c:if>
    </div>

  </c:if>
</div>

<script>
  (function () {
    const stateEl = document.getElementById("importState");
    const msgEl = document.getElementById("importMsg");
    const spinner = document.getElementById("importSpinner");

    const actions = document.querySelectorAll(".suggestion-action");
    const actionsInfo = document.getElementById("actionsDisabledInfo");
    const importAgainBtn = document.getElementById("importAgainBtn");

    function setSpinner(on) { if (spinner) spinner.style.display = on ? "inline" : "none"; }

    function setActionsEnabled(enabled) {
      actions.forEach(btn => btn.disabled = !enabled);
      if (actionsInfo) actionsInfo.style.display = enabled ? "none" : "block";
      if (importAgainBtn) importAgainBtn.disabled = !enabled;
    }

    function poll() {
      fetch("<c:url value='/app/import-status'/>", { headers: { "Accept": "application/json" } })
        .then(r => r.json())
        .then(data => {
          const state = (data.state || "").toUpperCase();
          const msg = data.message || "";

          if (stateEl) stateEl.textContent = state;
          if (msgEl) msgEl.textContent = msg;

          if (state === "RUNNING") {
            setSpinner(true);
            setActionsEnabled(false);
            setTimeout(poll, 2000);
            return;
          }

          setSpinner(false);
          setActionsEnabled(true);
        })
        .catch(() => setTimeout(poll, 4000));
    }

    const initial = ((stateEl && stateEl.textContent) || "").toUpperCase();
    if (initial === "RUNNING") {
      setSpinner(true);
      setActionsEnabled(false);
      setTimeout(poll, 600);
    } else {
      setActionsEnabled(true);
    }
  })();
</script>

</body>
</html>

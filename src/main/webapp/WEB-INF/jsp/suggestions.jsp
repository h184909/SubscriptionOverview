<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<html>
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="sugg.title"/></title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />
  <c:url var="favIco" value="/favicon.ico"/>
  <c:url var="fav16" value="/favicon-16.png"/>
  <c:url var="fav32" value="/favicon-32.png"/>
  <c:url var="appleTouch" value="/apple-touch-icon.png"/>

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
        <div class="sub"><fmt:message key="sugg.subtitle"/></div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
      <a href="<c:url value='/app/subscriptions'/>"><fmt:message key="nav.subscriptions"/></a>
      <a href="<c:url value='/app/analytics'/>">
        <fmt:message key="nav.analytics"/>
      </a>
      <a href="<c:url value='/app/transactions/import-csv'/>"><fmt:message key="nav.importCsv"/></a>
      <a href="<c:url value='/app/profile'/>">Profile</a>

      <span class="muted" style="margin:0 6px;">|</span>

      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English" aria-label="English">🇬🇧</a>
      <a href="${toNb}" title="Norsk" aria-label="Norsk">🇳🇴</a>
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
          <b><fmt:message key="sugg.importStatus"/>:</b>
          <span id="importState"><c:out value="${importState}" /></span>
          <span class="muted">–</span>
          <span id="importMsg"><c:out value="${importStatus}" /></span>
          <span id="importSpinner" style="display:none;">⏳</span>
        </div>
        <div class="muted" style="margin-top:6px;">
          <fmt:message key="sugg.importHint"/>
        </div>
      </div>

      <div style="display:flex; gap:10px; flex-wrap:wrap; align-items:center;">
        <c:choose>
          <c:when test="${bankConnected}">
            <span class="pill ok">✅ <fmt:message key="sugg.bankConnected"/></span>
            <form method="post" action="<c:url value='/app/import-again'/>" style="margin:0;">
              <button type="submit" id="importAgainBtn" class="btn btn-primary">
                <fmt:message key="sugg.importAgain"/>
              </button>
            </form>
          </c:when>
          <c:otherwise>
            <span class="pill warn">⚠️ <fmt:message key="sugg.bankNotConnected"/></span>
            <a class="btn btn-primary" href="<c:url value='/openbanking/institutions'/>">
              <fmt:message key="sugg.connectBank"/>
            </a>
          </c:otherwise>
        </c:choose>
      </div>
    </div>

    <c:if test="${isImportRunning}">
      <hr class="sep"/>
      <div class="notice">
        <b><fmt:message key="sugg.importingTitle"/></b>
        <fmt:message key="sugg.importingText"/>
      </div>
    </c:if>

    <div id="actionsDisabledInfo" class="notice" style="display:none; margin-top:10px;">
      <b><fmt:message key="sugg.waitingTitle"/></b>
      <fmt:message key="sugg.waitingText"/>
    </div>
  </div>

  <c:if test="${empty suggestions}">
    <div class="card">
      <h3><fmt:message key="sugg.noneTitle"/></h3>
      <div class="muted"><fmt:message key="sugg.noneText"/></div>
    </div>
  </c:if>

  <c:if test="${not empty suggestions}">

    <!-- Bulk header -->
    <div class="card" style="display:flex; justify-content:space-between; gap:12px; flex-wrap:wrap; align-items:center;">
      <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
        <label class="muted" style="display:flex; align-items:center; gap:8px;">
          <input type="checkbox" id="selectAllBox" />
          <fmt:message key="sugg.selectAll"/>
        </label>
        <span class="muted" id="selectedCount">0</span>
      </div>

      <div style="display:flex; gap:10px; flex-wrap:wrap; align-items:center;">
        <c:if test="${hiddenCount != null && hiddenCount > 0}">
          <form method="post" action="${pageContext.request.contextPath}/app/suggestions/reset-hidden" style="margin:0;">
            <button type="submit" class="btn btn-secondary">
              <fmt:message key="sugg.showRejected"/> (<c:out value="${hiddenCount}" />)
            </button>
          </form>
        </c:if>
      </div>
    </div>

    <!-- Form for bulk actions -->
    <form id="bulkForm" method="post" action="<c:url value='/app/suggestions/accept-bulk'/>" style="margin:0;">
      <div class="card" style="display:flex; gap:10px; flex-wrap:wrap; align-items:center;">
        <button type="submit" id="bulkAcceptBtn" class="btn btn-primary" disabled>
          <fmt:message key="sugg.acceptSelected"/>
        </button>
        <button type="button" id="bulkRejectBtn" class="btn btn-secondary" disabled>
          <fmt:message key="sugg.rejectSelected"/>
        </button>
        <span class="muted"><fmt:message key="sugg.singleRowHint"/></span>
      </div>

      <!-- Known -->
      <div class="card">
        <h3><fmt:message key="sugg.knownTitle"/></h3>
        <div class="muted"><fmt:message key="sugg.knownText"/></div>

        <c:set var="hasKnown" value="false" />
        <c:forEach var="s" items="${suggestions}">
          <c:if test="${s.knownProvider}">
            <c:set var="hasKnown" value="true" />
          </c:if>
        </c:forEach>

        <c:if test="${not hasKnown}">
          <hr class="sep"/>
          <div class="muted"><fmt:message key="sugg.knownNone"/></div>
        </c:if>

        <c:if test="${hasKnown}">
          <hr class="sep"/>
          <div class="tablewrap">
            <table>
              <thead>
              <tr>
                <th style="width:40px;"></th>
                <th><fmt:message key="table.name"/></th>
                <th><fmt:message key="table.amount"/></th>
                <th><fmt:message key="table.interval"/></th>
                <th><fmt:message key="sugg.last"/></th>
                <th><fmt:message key="sugg.next"/></th>
                <th><fmt:message key="sugg.count"/></th>
                <th><fmt:message key="sugg.confidence"/></th>
                <th><fmt:message key="table.action"/></th>
              </tr>
              </thead>
              <tbody>
              <c:forEach var="s" items="${suggestions}">
                <c:if test="${s.knownProvider}">
                  <tr>
                    <td><input type="checkbox" class="rowCheck" name="keys" value="${s.key}" /></td>
                    <td>
                      <b><c:out value="${s.name}" /></b>
                      <span class="badge-popular"><fmt:message key="sugg.popular"/></span>
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
                        <button type="submit" class="btn suggestion-action"><fmt:message key="common.accept"/></button>
                      </form>
                      <form method="post" action="<c:url value='/app/suggestions/reject'/>" style="display:inline; margin-left:6px;">
                        <input type="hidden" name="key" value="${s.key}" />
                        <button type="submit" class="btn suggestion-action"><fmt:message key="common.reject"/></button>
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

      <!-- Unknown -->
      <div class="card">
        <h3><fmt:message key="sugg.otherTitle"/></h3>
        <div class="muted"><fmt:message key="sugg.otherText"/></div>

        <c:set var="hasUnknown" value="false" />
        <c:forEach var="s" items="${suggestions}">
          <c:if test="${not s.knownProvider}">
            <c:set var="hasUnknown" value="true" />
          </c:if>
        </c:forEach>

        <c:if test="${not hasUnknown}">
          <hr class="sep"/>
          <div class="muted"><fmt:message key="sugg.otherNone"/></div>
        </c:if>

        <c:if test="${hasUnknown}">
          <hr class="sep"/>
          <div class="tablewrap">
            <table>
              <thead>
              <tr>
                <th style="width:40px;"></th>
                <th><fmt:message key="table.name"/></th>
                <th><fmt:message key="table.amount"/></th>
                <th><fmt:message key="table.interval"/></th>
                <th><fmt:message key="sugg.last"/></th>
                <th><fmt:message key="sugg.next"/></th>
                <th><fmt:message key="sugg.count"/></th>
                <th><fmt:message key="sugg.confidence"/></th>
                <th><fmt:message key="table.action"/></th>
              </tr>
              </thead>
              <tbody>
              <c:forEach var="s" items="${suggestions}">
                <c:if test="${not s.knownProvider}">
                  <tr>
                    <td><input type="checkbox" class="rowCheck" name="keys" value="${s.key}" /></td>
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
                        <button type="submit" class="btn suggestion-action"><fmt:message key="common.accept"/></button>
                      </form>
                      <form method="post" action="<c:url value='/app/suggestions/reject'/>" style="display:inline; margin-left:6px;">
                        <input type="hidden" name="key" value="${s.key}" />
                        <button type="submit" class="btn suggestion-action"><fmt:message key="common.reject"/></button>
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
    </form>
  </c:if>
</div>

<script>
  // Import polling + disable actions while running
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

<script>
  // Multi-select UX (count + indeterminate + enable/disable bulk buttons)
  (function () {
    const selectAll = document.getElementById("selectAllBox");
    const selectedCount = document.getElementById("selectedCount");
    const bulkForm = document.getElementById("bulkForm");
    const bulkRejectBtn = document.getElementById("bulkRejectBtn");
    const bulkAcceptBtn = document.getElementById("bulkAcceptBtn");

    const txtSelected = "<fmt:message key='sugg.selectedSuffix'/>";

    function checks(){ return Array.from(document.querySelectorAll(".rowCheck")); }

    function update() {
      const all = checks();
      const checked = all.filter(c => c.checked).length;

      if (selectedCount) selectedCount.textContent = checked + " " + txtSelected;

      if (bulkRejectBtn) bulkRejectBtn.disabled = checked === 0;
      if (bulkAcceptBtn) bulkAcceptBtn.disabled = checked === 0;

      if (selectAll) {
        if (checked === 0) {
          selectAll.indeterminate = false;
          selectAll.checked = false;
        } else if (checked === all.length) {
          selectAll.indeterminate = false;
          selectAll.checked = true;
        } else {
          selectAll.indeterminate = true;
          selectAll.checked = false;
        }
      }
    }

    if (selectAll) {
      selectAll.addEventListener("change", function () {
        checks().forEach(c => c.checked = selectAll.checked);
        update();
      });
    }

    document.addEventListener("change", function (e) {
      if (e.target && e.target.classList && e.target.classList.contains("rowCheck")) {
        update();
      }
    });

    if (bulkRejectBtn && bulkForm) {
      bulkRejectBtn.addEventListener("click", function () {
        bulkForm.action = "<c:url value='/app/suggestions/reject-bulk'/>";
        bulkForm.submit();
      });
    }

    update();
  })();
</script>

</body>
</html>
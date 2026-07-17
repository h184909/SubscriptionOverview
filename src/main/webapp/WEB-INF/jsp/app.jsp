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

  <style>
    .dashboard-grid {
      display:grid;
      grid-template-columns:minmax(0, 1fr) minmax(0, 1fr);
      gap:16px;
      align-items:start;
    }

    .dashboard-grid > .card {
      min-width:0;
    }

    .dashboard-full {
      grid-column:1 / -1;
      min-width:0;
    }

    .tablewrap {
      overflow-x:auto;
    }

    .dash-kpis {
      display:grid;
      grid-template-columns:repeat(2, minmax(0, 1fr));
      gap:12px;
      margin-top:10px;
    }

    .kpi-card {
      border:1px solid rgba(255,255,255,.14);
      background:rgba(255,255,255,.035);
      border-radius:14px;
      padding:14px;
    }

    .kpi-label {
      color:var(--muted);
      font-size:13px;
      margin-bottom:8px;
    }

    .kpi-value {
      font-size:26px;
      font-weight:900;
      letter-spacing:-.03em;
    }

    .bar-track {
      height:10px;
      background:rgba(255,255,255,.08);
      border-radius:999px;
      overflow:hidden;
      margin-top:7px;
    }

    .bar-fill {
      height:10px;
      background:linear-gradient(90deg, rgba(96,165,250,.95), rgba(52,211,153,.95));
      border-radius:999px;
    }

    .donut-wrap {
      display:flex;
      gap:18px;
      align-items:center;
      flex-wrap:wrap;
      margin-top:14px;
    }

    .donut {
      width:135px;
      height:135px;
      border-radius:50%;
      background:${categoryChartCss};
      position:relative;
      flex:0 0 auto;
      box-shadow:inset 0 0 0 1px rgba(255,255,255,.10);
    }

    .donut:after {
      content:"";
      position:absolute;
      inset:28px;
      background:var(--card);
      border-radius:50%;
      box-shadow:inset 0 0 0 1px rgba(255,255,255,.10);
    }

    .compact-list {
      display:flex;
      flex-direction:column;
      gap:10px;
      margin-top:12px;
    }

    .compact-row {
      display:flex;
      justify-content:space-between;
      gap:12px;
      align-items:center;
    }

    @media (max-width: 900px) {
      .dashboard-grid {
        grid-template-columns:1fr;
      }

      .dash-kpis {
        grid-template-columns:1fr;
      }
    }

    .dashboard-table {
      width: 100%;
      min-width: 0;
      table-layout: fixed;
    }

    .dashboard-table th,
    .dashboard-table td {
      white-space: normal;
      overflow-wrap: anywhere;
      vertical-align: middle;
    }

    .dashboard-table th {
      font-size: 12px;
    }

    .dashboard-table td {
      font-size: 14px;
    }

    .top-subscriptions-table th:nth-child(1),
    .top-subscriptions-table td:nth-child(1) {
      width: 42%;
    }

    .top-subscriptions-table th:nth-child(2),
    .top-subscriptions-table td:nth-child(2) {
      width: 32%;
    }

    .top-subscriptions-table th:nth-child(3),
    .top-subscriptions-table td:nth-child(3) {
      width: 26%;
      text-align: right;
      white-space: nowrap;
    }

    .due-month-table th:nth-child(1),
    .due-month-table td:nth-child(1) {
      width: 42%;
    }

    .due-month-table th:nth-child(2),
    .due-month-table td:nth-child(2) {
      width: 28%;
    }

    .due-month-table th:nth-child(3),
    .due-month-table td:nth-child(3) {
      width: 30%;
      text-align: right;
      white-space: nowrap;
    }

    .dashboard-compact-tablewrap {
      overflow-x: visible;
    }
  </style>
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

  <div class="dashboard-grid">

    <div class="card">
      <h3><fmt:message key="dash.status"/></h3>
      <div class="muted"><fmt:message key="dash.loggedInAs"/> <b><c:out value="${email}"/></b></div>

      <hr class="sep"/>

      <c:choose>
        <c:when test="${bankConnected}">
          <div class="pill ok">✅ <fmt:message key="dash.bankConnected"/></div>

          <div class="muted" style="margin-top:10px;">
            <b><c:out value="${empty bankInstitutionName ? 'Lunch Flow' : bankInstitutionName}"/></b>
          </div>

          <c:if test="${not empty bankAccountCount}">
            <div class="muted" style="margin-top:6px;">
              <c:out value="${bankAccountCount}"/> <fmt:message key="dash.accounts"/>
              <c:if test="${not empty bankAccountNames}">
                · <c:out value="${bankAccountNames}"/>
              </c:if>
            </div>
          </c:if>

          <c:if test="${not empty bankLastSynced}">
            <div class="muted" style="margin-top:6px;">
              <fmt:message key="dash.lastSynced"/> <b><c:out value="${bankLastSynced}"/></b>
            </div>
          </c:if>

          <div style="margin-top:12px; display:flex; gap:10px; flex-wrap:wrap; align-items:center;">
            <form method="post" action="<c:url value='/lunchflow/sync'/>" style="margin:0;">
              <button class="btn btn-primary" type="submit">
                <fmt:message key="dash.bankSync"/>
              </button>
            </form>

            <a class="btn" href="<c:url value='/lunchflow/connect'/>">
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
            <a class="btn btn-primary" href="<c:url value='/lunchflow/connect'/>">
              <fmt:message key="dash.bankConnect"/>
            </a>
          </div>

          <div class="muted" style="margin-top:10px;">
            <fmt:message key="dash.bankNotConnectedHint"/>
          </div>
        </c:otherwise>
      </c:choose>

      <hr class="sep"/>
      <form method="post" action="<c:url value='/logout'/>">
        <button class="btn btn-danger" type="submit"><fmt:message key="dash.logout"/></button>
      </form>
    </div>

    <div class="card">
      <h3><fmt:message key="dash.overview"/></h3>

      <div class="dash-kpis">
        <div class="kpi-card">
          <div class="kpi-label"><fmt:message key="dash.monthlyCost"/></div>
          <div class="kpi-value"><c:out value="${totalMonthlyNok}"/> NOK</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-label"><fmt:message key="dash.yearlyEstimate"/></div>
          <div class="kpi-value"><c:out value="${yearlyTotalNok}"/> NOK</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-label"><fmt:message key="dash.activeCount"/></div>
          <div class="kpi-value"><c:out value="${activeSubscriptionCount}"/></div>
        </div>

        <div class="kpi-card">
          <div class="kpi-label"><fmt:message key="dash.dueNext7"/></div>
          <div class="kpi-value"><c:out value="${dueSoonCount}"/></div>
        </div>
      </div>

      <c:if test="${not empty largestCategory}">
        <hr class="sep"/>
        <div class="compact-row">
          <div>
            <div class="muted"><fmt:message key="dash.largestCategory"/></div>
            <div style="margin-top:6px;">
              <b><c:out value="${largestCategory.category}"/></b>
              <span class="muted"> · <c:out value="${largestCategory.percent}"/>%</span>
            </div>
          </div>
          <span class="pill ok">
            <c:out value="${largestCategory.amount}"/> NOK/<fmt:message key="dash.monthShort"/>
          </span>
        </div>
      </c:if>

      <c:if test="${not empty largestSubscription}">
        <div style="margin-top:14px;">
          <div class="muted"><fmt:message key="dash.largestSubscription"/></div>
          <div style="margin-top:6px;">
            <b><c:out value="${largestSubscription.name}"/></b>
            <span class="muted">
              · <c:out value="${largestSubscriptionMonthly}"/> NOK/<fmt:message key="dash.monthShort"/>
            </span>
          </div>
        </div>
      </c:if>

      <c:if test="${not empty smartInsight}">
        <hr class="sep"/>
        <div class="notice">
          💡 <c:out value="${smartInsight}"/>
        </div>
      </c:if>
    </div>

    <div class="card">
      <h3><fmt:message key="dash.categoryDistribution"/></h3>

      <c:if test="${empty categoryInsights}">
        <div class="muted"><fmt:message key="dash.noCategoryData"/></div>
      </c:if>

      <c:if test="${not empty categoryInsights}">
        <div class="donut-wrap">
          <div class="donut" aria-label="Category distribution"></div>

          <div style="flex:1; min-width:220px;">
            <div class="compact-list">
              <c:forEach var="c" items="${categoryInsights}">
                <div>
                  <div class="compact-row">
                    <div>
                      <b><c:out value="${c.category}"/></b>
                      <span class="muted"> · <c:out value="${c.percent}"/>%</span>
                    </div>
                    <div><b><c:out value="${c.amount}"/></b> NOK</div>
                  </div>

                  <div class="bar-track">
                    <div class="bar-fill" style="width:${c.barWidth}%;"></div>
                  </div>
                </div>
              </c:forEach>
            </div>
          </div>
        </div>
      </c:if>
    </div>

    <div class="card">
      <h3><fmt:message key="dash.projection"/></h3>

      <c:if test="${empty projectionMonths}">
        <div class="muted"><fmt:message key="dash.noProjection"/></div>
      </c:if>

      <c:if test="${not empty projectionMonths}">
        <div class="compact-list">
          <c:forEach var="m" items="${projectionMonths}">
            <div>
              <div class="compact-row">
                <div><b><c:out value="${m.label}"/></b></div>
                <div><b><c:out value="${m.amount}"/></b> NOK</div>
              </div>
              <div class="bar-track">
                <div class="bar-fill" style="width:${m.barWidth}%;"></div>
              </div>
            </div>
          </c:forEach>
        </div>
      </c:if>
    </div>

    <div class="card">
      <h3><fmt:message key="dash.topSubscriptions"/></h3>

      <c:if test="${empty topSubscriptions}">
        <div class="muted"><fmt:message key="dash.noActiveSubs"/></div>
      </c:if>

      <c:if test="${not empty topSubscriptions}">
        <div class="dashboard-compact-tablewrap" style="margin-top:10px;">
          <table class="dashboard-table top-subscriptions-table">
            <thead>
            <tr>
              <th><fmt:message key="table.name"/></th>
              <th><fmt:message key="dash.category"/></th>
              <th><fmt:message key="dash.monthly"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${topSubscriptions}">
              <tr>
                <td>
                  <b><c:out value="${s.name}"/></b>
                </td>

                <td>
                  <c:choose>
                    <c:when test="${empty s.category || s.category == 'Other'}">
                      -
                    </c:when>
                    <c:otherwise>
                      <c:out value="${s.category}"/>
                    </c:otherwise>
                  </c:choose>
                </td>

                <td>
                  <b><c:out value="${monthlyNokBySubId[s.id]}"/></b> NOK
                </td>
              </tr>
            </c:forEach>
            </tbody>
          </table>
        </div>
      </c:if>
    </div>

    <div class="card">
      <h3><fmt:message key="dash.dueThisMonth"/></h3>

      <c:if test="${empty dueThisMonth}">
        <div class="muted"><fmt:message key="dash.noDueThisMonth"/></div>
      </c:if>

      <c:if test="${not empty dueThisMonth}">
        <div class="dashboard-compact-tablewrap" style="margin-top:10px;">
          <table class="dashboard-table due-month-table">
            <thead>
            <tr>
              <th><fmt:message key="table.name"/></th>
              <th><fmt:message key="dash.date"/></th>
              <th><fmt:message key="table.amount"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${dueThisMonth}">
              <tr>
                <td>
                  <b><c:out value="${s.name}"/></b>
                </td>

                <td>
                  <c:out value="${s.nextChargeDate}"/>
                </td>

                <td>
                  <b><c:out value="${s.amount}"/></b>
                  <c:out value="${s.currency}"/>
                </td>
              </tr>
            </c:forEach>
            </tbody>
          </table>
        </div>
      </c:if>
    </div>

    <div class="card dashboard-full">
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
              <th>Category</th>
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
                  <c:choose>
                    <c:when test="${empty s.category || s.category == 'Other'}">-</c:when>
                    <c:otherwise><c:out value="${s.category}"/></c:otherwise>
                  </c:choose>
                </td>

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

                <td><b><c:out value="${monthlyNokBySubId[s.id]}"/></b> NOK</td>
              </tr>
            </c:forEach>
            </tbody>
          </table>
        </div>

        <div style="margin-top:12px;">
          <span class="pill ok">
            <fmt:message key="dash.totalMonthly"/>
            <b><c:out value="${totalMonthlyNok}"/> NOK</b>
          </span>
        </div>
      </c:if>
    </div>
  </div>
</div>

</body>
</html>
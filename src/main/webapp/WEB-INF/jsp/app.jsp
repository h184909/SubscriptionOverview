<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html lang="${pageContext.request.locale.language}" data-context-path="${pageContext.request.contextPath}">
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="dash.title"/></title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css?v=mobile-v2" />
  <link rel="icon" href="<c:url value='/favicon.ico'/>" />
  <meta name="theme-color" content="#0b1220" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />

  <style>
    .dash-hero {
      display:flex;
      justify-content:space-between;
      gap:20px;
      align-items:flex-end;
      margin-top:16px;
      padding:22px;
    }

    .dash-hero h2 {
      margin:0;
      font-size:clamp(27px,4vw,42px);
      letter-spacing:-.035em;
    }

    .dash-hero p {
      margin:8px 0 0;
      color:var(--muted);
      line-height:1.55;
    }

    .dash-quick-actions {
      display:flex;
      flex-wrap:wrap;
      justify-content:flex-end;
      gap:9px;
    }

    .dash-kpis {
      display:grid;
      grid-template-columns:repeat(4,minmax(0,1fr));
      gap:14px;
      margin-top:16px;
    }

    .dash-kpi {
      min-width:0;
      padding:17px;
      border:1px solid var(--border);
      border-radius:16px;
      background:rgba(255,255,255,.025);
      box-shadow:var(--shadow);
    }

    .dash-kpi-icon {
      display:grid;
      place-items:center;
      width:38px;
      height:38px;
      margin-bottom:16px;
      border-radius:12px;
      background:rgba(52,211,153,.09);
      border:1px solid rgba(52,211,153,.20);
      font-size:18px;
    }

    .dash-kpi-label {
      color:var(--muted);
      font-size:12px;
    }

    .dash-kpi-value {
      margin-top:6px;
      font-size:25px;
      font-weight:900;
      letter-spacing:-.03em;
      overflow-wrap:anywhere;
    }

    .dash-kpi-note {
      margin-top:7px;
      color:var(--muted);
      font-size:12px;
    }

    .dash-grid {
      display:grid;
      grid-template-columns:minmax(0,1.05fr) minmax(0,.95fr);
      gap:16px;
      margin-top:16px;
      align-items:start;
    }

    .dash-stack {
      display:grid;
      gap:16px;
      min-width:0;
    }

    .dash-grid .card {
      margin:0;
      min-width:0;
    }

    .dash-section-title {
      display:flex;
      align-items:center;
      justify-content:space-between;
      gap:12px;
      margin-bottom:12px;
    }

    .dash-section-title h3 {
      margin:0;
    }

    .next-payment {
      display:grid;
      grid-template-columns:1fr auto;
      gap:18px;
      align-items:center;
      padding:20px;
      border:1px solid rgba(96,165,250,.22);
      border-radius:16px;
      background:
        radial-gradient(circle at top right,rgba(96,165,250,.11),transparent 45%),
        rgba(255,255,255,.02);
    }

    .next-payment-name {
      margin-top:5px;
      font-size:23px;
      font-weight:900;
    }

    .next-payment-date {
      color:var(--muted);
      margin-top:7px;
    }

    .next-payment-amount {
      text-align:right;
      font-size:25px;
      font-weight:900;
      white-space:nowrap;
    }

    .activity-list,
    .alert-list,
    .payment-list {
      display:grid;
      gap:10px;
    }

    .activity-item,
    .alert-item,
    .payment-item {
      display:grid;
      grid-template-columns:auto 1fr auto;
      gap:12px;
      align-items:center;
      padding:12px;
      border:1px solid rgba(255,255,255,.075);
      border-radius:13px;
      background:rgba(255,255,255,.018);
    }

    .activity-icon,
    .alert-icon {
      display:grid;
      place-items:center;
      width:34px;
      height:34px;
      border-radius:11px;
      background:rgba(96,165,250,.10);
      border:1px solid rgba(96,165,250,.17);
    }

    .activity-title,
    .alert-title,
    .payment-name {
      font-weight:800;
    }

    .activity-text,
    .alert-text,
    .payment-date {
      margin-top:3px;
      color:var(--muted);
      font-size:12px;
    }

    .alert-item.warning {
      border-color:rgba(251,191,36,.25);
    }

    .alert-item.good {
      border-color:rgba(52,211,153,.25);
    }

    .payment-amount {
      font-weight:900;
      white-space:nowrap;
    }

    .bank-card {
      display:grid;
      gap:12px;
    }

    .bank-meta {
      display:grid;
      gap:5px;
      color:var(--muted);
      font-size:13px;
    }

    .bank-actions {
      display:flex;
      flex-wrap:wrap;
      gap:9px;
    }

    .top-table {
      width:100%;
      min-width:0;
      table-layout:fixed;
    }

    .top-table th,
    .top-table td {
      white-space:normal;
      overflow-wrap:anywhere;
      vertical-align:middle;
    }

    .top-table th:last-child,
    .top-table td:last-child {
      text-align:right;
      white-space:nowrap;
    }

    @media (max-width:980px) {
      .dash-kpis {
        grid-template-columns:repeat(2,minmax(0,1fr));
      }

      .dash-grid {
        grid-template-columns:1fr;
      }
    }

    @media (max-width:680px) {
      .dash-hero {
        align-items:flex-start;
        flex-direction:column;
      }

      .dash-quick-actions {
        justify-content:flex-start;
      }

      .dash-kpis {
        grid-template-columns:1fr;
      }

      .next-payment {
        grid-template-columns:1fr;
      }

      .next-payment-amount {
        text-align:left;
      }

      .activity-item,
      .alert-item,
      .payment-item {
        grid-template-columns:auto 1fr;
      }

      .payment-amount {
        grid-column:2;
      }
    }
  </style>
  <link rel="stylesheet" href="<c:url value='/assets/subscription-details.css?v=2'/>" />
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

    <button type="button"
            class="mobile-menu-toggle"
            aria-label="Open navigation"
            aria-expanded="false"
            aria-controls="main-navigation">
      <span class="mobile-menu-toggle-lines" aria-hidden="true"></span>
    </button>

    <div class="nav" id="main-navigation">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
      <a href="<c:url value='/app/subscriptions'/>"><fmt:message key="nav.subscriptions"/></a>
      <a href="<c:url value='/app/analytics'/>"><fmt:message key="nav.analytics"/></a>
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
    <div class="card" style="margin-top:16px;">
      <div class="notice flash"><b><c:out value="${flashMsg}"/></b></div>
    </div>
  </c:if>

  <section class="card dash-hero">
    <div>
      <h2>
        <fmt:message key="${greetingKey}"/>,
        <c:out value="${displayName}"/> 👋
      </h2>
      <p><fmt:message key="dash.greeting.lead"/></p>
    </div>

    <div class="dash-quick-actions">
      <a class="btn btn-primary" href="<c:url value='/app/suggestions'/>">
        ✦ <fmt:message key="dash.quick.review"/>
      </a>
      <a class="btn" href="<c:url value='/app/subscriptions/new'/>">
        ＋ <fmt:message key="dash.quick.add"/>
      </a>
      <a class="btn" href="<c:url value='/app/analytics'/>">
        📊 <fmt:message key="dash.quick.analytics"/>
      </a>
    </div>
  </section>

  <section class="dash-kpis">
    <div class="dash-kpi">
      <div class="dash-kpi-icon">💳</div>
      <div class="dash-kpi-label"><fmt:message key="dash.monthlyCost"/></div>
      <div class="dash-kpi-value"><c:out value="${totalMonthlyNok}"/> NOK</div>
      <div class="dash-kpi-note">
        <c:out value="${activeSubscriptionCount}"/>
        <fmt:message key="dash.kpi.activeSuffix"/>
      </div>
    </div>

    <div class="dash-kpi">
      <div class="dash-kpi-icon">📅</div>
      <div class="dash-kpi-label"><fmt:message key="dash.kpi.nextPayment"/></div>

      <c:choose>
        <c:when test="${not empty nextPayment}">
          <div class="dash-kpi-value"><c:out value="${nextPaymentNok}"/> NOK</div>
          <div class="dash-kpi-note">
            <c:out value="${nextPayment.name}"/> ·
            <c:out value="${nextPayment.nextChargeDate}"/>
          </div>
        </c:when>
        <c:otherwise>
          <div class="dash-kpi-value">-</div>
          <div class="dash-kpi-note"><fmt:message key="dash.kpi.noPayment"/></div>
        </c:otherwise>
      </c:choose>
    </div>

    <div class="dash-kpi">
      <div class="dash-kpi-icon">✂️</div>
      <div class="dash-kpi-label"><fmt:message key="dash.kpi.savedYearly"/></div>
      <div class="dash-kpi-value"><c:out value="${savedYearlyNok}"/> NOK</div>
      <div class="dash-kpi-note">
        <c:out value="${endedSubscriptionCount}"/>
        <fmt:message key="dash.kpi.endedSuffix"/>
      </div>
    </div>

    <div class="dash-kpi">
      <div class="dash-kpi-icon">⏳</div>
      <div class="dash-kpi-label"><fmt:message key="dash.kpi.dueSoon"/></div>
      <div class="dash-kpi-value"><c:out value="${dueSoonCount}"/></div>
      <div class="dash-kpi-note"><fmt:message key="dash.kpi.nextSevenDays"/></div>
    </div>
  </section>

  <section class="dash-grid">
    <div class="dash-stack">

      <div class="card dash-card-next">
        <div class="dash-section-title">
          <h3><fmt:message key="dash.nextPayment.title"/></h3>
          <a href="<c:url value='/app/subscriptions'/>">
            <fmt:message key="dash.viewAll"/>
          </a>
        </div>

        <c:choose>
          <c:when test="${not empty nextPayment}">
            <div class="next-payment subscription-details-trigger" data-subscription-id="${nextPayment.id}" tabindex="0">
              <div>
                <div class="muted"><fmt:message key="dash.nextPayment.upNext"/></div>
                <div class="next-payment-name"><c:out value="${nextPayment.name}"/></div>
                <div class="next-payment-date">
                  <c:choose>
                    <c:when test="${nextPaymentDays == 0}">
                      <fmt:message key="dash.date.today"/>
                    </c:when>
                    <c:when test="${nextPaymentDays == 1}">
                      <fmt:message key="dash.date.tomorrow"/>
                    </c:when>
                    <c:otherwise>
                      <c:out value="${nextPayment.nextChargeDate}"/>
                    </c:otherwise>
                  </c:choose>
                </div>
              </div>

              <div class="next-payment-amount">
                <c:out value="${nextPaymentNok}"/> NOK
              </div>
            </div>
          </c:when>

          <c:otherwise>
            <div class="muted"><fmt:message key="dash.nextPayment.none"/></div>
          </c:otherwise>
        </c:choose>
      </div>

      <div class="card dash-card-subscriptions">
        <div class="dash-section-title">
          <h3><fmt:message key="nav.subscriptions"/></h3>
          <a href="<c:url value='/app/subscriptions'/>">
            <fmt:message key="dash.viewAll"/>
          </a>
        </div>

        <c:if test="${empty subs}">
          <div class="muted"><fmt:message key="dash.noActiveSubs"/></div>
        </c:if>

        <c:if test="${not empty subs}">
          <div class="tablewrap">
            <table class="top-table">
              <thead>
              <tr>
                <th><fmt:message key="table.name"/></th>
                <th><fmt:message key="dash.category"/></th>
                <th><fmt:message key="table.monthlyApprox"/></th>
              </tr>
              </thead>
              <tbody>
              <c:forEach var="s" items="${subs}">
                <tr class="subscription-details-trigger" data-subscription-id="${s.id}" tabindex="0">
                  <td><b><c:out value="${s.name}"/></b></td>
                  <td>
                    <c:choose>
                      <c:when test="${empty s.category || s.category == 'Other'}">-</c:when>
                      <c:otherwise><c:out value="${s.category}"/></c:otherwise>
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


      <div class="card dash-card-upcoming">
        <div class="dash-section-title">
          <h3><fmt:message key="dash.upcoming.title"/></h3>
          <span class="muted"><fmt:message key="dash.upcoming.lead"/></span>
        </div>

        <c:if test="${empty upcomingPayments}">
          <div class="muted"><fmt:message key="dash.upcoming.none"/></div>
        </c:if>

        <div class="payment-list">
          <c:forEach var="s" items="${upcomingPayments}">
            <div class="payment-item">
              <div class="activity-icon">◷</div>
              <div>
                <div class="payment-name"><c:out value="${s.name}"/></div>
                <div class="payment-date"><c:out value="${s.nextChargeDate}"/></div>
              </div>
              <div class="payment-amount">
                <c:out value="${s.amount}"/> <c:out value="${s.currency}"/>
              </div>
            </div>
          </c:forEach>
        </div>
      </div>

    </div>

    <div class="dash-stack">

      <div class="card dash-card-bank">
        <div class="dash-section-title">
          <h3><fmt:message key="dash.bank.title"/></h3>
          <c:choose>
            <c:when test="${bankConnected}">
              <span class="pill ok">✓ <fmt:message key="dash.bankConnected"/></span>
            </c:when>
            <c:otherwise>
              <span class="pill warn">! <fmt:message key="dash.bankNotConnected"/></span>
            </c:otherwise>
          </c:choose>
        </div>

        <div class="bank-card">
          <c:choose>
            <c:when test="${bankConnected}">
              <div class="bank-meta">
                <b><c:out value="${empty bankInstitutionName ? 'Lunch Flow' : bankInstitutionName}"/></b>
                <c:if test="${not empty bankAccountCount}">
                  <span>
                    <c:out value="${bankAccountCount}"/> <fmt:message key="dash.accounts"/>
                    <c:if test="${not empty bankAccountNames}">
                      · <c:out value="${bankAccountNames}"/>
                    </c:if>
                  </span>
                </c:if>
                <c:if test="${not empty bankLastSynced}">
                  <span>
                    <fmt:message key="dash.lastSynced"/>
                    <b><c:out value="${bankLastSynced}"/></b>
                  </span>
                </c:if>
              </div>

              <div class="bank-actions">
                <form method="post" action="<c:url value='/lunchflow/sync'/>" style="margin:0;">
                  <button class="btn btn-primary" type="submit">
                    <fmt:message key="dash.bankSync"/>
                  </button>
                </form>

                <a class="btn" href="<c:url value='/app/profile'/>">
                  <fmt:message key="dash.bankManage"/>
                </a>
              </div>
            </c:when>

            <c:otherwise>
              <div class="muted"><fmt:message key="dash.bankNotConnectedHint"/></div>
              <div>
                <a class="btn btn-primary" href="<c:url value='/lunchflow/connect'/>">
                  <fmt:message key="dash.bankConnect"/>
                </a>
              </div>
            </c:otherwise>
          </c:choose>
        </div>
      </div>

      <div class="card dash-card-alerts">
        <div class="dash-section-title">
          <h3><fmt:message key="dash.alerts.title"/></h3>
          <span class="pill"><c:out value="${alerts.size()}"/></span>
        </div>

        <div class="alert-list">
          <c:forEach var="alert" items="${alerts}">
            <div class="alert-item ${alert.type}">
              <div class="alert-icon"><c:out value="${alert.icon}"/></div>
              <div>
                <div class="alert-title"><c:out value="${alert.title}"/></div>
                <div class="alert-text"><c:out value="${alert.text}"/></div>
              </div>
            </div>
          </c:forEach>
        </div>
      </div>

      <div class="card dash-card-activity">
        <div class="dash-section-title">
          <h3><fmt:message key="dash.activity.title"/></h3>
          <span class="muted"><fmt:message key="dash.activity.lead"/></span>
        </div>

        <div class="activity-list">
          <c:forEach var="activity" items="${recentActivity}">
            <div class="activity-item">
              <div class="activity-icon"><c:out value="${activity.icon}"/></div>
              <div>
                <div class="activity-title"><c:out value="${activity.title}"/></div>
                <div class="activity-text"><c:out value="${activity.text}"/></div>
              </div>
            </div>
          </c:forEach>
        </div>
      </div>



      <div class="card dash-card-account">
        <div class="dash-section-title">
          <h3><fmt:message key="dash.account.title"/></h3>
        </div>

        <div class="muted">
          <fmt:message key="dash.loggedInAs"/>
          <b><c:out value="${email}"/></b>
        </div>

        <form method="post" action="<c:url value='/logout'/>" style="margin-top:14px;">
          <button class="btn btn-danger" type="submit">
            <fmt:message key="dash.logout"/>
          </button>
        </form>
      </div>
    </div>
  </section>
</div>
<script src="<c:url value='/assets/mobile-nav.js?v=mobile-v2'/>"></script>
  <script src="<c:url value='/assets/subscription-details.js?v=3'/>"></script>
</body>
</html>



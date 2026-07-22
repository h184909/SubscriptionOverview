<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html lang="${pageContext.request.locale.language}">
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="analytics.title"/></title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />
  <link rel="icon" href="<c:url value='/favicon.ico'/>" />
  <meta name="theme-color" content="#0b1220" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <style>
    .analytics-header{display:flex;justify-content:space-between;gap:16px;align-items:flex-start;flex-wrap:wrap;margin-top:16px}
    .analytics-kpis{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px;margin-top:16px}
    .analytics-kpi{min-width:0;border:1px solid var(--border);border-radius:16px;padding:16px;background:rgba(255,255,255,.025)}
    .analytics-kpi-label{color:var(--muted);font-size:13px;margin-bottom:8px}
    .analytics-kpi-value{font-size:26px;font-weight:900;letter-spacing:-.03em;overflow-wrap:anywhere}
    .analytics-kpi-note{color:var(--muted);font-size:12px;margin-top:8px}
    .analytics-grid{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:16px;margin-top:16px;align-items:start}
    .analytics-grid>.card{min-width:0;margin:0}.analytics-full{grid-column:1/-1}
    .analytics-list{display:flex;flex-direction:column;gap:12px;margin-top:12px}
    .analytics-row{display:flex;justify-content:space-between;align-items:center;gap:12px}
    .analytics-bar{height:10px;border-radius:999px;background:rgba(255,255,255,.08);overflow:hidden;margin-top:7px}
    .analytics-bar-fill{height:100%;border-radius:999px;background:linear-gradient(90deg,rgba(96,165,250,.95),rgba(52,211,153,.95))}
    .analytics-donut-layout{display:flex;align-items:center;gap:22px;margin-top:14px}
    .analytics-donut{position:relative;width:150px;height:150px;flex:0 0 150px;border-radius:50%;background:${categoryChartCss};box-shadow:inset 0 0 0 1px rgba(255,255,255,.10)}
    .analytics-donut::after{content:"";position:absolute;inset:32px;border-radius:50%;background:var(--card);box-shadow:inset 0 0 0 1px rgba(255,255,255,.10)}
    .score-layout{display:flex;align-items:center;gap:20px;flex-wrap:wrap;margin-top:14px}
    .score-circle{width:130px;height:130px;flex:0 0 130px;border-radius:50%;display:grid;place-items:center;background:radial-gradient(circle at center,var(--card) 57%,transparent 58%),conic-gradient(var(--good) 0 ${dataQualityScore}%,rgba(255,255,255,.08) ${dataQualityScore}% 100%);border:1px solid var(--border)}
    .score-number{font-size:30px;font-weight:900}
    .analytics-table{width:100%;min-width:0;table-layout:fixed}.analytics-table th,.analytics-table td{white-space:normal;overflow-wrap:anywhere;vertical-align:middle}.money{text-align:right;white-space:nowrap}
    .analytics-insights{display:grid;gap:10px;margin-top:12px}.analytics-insight{border:1px dashed rgba(255,255,255,.18);border-radius:14px;background:rgba(255,255,255,.03);padding:12px 14px}
    .award-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px;margin-top:12px}.award{border:1px solid var(--border);border-radius:16px;padding:14px;background:rgba(255,255,255,.025)}.award-icon{font-size:26px}.award-title{font-weight:800;margin-top:8px}.award-text{color:var(--muted);font-size:13px;margin-top:6px}
    .mini-stats{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-top:12px}.mini-stat{border:1px solid var(--border);border-radius:14px;padding:14px;background:rgba(255,255,255,.025)}.mini-stat-value{font-size:21px;font-weight:900;margin-top:6px}
    @media(max-width:1000px){.analytics-kpis,.award-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
    @media(max-width:850px){.analytics-grid{grid-template-columns:1fr}.analytics-full{grid-column:auto}.analytics-donut-layout{align-items:flex-start;flex-direction:column}.mini-stats{grid-template-columns:1fr}}
    @media(max-width:560px){.analytics-kpis,.award-grid{grid-template-columns:1fr}.analytics-kpi-value{font-size:23px}}
  </style>
</head>
<body>
<div class="container">
  <div class="topbar">
    <div class="brand">
      <img class="logo" src="<c:url value='/assets/logo.png'/>" alt="SubscriptionOverview" />
      <div><h1>SubscriptionOverview</h1><div class="sub"><fmt:message key="analytics.subtitle"/></div></div>
    </div>
    <div class="nav">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
      <a href="<c:url value='/app/subscriptions'/>"><fmt:message key="nav.subscriptions"/></a>
      <a href="<c:url value='/app/analytics'/>"><fmt:message key="nav.analytics"/></a>
      <a href="<c:url value='/app/suggestions'/>"><fmt:message key="nav.suggestions"/></a>
      <a href="<c:url value='/app/transactions/import-csv'/>"><fmt:message key="nav.importCsv"/></a>
      <a href="<c:url value='/app/profile'/>"><fmt:message key="nav.profile"/></a>
      <span class="muted" style="margin:0 6px;">|</span>
      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English">🇬🇧</a><a href="${toNb}" title="Norsk">🇳🇴</a>
    </div>
  </div>

  <div class="card analytics-header">
    <div><h2 style="margin:0;"><fmt:message key="analytics.header"/></h2><div class="muted" style="margin-top:7px;"><fmt:message key="analytics.lead"/></div></div>
    <a class="btn" href="<c:url value='/app/subscriptions'/>"><fmt:message key="analytics.manageSubscriptions"/></a>
  </div>

  <div class="analytics-kpis">
    <div class="analytics-kpi"><div class="analytics-kpi-label"><fmt:message key="analytics.monthlyCost"/></div><div class="analytics-kpi-value"><c:out value="${activeMonthlyNok}"/> NOK</div><div class="analytics-kpi-note"><c:out value="${activeCount}"/> <fmt:message key="analytics.activeSuffix"/></div></div>
    <div class="analytics-kpi"><div class="analytics-kpi-label"><fmt:message key="analytics.yearlyEstimate"/></div><div class="analytics-kpi-value"><c:out value="${yearlyNok}"/> NOK</div><div class="analytics-kpi-note"><fmt:message key="analytics.basedOnActive"/></div></div>
    <div class="analytics-kpi"><div class="analytics-kpi-label"><fmt:message key="analytics.upcoming30Days"/></div><div class="analytics-kpi-value"><c:out value="${upcoming30DaysNok}"/> NOK</div><div class="analytics-kpi-note"><c:out value="${upcomingCount}"/> <fmt:message key="analytics.upcomingSuffix"/></div></div>
    <div class="analytics-kpi"><div class="analytics-kpi-label"><fmt:message key="analytics.savedMonthly"/></div><div class="analytics-kpi-value"><c:out value="${savedMonthlyNok}"/> NOK</div><div class="analytics-kpi-note"><c:out value="${endedCount}"/> <fmt:message key="analytics.endedSuffix"/></div></div>
  </div>

  <div class="analytics-grid">
    <div class="card analytics-full">
      <h3><fmt:message key="analytics.quickStats"/></h3>
      <div class="mini-stats">
        <div class="mini-stat"><div class="muted"><fmt:message key="analytics.averageCost"/></div><div class="mini-stat-value"><c:out value="${averageMonthlyNok}"/> NOK</div></div>
        <div class="mini-stat"><div class="muted"><fmt:message key="analytics.medianCost"/></div><div class="mini-stat-value"><c:out value="${medianMonthlyNok}"/> NOK</div></div>
        <div class="mini-stat"><div class="muted"><fmt:message key="analytics.forecastAverage"/></div><div class="mini-stat-value"><c:out value="${forecastAverage}"/> NOK</div></div>
      </div>
    </div>

    <div class="card">
      <h3><fmt:message key="analytics.categoryDistribution"/></h3>
      <c:if test="${empty categoryAnalytics}"><div class="muted"><fmt:message key="analytics.noCategoryData"/></div></c:if>
      <c:if test="${not empty categoryAnalytics}">
        <div class="analytics-donut-layout"><div class="analytics-donut"></div><div style="flex:1;min-width:0;"><div class="analytics-list">
          <c:forEach var="category" items="${categoryAnalytics}"><div><div class="analytics-row"><div><b><c:out value="${category.category}"/></b><span class="muted"> · <c:out value="${category.percentage}"/>%</span></div><div style="white-space:nowrap;"><b><c:out value="${category.amount}"/></b> NOK</div></div><div class="analytics-bar"><div class="analytics-bar-fill" style="width:${category.barWidth}%;"></div></div></div></c:forEach>
        </div></div></div>
      </c:if>
    </div>

    <div class="card">
      <h3><fmt:message key="analytics.dataQuality"/></h3>
      <div class="score-layout"><div class="score-circle"><div class="score-number"><c:out value="${dataQualityScore}"/></div></div><div style="flex:1;min-width:220px;"><b><fmt:message key="analytics.dataQualityExplanation"/></b><div class="muted" style="margin-top:9px;"><fmt:message key="analytics.uncategorized"/>: <b><c:out value="${uncategorizedCount}"/></b></div><div class="muted" style="margin-top:6px;"><fmt:message key="analytics.missingDates"/>: <b><c:out value="${missingNextChargeCount}"/></b></div><div class="muted" style="margin-top:6px;"><fmt:message key="analytics.largestShare"/>: <b><c:out value="${largestSubscriptionShare}"/>%</b></div><div style="margin-top:14px;"><a class="btn" href="<c:url value='/app/subscriptions'/>"><fmt:message key="analytics.improveData"/></a></div></div></div>
    </div>

    <div class="card">
      <h3><fmt:message key="analytics.intervalDistribution"/></h3>
      <div class="analytics-list"><c:forEach var="interval" items="${intervalAnalytics}"><div><div class="analytics-row"><div><b><c:choose><c:when test="${interval.interval=='WEEKLY'}"><fmt:message key="interval.weekly"/></c:when><c:when test="${interval.interval=='MONTHLY'}"><fmt:message key="interval.monthly"/></c:when><c:when test="${interval.interval=='QUARTERLY'}"><fmt:message key="interval.quarterly"/></c:when><c:when test="${interval.interval=='YEARLY'}"><fmt:message key="interval.yearly"/></c:when><c:otherwise><fmt:message key="analytics.otherInterval"/></c:otherwise></c:choose></b><span class="muted"> · <c:out value="${interval.count}"/></span></div><div><b><c:out value="${interval.percentage}"/>%</b></div></div><div class="analytics-bar"><div class="analytics-bar-fill" style="width:${interval.barWidth}%;"></div></div></div></c:forEach></div>
    </div>

    <div class="card">
      <h3><fmt:message key="analytics.currencyDistribution"/></h3>
      <div class="analytics-list"><c:forEach var="currency" items="${currencyAnalytics}"><div><div class="analytics-row"><div><b><c:out value="${currency.currency}"/></b><span class="muted"> · <c:out value="${currency.count}"/></span></div><div><b><c:out value="${currency.percentage}"/>%</b></div></div><div class="analytics-bar"><div class="analytics-bar-fill" style="width:${currency.barWidth}%;"></div></div></div></c:forEach></div>
    </div>

    <div class="card analytics-full">
      <h3><fmt:message key="analytics.awards"/></h3>
      <c:if test="${empty awards}"><div class="muted"><fmt:message key="analytics.noAwards"/></div></c:if>
      <c:if test="${not empty awards}"><div class="award-grid"><c:forEach var="award" items="${awards}"><div class="award"><div class="award-icon"><c:out value="${award.icon}"/></div><div class="award-title"><c:out value="${award.title}"/></div><div class="award-text"><c:out value="${award.text}"/></div></div></c:forEach></div></c:if>
    </div>

    <div class="card analytics-full">
      <h3><fmt:message key="analytics.forecast"/></h3>
      <div class="analytics-list"><c:forEach var="month" items="${forecastMonths}"><div><div class="analytics-row"><div><b><c:out value="${month.label}"/></b></div><div style="white-space:nowrap;"><b><c:out value="${month.amount}"/></b> NOK</div></div><div class="analytics-bar"><div class="analytics-bar-fill" style="width:${month.barWidth}%;"></div></div></div></c:forEach></div>
    </div>

    <div class="card analytics-full">
      <h3><fmt:message key="analytics.upcomingPayments"/></h3>
      <c:if test="${empty upcomingSubscriptions}"><div class="muted"><fmt:message key="analytics.noUpcomingPayments"/></div></c:if>
      <c:if test="${not empty upcomingSubscriptions}"><div class="tablewrap"><table><thead><tr><th><fmt:message key="table.name"/></th><th><fmt:message key="dash.date"/></th><th><fmt:message key="table.amount"/></th><th><fmt:message key="dash.category"/></th></tr></thead><tbody><c:forEach var="s" items="${upcomingSubscriptions}"><tr><td><b><c:out value="${s.name}"/></b></td><td><c:out value="${s.nextChargeDate}"/></td><td><c:out value="${s.amount}"/> <c:out value="${s.currency}"/></td><td><c:choose><c:when test="${empty s.category||s.category=='Other'}">-</c:when><c:otherwise><c:out value="${s.category}"/></c:otherwise></c:choose></td></tr></c:forEach></tbody></table></div></c:if>
    </div>

    <div class="card analytics-full">
      <h3><fmt:message key="analytics.topSubscriptions"/></h3>
      <c:if test="${not empty topSubscriptions}"><div class="tablewrap"><table><thead><tr><th><fmt:message key="table.name"/></th><th><fmt:message key="dash.category"/></th><th><fmt:message key="table.interval"/></th><th><fmt:message key="table.price"/></th><th><fmt:message key="table.monthlyApprox"/></th></tr></thead><tbody><c:forEach var="s" items="${topSubscriptions}"><tr><td><b><c:out value="${s.name}"/></b></td><td><c:choose><c:when test="${empty s.category||s.category=='Other'}">-</c:when><c:otherwise><c:out value="${s.category}"/></c:otherwise></c:choose></td><td><c:out value="${s.interval}"/></td><td><c:out value="${s.amount}"/> <c:out value="${s.currency}"/></td><td><b><c:out value="${monthlyNokBySubscriptionId[s.id]}"/></b> NOK</td></tr></c:forEach></tbody></table></div></c:if>
    </div>

    <div class="card">
      <h3><fmt:message key="analytics.savings"/></h3>
      <div class="mini-stats" style="grid-template-columns:repeat(2,minmax(0,1fr));"><div class="mini-stat"><div class="muted"><fmt:message key="analytics.savedMonthly"/></div><div class="mini-stat-value"><c:out value="${savedMonthlyNok}"/> NOK</div></div><div class="mini-stat"><div class="muted"><fmt:message key="analytics.savedYearly"/></div><div class="mini-stat-value"><c:out value="${savedYearlyNok}"/> NOK</div></div></div>
      <c:if test="${empty endedSubscriptions}"><div class="muted" style="margin-top:14px;"><fmt:message key="analytics.noEndedSubscriptions"/></div></c:if>
      <c:if test="${not empty endedSubscriptions}"><div class="tablewrap" style="margin-top:14px;"><table class="analytics-table"><thead><tr><th><fmt:message key="table.name"/></th><th class="money"><fmt:message key="analytics.savedPerMonth"/></th></tr></thead><tbody><c:forEach var="s" items="${endedSubscriptions}"><tr><td><b><c:out value="${s.name}"/></b></td><td class="money"><b><c:out value="${monthlyNokBySubscriptionId[s.id]}"/></b> NOK</td></tr></c:forEach></tbody></table></div></c:if>
    </div>

    <div class="card">
      <h3><fmt:message key="analytics.insights"/></h3>
      <div class="analytics-insights"><c:forEach var="insight" items="${insights}"><div class="analytics-insight">💡 <c:out value="${insight}"/></div></c:forEach></div>
    </div>
  </div>
</div>
<script src="<c:url value='/assets/mobile-nav.js'/>"></script>
</body>
</html>

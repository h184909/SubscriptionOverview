<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="landing.title"/></title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
</head>
<body>

<div class="container">
  <header class="topbar">
    <div class="brand">
      <div class="logo" aria-hidden="true"></div>
      <div>
        <h1>SubscriptionOverview</h1>
        <div class="sub"><fmt:message key="landing.subtitle"/></div>
      </div>
    </div>

    <nav class="nav" aria-label="Top navigation">
      <!-- språk -->
      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English" aria-label="English">🇬🇧</a>
      <a href="${toNb}" title="Norsk" aria-label="Norsk">🇳🇴</a>

      <span class="muted" style="margin:0 6px;">|</span>

      <a class="btn" href="<%=request.getContextPath()%>/login"><fmt:message key="landing.login"/></a>
      <a class="btn btn-primary" href="<%=request.getContextPath()%>/auth/register"><fmt:message key="landing.register"/></a>
    </nav>
  </header>

  <main>
    <div class="grid two">
      <section class="card">
        <h2 style="margin:0 0 8px 0;"><fmt:message key="landing.heroTitle"/></h2>

        <div class="muted">
          <fmt:message key="landing.heroText"/>
        </div>

        <!-- liten feature-liste -->
        <div style="margin-top:12px;">
          <div class="muted" style="display:flex; gap:10px; flex-wrap:wrap; align-items:center;">
            <span class="pill ok">✅ <fmt:message key="landing.badgeCsv"/></span>
            <span class="pill ok">✅ <fmt:message key="landing.featureDetect"/></span>
            <span class="pill ok">✅ <fmt:message key="landing.featureMonthly"/></span>
          </div>
          <div style="margin-top:8px;" class="muted">
            <span class="pill warn">⚠️ <fmt:message key="landing.badgeBank"/></span>
          </div>
        </div>

        <hr class="sep"/>

        <div style="display:flex; gap:10px; flex-wrap:wrap; align-items:center;">
          <a class="btn btn-primary" href="<%=request.getContextPath()%>/login"><fmt:message key="landing.ctaStart"/></a>
          <a class="btn" href="<%=request.getContextPath()%>/auth/register"><fmt:message key="landing.ctaCreate"/></a>
          <span class="muted" style="margin-left:auto;">
            <fmt:message key="landing.ctaHint"/>
          </span>
        </div>
      </section>

      <section class="card">
        <h3><fmt:message key="landing.howTitle"/></h3>
        <ul style="margin:8px 0 0 18px; color:var(--muted);">
          <li><fmt:message key="landing.how1"/></li>
          <li><fmt:message key="landing.how2"/></li>
          <li><fmt:message key="landing.how3"/></li>
          <li><fmt:message key="landing.how4"/></li>
        </ul>

        <hr class="sep"/>

        <div class="notice">
          <b><fmt:message key="landing.tipTitle"/></b>
          <fmt:message key="landing.tipText"/>
        </div>
      </section>
    </div>
  </main>
</div>

</body>
</html>
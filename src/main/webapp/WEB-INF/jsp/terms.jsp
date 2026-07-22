<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="${pageContext.request.locale.language}">
<head>
  <fmt:setBundle basename="messages" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="theme-color" content="#081220" />
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css?v=mobile-v2" />
  <link rel="icon" href="<c:url value='/favicon.ico'/>" />
  <style>
    .public-shell { width:min(920px,92vw); margin:28px auto 70px; }
    .public-main { margin-top:16px; }
    .public-main .card { margin-top:14px; }
    .public-main h1 { margin:0 0 10px; font-size:clamp(30px,5vw,46px); }
    .public-main h2 { margin-top:28px; }
    .public-main p, .public-main li { color:var(--muted); line-height:1.72; }
    .public-main a { overflow-wrap:anywhere; }
    .public-actions { display:flex; gap:10px; flex-wrap:wrap; margin-top:22px; }
    .faq-item { padding:18px 0; border-bottom:1px solid var(--border); }
    .faq-item:last-child { border-bottom:0; }
    .faq-item h3 { margin:0 0 8px; }
  </style>
  <title><fmt:message key="terms.title"/></title>
</head>
<body>
<div class="public-shell">
  <header class="topbar">
    <a class="brand" href="<c:url value='/'/>">
      <img class="logo" src="<c:url value='/assets/logo.png'/>" alt="SubscriptionOverview" />
      <div>
        <h1>SubscriptionOverview</h1>
        <div class="sub"><fmt:message key="landing.subtitle"/></div>
      </div>
    </a>

    <div class="nav">
      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English" aria-label="English">🇬🇧</a>
      <a href="${toNb}" title="Norsk" aria-label="Norsk">🇳🇴</a>
      <a href="<c:url value='/login'/>"><fmt:message key="landing.login"/></a>
      <a class="btn btn-primary" href="<c:url value='/auth/register'/>"><fmt:message key="landing.register"/></a>
    </div>
  </header>

  <main class="public-main">
    <section class="card">
      <h1><fmt:message key="terms.header"/></h1>
      <p><fmt:message key="terms.updated"/></p>

      <h2><fmt:message key="terms.service.title"/></h2>
      <p><fmt:message key="terms.service.text"/></p>

      <h2><fmt:message key="terms.account.title"/></h2>
      <p><fmt:message key="terms.account.text"/></p>

      <h2><fmt:message key="terms.accuracy.title"/></h2>
      <p><fmt:message key="terms.accuracy.text"/></p>

      <h2><fmt:message key="terms.availability.title"/></h2>
      <p><fmt:message key="terms.availability.text"/></p>

      <h2><fmt:message key="terms.contact.title"/></h2>
      <p><fmt:message key="terms.contact.text"/></p>

      <div class="public-actions">
        <a class="btn" href="<c:url value='/'/>"><fmt:message key="support.back"/></a>
      </div>
    </section>
  </main>
</div>
<script src="<c:url value='/assets/mobile-nav.js?v=mobile-v2'/>"></script>
</body>
</html>


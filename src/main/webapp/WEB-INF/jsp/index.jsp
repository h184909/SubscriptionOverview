<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="${pageContext.request.locale.language}">
<head>
  <fmt:setBundle basename="messages" />

  <title><fmt:message key="landing.title"/></title>
  <meta name="description" content="<fmt:message key='landing.metaDescription'/>" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="theme-color" content="#081220" />

  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />

  <link rel="icon" href="<c:url value='/favicon.ico'/>" />
  <link rel="icon" type="image/png" sizes="32x32" href="<c:url value='/favicon-32.png'/>" />
  <link rel="icon" type="image/png" sizes="16x16" href="<c:url value='/favicon-16.png'/>" />
  <link rel="apple-touch-icon" href="<c:url value='/apple-touch-icon.png'/>" />

  <style>
    .landing-page { overflow-x:hidden; }
    .landing-shell { width:min(1240px,92vw); margin:0 auto; }
    .landing-topbar { position:sticky; top:12px; z-index:20; margin-top:18px; backdrop-filter:blur(16px); -webkit-backdrop-filter:blur(16px); }
    .landing-nav-links { display:flex; align-items:center; gap:24px; margin-left:auto; }
    .landing-nav-links a { color:var(--muted); font-size:14px; }
    .landing-nav-links a:hover { color:var(--text); text-decoration:none; }
    .landing-auth { display:flex; align-items:center; gap:8px; }

    .landing-hero { display:grid; grid-template-columns:minmax(0,.9fr) minmax(520px,1.1fr); gap:54px; align-items:center; padding:88px 0 70px; }
    .landing-eyebrow { display:inline-flex; align-items:center; gap:9px; padding:8px 13px; border:1px solid rgba(52,211,153,.36); border-radius:999px; background:rgba(52,211,153,.08); color:#94f3ce; font-size:13px; font-weight:700; }
    .landing-title { max-width:660px; margin:24px 0 20px; font-size:clamp(46px,6vw,78px); line-height:.99; letter-spacing:-.055em; }
    .landing-title-accent { color:var(--good); }
    .landing-lead { max-width:620px; color:#aeb9c9; font-size:18px; line-height:1.65; }
    .landing-cta-row { display:flex; align-items:center; gap:12px; flex-wrap:wrap; margin-top:30px; }
    .landing-cta-primary { padding:14px 19px; font-weight:800; background:linear-gradient(135deg,#34d399,#22c3d6); border-color:rgba(52,211,153,.65); color:#04131a; box-shadow:0 14px 34px rgba(34,195,214,.18); }
    .landing-cta-primary:hover { color:#04131a; transform:translateY(-1px); }
    .landing-proof-row { display:flex; flex-wrap:wrap; gap:18px; margin-top:24px; color:var(--muted); font-size:13px; }
    .landing-proof-row span { display:inline-flex; align-items:center; gap:7px; }

    .landing-preview { position:relative; border:1px solid rgba(255,255,255,.12); border-radius:24px; padding:14px; background:radial-gradient(circle at top right,rgba(52,211,153,.11),transparent 35%),rgba(5,13,27,.76); box-shadow:0 30px 80px rgba(0,0,0,.48); }
    .landing-preview::before { content:""; position:absolute; inset:-1px; z-index:-1; border-radius:25px; background:linear-gradient(135deg,rgba(96,165,250,.35),rgba(52,211,153,.25),transparent 60%); filter:blur(22px); opacity:.52; }
    .preview-window { overflow:hidden; border:1px solid rgba(255,255,255,.08); border-radius:18px; background:#0b1324; }
    .preview-browser { display:flex; align-items:center; gap:6px; height:38px; padding:0 13px; border-bottom:1px solid rgba(255,255,255,.07); background:rgba(255,255,255,.025); }
    .preview-dot { width:8px; height:8px; border-radius:50%; background:rgba(255,255,255,.18); }
    .preview-body { display:grid; grid-template-columns:154px 1fr; min-height:450px; }
    .preview-sidebar { padding:18px 12px; border-right:1px solid rgba(255,255,255,.07); background:rgba(0,0,0,.12); }
    .preview-brand { display:flex; gap:8px; align-items:center; margin-bottom:20px; font-weight:800; font-size:11px; }
    .preview-logo { width:25px; height:25px; border-radius:8px; object-fit:cover; }
    .preview-menu { display:grid; gap:7px; }
    .preview-menu-item { padding:9px 10px; border-radius:9px; color:#94a3b8; font-size:10px; }
    .preview-menu-item.active { color:#dbeafe; background:rgba(96,165,250,.12); }
    .preview-content { padding:18px; min-width:0; }
    .preview-heading { display:flex; align-items:center; justify-content:space-between; gap:10px; margin-bottom:13px; }
    .preview-heading h3 { margin:0; font-size:17px; }
    .preview-chip { padding:5px 8px; border:1px solid rgba(255,255,255,.10); border-radius:8px; color:var(--muted); font-size:9px; }
    .preview-kpis { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:8px; }
    .preview-kpi,.preview-panel { border:1px solid rgba(255,255,255,.08); border-radius:11px; background:rgba(255,255,255,.025); }
    .preview-kpi { padding:11px; }
    .preview-label { color:#8190a5; font-size:8px; }
    .preview-value { margin-top:6px; font-size:15px; font-weight:900; white-space:nowrap; }
    .preview-note { margin-top:4px; color:#728096; font-size:7px; }
    .preview-grid { display:grid; grid-template-columns:1.05fr .95fr; gap:9px; margin-top:9px; }
    .preview-panel { padding:12px; }
    .preview-panel-title { margin-bottom:12px; font-size:10px; font-weight:800; }
    .preview-category { display:flex; align-items:center; gap:14px; }
    .preview-donut { width:80px; height:80px; flex:0 0 80px; border-radius:50%; background:radial-gradient(circle,#0b1324 0 45%,transparent 46%),conic-gradient(#60a5fa 0 73%,#34d399 73% 100%); }
    .preview-bars { flex:1; display:grid; gap:12px; }
    .preview-bar-row { font-size:8px; color:#a7b2c3; }
    .preview-bar { height:6px; margin-top:5px; border-radius:999px; background:rgba(255,255,255,.08); overflow:hidden; }
    .preview-bar > span { display:block; height:100%; border-radius:inherit; background:linear-gradient(90deg,#60a5fa,#34d399); }
    .preview-payment-list { display:grid; gap:8px; }
    .preview-payment { display:grid; grid-template-columns:1fr auto; gap:8px; align-items:center; padding-bottom:7px; border-bottom:1px solid rgba(255,255,255,.06); font-size:8px; }
    .preview-payment:last-child { border-bottom:0; padding-bottom:0; }
    .preview-payment b { display:block; font-size:9px; }

    .landing-section { padding:78px 0; scroll-margin-top:110px; }
    .landing-section-header { max-width:720px; margin:0 auto 38px; text-align:center; }
    .landing-section-kicker { color:var(--good); font-size:13px; font-weight:800; letter-spacing:.12em; text-transform:uppercase; }
    .landing-section-title { margin:12px 0 14px; font-size:clamp(32px,4vw,50px); line-height:1.08; letter-spacing:-.035em; }
    .landing-section-copy { color:var(--muted); font-size:16px; line-height:1.65; }
    .landing-feature-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:14px; }
    .landing-feature-card { min-height:230px; padding:22px; border:1px solid var(--border); border-radius:18px; background:linear-gradient(180deg,rgba(255,255,255,.035),rgba(255,255,255,.015)); box-shadow:var(--shadow); }
    .landing-feature-icon { display:grid; place-items:center; width:48px; height:48px; margin-bottom:22px; border-radius:14px; background:rgba(52,211,153,.10); border:1px solid rgba(52,211,153,.20); font-size:22px; }
    .landing-feature-card h3 { margin:0 0 10px; font-size:18px; }
    .landing-feature-card p { margin:0; color:var(--muted); font-size:14px; line-height:1.6; }
    .landing-steps { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:14px; }
    .landing-step { padding:24px; border:1px solid var(--border); border-radius:18px; background:rgba(255,255,255,.025); }
    .landing-step-number { display:grid; place-items:center; width:36px; height:36px; margin-bottom:18px; border-radius:50%; color:#04131a; background:linear-gradient(135deg,#60a5fa,#34d399); font-weight:900; }
    .landing-step h3 { margin:0 0 8px; }
    .landing-step p { margin:0; color:var(--muted); line-height:1.6; }
    .landing-security { display:grid; grid-template-columns:1fr 1fr; gap:16px; align-items:stretch; }
    .landing-security-main,.landing-security-side { border:1px solid var(--border); border-radius:22px; background:rgba(255,255,255,.025); box-shadow:var(--shadow); }
    .landing-security-main { display:flex; gap:20px; align-items:flex-start; padding:28px; }
    .landing-security-icon { display:grid; place-items:center; width:68px; height:68px; flex:0 0 68px; border-radius:20px; background:rgba(52,211,153,.10); border:1px solid rgba(52,211,153,.24); font-size:32px; }
    .landing-security-main h3 { margin:0 0 10px; font-size:24px; }
    .landing-security-main p { margin:0; color:var(--muted); line-height:1.65; }
    .landing-security-side { display:grid; grid-template-columns:1fr 1fr; gap:1px; overflow:hidden; }
    .landing-security-point { padding:24px; background:rgba(255,255,255,.018); }
    .landing-security-point b { display:block; margin-bottom:8px; }
    .landing-security-point span { color:var(--muted); font-size:13px; line-height:1.55; }
    .landing-final-cta { margin:24px 0 70px; padding:38px; border:1px solid rgba(52,211,153,.22); border-radius:24px; background:radial-gradient(circle at top right,rgba(52,211,153,.14),transparent 45%),radial-gradient(circle at bottom left,rgba(96,165,250,.12),transparent 45%),rgba(255,255,255,.025); text-align:center; box-shadow:var(--shadow); }
    .landing-final-cta h2 { margin:0 0 12px; font-size:clamp(30px,4vw,46px); letter-spacing:-.035em; }
    .landing-final-cta p { max-width:680px; margin:0 auto; color:var(--muted); line-height:1.65; }
    .landing-footer {
      margin-top: 24px;
      padding: 54px 0 34px;
      border-top: 1px solid var(--border);
      color: var(--muted);
      font-size: 13px;
    }

    .landing-footer-grid {
      display: grid;
      grid-template-columns: 1.25fr repeat(3, minmax(150px, .75fr));
      gap: 42px;
    }

    .landing-footer-brand {
      max-width: 320px;
    }

    .landing-footer-brandline {
      display: flex;
      align-items: center;
      gap: 10px;
      color: var(--text);
      font-weight: 800;
      font-size: 15px;
    }

    .landing-footer-brand p {
      margin: 14px 0 0;
      line-height: 1.65;
    }

    .landing-footer-column h3 {
      margin: 0 0 15px;
      color: var(--text);
      font-size: 14px;
    }

    .landing-footer-links {
      display: grid;
      gap: 11px;
    }

    .landing-footer-links a {
      width: fit-content;
      color: #9fb0c8;
    }

    .landing-footer-links a:hover {
      color: var(--text);
      text-decoration: none;
    }

    .landing-footer-bottom {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
      margin-top: 42px;
      padding-top: 24px;
      border-top: 1px solid var(--border);
    }

    .landing-footer-status {
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }

    .landing-footer-status::before {
      content: "";
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--good);
      box-shadow: 0 0 0 4px rgba(52,211,153,.10);
    }

    @media (max-width:1050px) {
      .landing-nav-links { display:none; }
      .landing-hero { grid-template-columns:1fr; gap:42px; padding-top:64px; }
      .landing-feature-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }
    }
    @media (max-width:760px) {
      .landing-topbar { position:static; }
      .landing-topbar .topbar { align-items:flex-start; }
      .landing-auth { width:100%; justify-content:flex-end; }
      .landing-hero { padding:48px 0 44px; }
      .landing-title { font-size:clamp(42px,13vw,60px); }
      .preview-body { grid-template-columns:1fr; }
      .preview-sidebar { display:none; }
      .preview-kpis { grid-template-columns:repeat(2,minmax(0,1fr)); }
      .preview-grid,.landing-security { grid-template-columns:1fr; }
      .landing-feature-grid,.landing-steps { grid-template-columns:1fr; }
      .landing-security-side { grid-template-columns:1fr; }
      .landing-footer-grid { grid-template-columns:1fr 1fr; }
      .landing-footer-bottom { align-items:flex-start; flex-direction:column; }
    }
    @media (max-width:520px) {
      .landing-shell { width:min(94vw,1240px); }
      .landing-auth .btn { padding:8px 10px; }
      .landing-hero { padding-top:38px; }
      .landing-lead { font-size:16px; }
      .landing-cta-row { align-items:stretch; flex-direction:column; }
      .landing-cta-row .btn { justify-content:center; }
      .preview-content { padding:12px; }
      .preview-kpis { grid-template-columns:1fr 1fr; }
      .preview-value { font-size:13px; }
      .landing-section { padding:58px 0; }
      .landing-security-main { flex-direction:column; }
      .landing-footer-grid { grid-template-columns:1fr; }
    }
  </style>
</head>
<body class="landing-page">
<div class="landing-shell">
  <div class="landing-topbar">
    <header class="topbar">
      <a class="brand" href="<c:url value='/'/>" aria-label="SubscriptionOverview">
        <img class="logo" src="<c:url value='/assets/logo.png'/>" alt="SubscriptionOverview" />
        <div>
          <h1>SubscriptionOverview</h1>
          <div class="sub"><fmt:message key="landing.subtitle"/></div>
        </div>
      </a>

      <nav class="landing-nav-links" aria-label="Main navigation">
        <a href="#features"><fmt:message key="landing.nav.features"/></a>
        <a href="#how-it-works"><fmt:message key="landing.nav.how"/></a>
        <a href="#security"><fmt:message key="landing.nav.security"/></a>
      </nav>

      <div class="landing-auth">
        <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
        <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
        <a href="${toEn}" class="btn" title="English" aria-label="English">🇬🇧</a>
        <a href="${toNb}" class="btn" title="Norsk" aria-label="Norsk">🇳🇴</a>
        <a class="btn" href="<c:url value='/login'/>"><fmt:message key="landing.login"/></a>
        <a class="btn btn-primary" href="<c:url value='/auth/register'/>"><fmt:message key="landing.register"/></a>
      </div>
    </header>
  </div>

  <main>
    <section class="landing-hero">
      <div>
        <div class="landing-eyebrow">✦ <fmt:message key="landing.eyebrow"/></div>
        <h2 class="landing-title">
          <fmt:message key="landing.heroLine1"/><br/>
          <fmt:message key="landing.heroLine2"/><br/>
          <span class="landing-title-accent"><fmt:message key="landing.heroLine3"/></span>
        </h2>
        <p class="landing-lead"><fmt:message key="landing.heroText"/></p>
        <div class="landing-cta-row">
          <a class="btn landing-cta-primary" href="<c:url value='/auth/register'/>"><fmt:message key="landing.primaryCta"/> →</a>
          <a class="btn" href="#how-it-works"><fmt:message key="landing.secondaryCta"/> ↓</a>
        </div>
        <div class="landing-proof-row">
          <span>✓ <fmt:message key="landing.proof.free"/></span>
          <span>✓ <fmt:message key="landing.proof.noCard"/></span>
          <span>✓ <fmt:message key="landing.proof.csv"/></span>
        </div>
      </div>

      <div class="landing-preview" aria-label="<fmt:message key='landing.previewLabel'/>">
        <div class="preview-window">
          <div class="preview-browser"><span class="preview-dot"></span><span class="preview-dot"></span><span class="preview-dot"></span></div>
          <div class="preview-body">
            <aside class="preview-sidebar">
              <div class="preview-brand"><img class="preview-logo" src="<c:url value='/assets/logo.png'/>" alt=""/><span>SubscriptionOverview</span></div>
              <div class="preview-menu">
                <div class="preview-menu-item active">⌂ Dashboard</div>
                <div class="preview-menu-item">▤ Subscriptions</div>
                <div class="preview-menu-item">⌁ Analytics</div>
                <div class="preview-menu-item">✦ Suggestions</div>
                <div class="preview-menu-item">⚙ Profile</div>
              </div>
            </aside>

            <div class="preview-content">
              <div class="preview-heading"><h3><fmt:message key="landing.preview.overview"/></h3><span class="preview-chip"><fmt:message key="landing.preview.thisMonth"/></span></div>
              <div class="preview-kpis">
                <div class="preview-kpi"><div class="preview-label"><fmt:message key="landing.preview.monthly"/></div><div class="preview-value">923 NOK</div><div class="preview-note">4 <fmt:message key="landing.preview.active"/></div></div>
                <div class="preview-kpi"><div class="preview-label"><fmt:message key="landing.preview.yearly"/></div><div class="preview-value">11 076 NOK</div><div class="preview-note"><fmt:message key="landing.preview.estimate"/></div></div>
                <div class="preview-kpi"><div class="preview-label"><fmt:message key="landing.preview.upcoming"/></div><div class="preview-value">923 NOK</div><div class="preview-note">4 <fmt:message key="landing.preview.payments"/></div></div>
                <div class="preview-kpi"><div class="preview-label"><fmt:message key="landing.preview.saved"/></div><div class="preview-value">249 NOK</div><div class="preview-note"><fmt:message key="landing.preview.perMonth"/></div></div>
              </div>

              <div class="preview-grid">
                <div class="preview-panel">
                  <div class="preview-panel-title"><fmt:message key="landing.preview.category"/></div>
                  <div class="preview-category">
                    <div class="preview-donut"></div>
                    <div class="preview-bars">
                      <div class="preview-bar-row">Utilities · 73%<div class="preview-bar"><span style="width:73%;"></span></div></div>
                      <div class="preview-bar-row">Health &amp; Fitness · 27%<div class="preview-bar"><span style="width:27%;"></span></div></div>
                    </div>
                  </div>
                </div>

                <div class="preview-panel">
                  <div class="preview-panel-title"><fmt:message key="landing.preview.nextPayments"/></div>
                  <div class="preview-payment-list">
                    <div class="preview-payment"><div><b>Skyss Mobilitet</b><span class="preview-note">16 Jul</span></div><strong>413 NOK</strong></div>
                    <div class="preview-payment"><div><b>ChatGPT</b><span class="preview-note">16 Jul</span></div><strong>249 NOK</strong></div>
                    <div class="preview-payment"><div><b>Apple</b><span class="preview-note">20 Jul</span></div><strong>12 NOK</strong></div>
                  </div>
                </div>
              </div>

              <div class="preview-grid">
                <div class="preview-panel">
                  <div class="preview-panel-title"><fmt:message key="landing.preview.projection"/></div>
                  <div class="preview-bars">
                    <div class="preview-bar-row">Jul<div class="preview-bar"><span style="width:100%;"></span></div></div>
                    <div class="preview-bar-row">Aug<div class="preview-bar"><span style="width:100%;"></span></div></div>
                    <div class="preview-bar-row">Sep<div class="preview-bar"><span style="width:100%;"></span></div></div>
                  </div>
                </div>
                <div class="preview-panel">
                  <div class="preview-panel-title"><fmt:message key="landing.preview.insight"/></div>
                  <div class="notice" style="font-size:9px;line-height:1.5;">💡 <fmt:message key="landing.preview.insightText"/></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section id="features" class="landing-section">
      <div class="landing-section-header">
        <div class="landing-section-kicker"><fmt:message key="landing.features.kicker"/></div>
        <h2 class="landing-section-title"><fmt:message key="landing.features.title"/></h2>
        <p class="landing-section-copy"><fmt:message key="landing.features.lead"/></p>
      </div>
      <div class="landing-feature-grid">
        <article class="landing-feature-card"><div class="landing-feature-icon">📥</div><h3><fmt:message key="landing.feature.import.title"/></h3><p><fmt:message key="landing.feature.import.text"/></p></article>
        <article class="landing-feature-card"><div class="landing-feature-icon">✦</div><h3><fmt:message key="landing.feature.detect.title"/></h3><p><fmt:message key="landing.feature.detect.text"/></p></article>
        <article class="landing-feature-card"><div class="landing-feature-icon">📊</div><h3><fmt:message key="landing.feature.overview.title"/></h3><p><fmt:message key="landing.feature.overview.text"/></p></article>
        <article class="landing-feature-card"><div class="landing-feature-icon">💰</div><h3><fmt:message key="landing.feature.save.title"/></h3><p><fmt:message key="landing.feature.save.text"/></p></article>
      </div>
    </section>

    <section id="how-it-works" class="landing-section">
      <div class="landing-section-header">
        <div class="landing-section-kicker"><fmt:message key="landing.steps.kicker"/></div>
        <h2 class="landing-section-title"><fmt:message key="landing.steps.title"/></h2>
        <p class="landing-section-copy"><fmt:message key="landing.steps.lead"/></p>
      </div>
      <div class="landing-steps">
        <article class="landing-step"><div class="landing-step-number">1</div><h3><fmt:message key="landing.step1.title"/></h3><p><fmt:message key="landing.step1.text"/></p></article>
        <article class="landing-step"><div class="landing-step-number">2</div><h3><fmt:message key="landing.step2.title"/></h3><p><fmt:message key="landing.step2.text"/></p></article>
        <article class="landing-step"><div class="landing-step-number">3</div><h3><fmt:message key="landing.step3.title"/></h3><p><fmt:message key="landing.step3.text"/></p></article>
      </div>
    </section>

    <section id="security" class="landing-section">
      <div class="landing-security">
        <div class="landing-security-main">
          <div class="landing-security-icon">🛡️</div>
          <div><h3><fmt:message key="landing.security.title"/></h3><p><fmt:message key="landing.security.text"/></p></div>
        </div>
        <div class="landing-security-side">
          <div class="landing-security-point"><b><fmt:message key="landing.security.control.title"/></b><span><fmt:message key="landing.security.control.text"/></span></div>
          <div class="landing-security-point"><b><fmt:message key="landing.security.transparent.title"/></b><span><fmt:message key="landing.security.transparent.text"/></span></div>
        </div>
      </div>
    </section>

    <section class="landing-final-cta">
      <h2><fmt:message key="landing.final.title"/></h2>
      <p><fmt:message key="landing.final.text"/></p>
      <div class="landing-cta-row" style="justify-content:center;">
        <a class="btn landing-cta-primary" href="<c:url value='/auth/register'/>"><fmt:message key="landing.primaryCta"/> →</a>
        <a class="btn" href="<c:url value='/login'/>"><fmt:message key="landing.login"/></a>
      </div>
    </section>
  </main>

  <footer class="landing-footer">
    <div class="landing-footer-grid">
      <div class="landing-footer-brand">
        <div class="landing-footer-brandline">
          <img class="preview-logo"
               src="<c:url value='/assets/logo.png'/>"
               alt="" />
          <span>SubscriptionOverview</span>
        </div>

        <p>
          <fmt:message key="landing.footer.description"/>
        </p>
      </div>

      <div class="landing-footer-column">
        <h3><fmt:message key="landing.footer.product"/></h3>
        <div class="landing-footer-links">
          <a href="#features"><fmt:message key="landing.nav.features"/></a>
          <a href="#how-it-works"><fmt:message key="landing.nav.how"/></a>
          <a href="#security"><fmt:message key="landing.nav.security"/></a>
          <a href="<c:url value='/auth/register'/>">
            <fmt:message key="landing.register"/>
          </a>
        </div>
      </div>

      <div class="landing-footer-column">
        <h3><fmt:message key="landing.footer.support"/></h3>
        <div class="landing-footer-links">
          <a href="<c:url value='/support'/>">
            <fmt:message key="landing.footer.help"/>
          </a>
          <a href="<c:url value='/support'/>#faq">
            <fmt:message key="landing.footer.faq"/>
          </a>
          <a href="mailto:support@subscriptionoverview.com">
            <fmt:message key="landing.footer.contact"/>
          </a>
        </div>
      </div>

      <div class="landing-footer-column">
        <h3><fmt:message key="landing.footer.legal"/></h3>
        <div class="landing-footer-links">
          <a href="<c:url value='/privacy'/>">
            <fmt:message key="landing.footer.privacy"/>
          </a>
          <a href="<c:url value='/terms'/>">
            <fmt:message key="landing.footer.terms"/>
          </a>
        </div>
      </div>
    </div>

    <div class="landing-footer-bottom">
      <div>
        © <span id="currentYear"></span> SubscriptionOverview
      </div>

      <div class="landing-footer-status">
        <fmt:message key="landing.footer.status"/>
      </div>
    </div>
  </footer>
</div>

<script>
  const yearNode = document.getElementById("currentYear");
  if (yearNode) yearNode.textContent = new Date().getFullYear();

  document.querySelectorAll('a[href^="#"]').forEach(link => {
    link.addEventListener("click", event => {
      const selector = link.getAttribute("href");
      if (!selector || selector === "#") return;
      const target = document.querySelector(selector);
      if (!target) return;
      event.preventDefault();
      target.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  });
</script>
</body>
</html>

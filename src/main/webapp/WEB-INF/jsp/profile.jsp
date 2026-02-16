<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html>
<head>
  <fmt:setBundle basename="messages" />
  <title>Profile</title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
</head>
<body>

<div class="container">
  <div class="topbar">
    <div class="brand">
      <div class="logo" aria-hidden="true"></div>
      <div>
        <h1>SubscriptionOverview</h1>
        <div class="sub">Profile</div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
      <a href="<c:url value='/app/subscriptions'/>"><fmt:message key="nav.subscriptions"/></a>
      <a href="<c:url value='/app/suggestions'/>"><fmt:message key="nav.suggestions"/></a>
      <a href="<c:url value='/app/transactions/import-csv'/>"><fmt:message key="nav.importCsv"/></a>

      <span class="muted" style="margin:0 6px;">|</span>

      <c:url var="toEn" value="/lang"><c:param name="v" value="en"/></c:url>
      <c:url var="toNb" value="/lang"><c:param name="v" value="nb"/></c:url>
      <a href="${toEn}" title="English" aria-label="English">🇬🇧</a>
      <a href="${toNb}" title="Norsk" aria-label="Norsk">🇳🇴</a>
    </div>
  </div>

  <c:if test="${not empty flashMsg}">
    <div class="card"><div class="notice flash"><b><c:out value="${flashMsg}"/></b></div></div>
  </c:if>

  <c:if test="${not empty flashError}">
    <div class="card"><div class="notice error"><b><c:out value="${flashError}"/></b></div></div>
  </c:if>

  <div class="grid two">
    <div class="card">
      <h3>Account</h3>
      <div class="muted">Signed in as: <b><c:out value="${email}"/></b></div>

      <hr class="sep"/>

      <h4 style="margin:0 0 8px;">Change password</h4>

      <form class="form" method="post" action="<c:url value='/app/profile/change-password'/>">
        <div class="field">
          <label>Current password</label>
          <input type="password" name="currentPassword" required />
        </div>

        <div class="field">
          <label>New password</label>
          <input type="password" name="newPassword" required />
        </div>

        <div class="field">
          <label>Repeat new password</label>
          <input type="password" name="newPassword2" required />
          <div class="muted">Minimum 8 characters.</div>
        </div>

        <div class="row" style="justify-content:flex-end;">
          <button class="btn btn-primary" type="submit">Save</button>
        </div>
      </form>
    </div>

    <div class="card">
      <h3>Danger zone</h3>
      <div class="muted">Delete your account and all related data (subscriptions, transactions, decisions).</div>

      <hr class="sep"/>

      <form class="form" method="post" action="<c:url value='/app/profile/delete'/>"
            onsubmit="return confirm('This will delete your account permanently. Continue?');">
        <div class="field">
          <label>Password</label>
          <input type="password" name="password" required />
        </div>

        <div class="field">
          <label>Type <b>DELETE</b> to confirm</label>
          <input type="text" name="confirm" placeholder="DELETE" required />
        </div>

        <div class="row" style="justify-content:flex-end;">
          <button class="btn btn-danger" type="submit">Delete account</button>
        </div>
      </form>
    </div>
  </div>

</div>

</body>
</html>
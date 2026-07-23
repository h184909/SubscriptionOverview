<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<html data-context-path="${pageContext.request.contextPath}">
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="subs.title"/></title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/assets/app.css?v=mobile-v2" />

  <link rel="icon" href="<c:url value='/favicon.ico'/>" />
  <link rel="icon" type="image/png" sizes="32x32" href="<c:url value='/favicon-32.png'/>" />
  <link rel="icon" type="image/png" sizes="16x16" href="<c:url value='/favicon-16.png'/>" />
  <link rel="apple-touch-icon" href="<c:url value='/apple-touch-icon.png'/>" />
  <meta name="theme-color" content="#0b1220" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <link rel="stylesheet" href="<c:url value='/assets/subscription-details.css?v=2'/>" />
</head>
<body>

<div class="container">
  <div class="topbar">
    <div class="brand">
      <img class="logo" src="<c:url value='/assets/logo.png'/>" alt="SubscriptionOverview" />
      <div>
        <h1>SubscriptionOverview</h1>
        <div class="sub"><fmt:message key="subs.subtitle"/></div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
      <a href="<c:url value='/app/analytics'/>">
        <fmt:message key="nav.analytics"/>
      </a>
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

  <div class="card">
    <div class="row" style="display:flex; justify-content:space-between; gap:12px; flex-wrap:wrap; align-items:center;">
      <div>
        <h3 style="margin:0;"><fmt:message key="subs.header"/></h3>
        <div class="muted" style="margin-top:6px;"><fmt:message key="subs.lead"/></div>
      </div>

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <a class="btn btn-primary" href="<c:url value='/app/subscriptions/new'/>"><fmt:message key="subs.add"/></a>
        <a class="btn" href="<c:url value='/app/suggestions'/>"><fmt:message key="subs.seeSuggestions"/></a>
      </div>
    </div>
  </div>

  <c:if test="${empty subs}">
    <div class="card">
      <h3><fmt:message key="subs.noneTitle"/></h3>
      <div class="muted"><fmt:message key="subs.noneText"/></div>
    </div>
  </c:if>

  <c:if test="${not empty subs}">
    <div class="card">
      <div class="tablewrap">
        <table>
          <thead>
          <tr>
            <th><fmt:message key="table.name"/></th>
            <th style="width:190px;">Category</th>
            <th><fmt:message key="table.price"/></th>
            <th><fmt:message key="table.interval"/></th>
            <th><fmt:message key="table.nextCharge"/></th>
            <th><fmt:message key="subs.status"/></th>
            <th><fmt:message key="subs.cancelProvider"/></th>
            <th><fmt:message key="table.action"/></th>
          </tr>
          </thead>
          <tbody>

          <c:forEach var="s" items="${subs}">
            <tr class="subscription-details-trigger" data-subscription-id="${s.id}" tabindex="0">
              <td>
                <div style="display:flex; flex-direction:column; gap:8px;">
                  <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
                    <b><c:out value="${s.name}" /></b>
                    <button type="button" class="btn" onclick="openRename('${s.id}')">
                      Rename
                    </button>
                  </div>

                  <div id="renameBox-${s.id}" style="display:none;">
                    <form method="post"
                          action="${pageContext.request.contextPath}/app/subscriptions/rename"
                          style="display:flex; gap:6px; align-items:center; flex-wrap:wrap; margin:0;">
                      <input type="hidden" name="id" value="${s.id}" />
                      <input type="text"
                             name="name"
                             value="<c:out value='${s.name}'/>"
                             maxlength="80"
                             style="max-width:240px;"
                             aria-label="New name"
                             onkeydown="renameKeydown(event, '${s.id}')" />
                      <button type="submit" class="btn btn-primary"><fmt:message key="common.save"/></button>
                      <button type="button" class="btn" onclick="closeRename('${s.id}')"><fmt:message key="common.cancel"/></button>
                    </form>
                    <div class="muted" style="margin-top:6px;">
                      <fmt:message key="subs.renameHint"/>
                    </div>
                  </div>
                </div>
              </td>

              <td>
                <div style="display:flex; flex-direction:column; gap:8px;">
                  <button type="button"
                          class="btn"
                          onclick="openCategory('${s.id}')"
                          style="padding:0; border:0; background:transparent; text-align:left;">
                    <c:choose>
                      <c:when test="${empty s.category || s.category == 'Other'}">
                        <span class="muted">No category</span>
                      </c:when>
                      <c:otherwise>
                        <span class="pill"
                              style="white-space:nowrap; min-width:145px; display:inline-flex; justify-content:center;">
                          <c:choose>
                            <c:when test="${s.category == 'Entertainment'}"></c:when>
                            <c:when test="${s.category == 'Telecom'}"></c:when>
                            <c:when test="${s.category == 'Utilities'}"></c:when>
                            <c:when test="${s.category == 'Health & Fitness'}"></c:when>
                            <c:when test="${s.category == 'News'}"></c:when>
                            <c:when test="${s.category == 'Shopping & Food'}"></c:when>
                            <c:otherwise>📦 </c:otherwise>
                          </c:choose>
                          <c:out value="${s.category}" />
                        </span>
                      </c:otherwise>
                    </c:choose>
                  </button>

                  <div id="categoryBox-${s.id}" style="display:none;">
                    <form method="post"
                          action="${pageContext.request.contextPath}/app/subscriptions/category"
                          style="display:flex; gap:6px; align-items:center; flex-wrap:wrap; margin:0;">
                      <input type="hidden" name="id" value="${s.id}" />

                      <select name="category" onkeydown="categoryKeydown(event, '${s.id}')">
                        <option value="" <c:if test="${empty s.category}">selected</c:if>>No category</option>
                        <option value="Entertainment" <c:if test="${s.category == 'Entertainment'}">selected</c:if>>Entertainment</option>
                        <option value="Utilities" <c:if test="${s.category == 'Utilities'}">selected</c:if>>Utilities</option>
                        <option value="Telecom" <c:if test="${s.category == 'Telecom'}">selected</c:if>>Telecom</option>
                        <option value="Health & Fitness" <c:if test="${s.category == 'Health & Fitness'}">selected</c:if>>Health & Fitness</option>
                        <option value="News" <c:if test="${s.category == 'News'}">selected</c:if>>News</option>
                        <option value="Shopping & Food" <c:if test="${s.category == 'Shopping & Food'}">selected</c:if>>Shopping & Food</option>
                        <option value="Other" <c:if test="${s.category == 'Other'}">selected</c:if>>Other</option>
                      </select>

                      <button type="submit" class="btn btn-primary"><fmt:message key="common.save"/></button>
                      <button type="button" class="btn" onclick="closeCategory('${s.id}')"><fmt:message key="common.cancel"/></button>
                    </form>
                  </div>
                </div>
              </td>

              <td><c:out value="${s.amount}" /> <c:out value="${s.currency}" /></td>
              <td><c:out value="${s.interval}" /></td>

              <td>
                <c:choose>
                  <c:when test="${empty s.nextChargeDate}">-</c:when>
                  <c:otherwise><c:out value="${s.nextChargeDate}" /></c:otherwise>
                </c:choose>
              </td>

              <td>
                <c:choose>
                  <c:when test="${s.active}">
                    <span class="pill ok"><fmt:message key="subs.active"/></span>
                  </c:when>
                  <c:otherwise>
                    <span class="pill warn"><fmt:message key="subs.inactive"/></span>
                  </c:otherwise>
                </c:choose>
              </td>

              <td>
                <c:choose>
                  <c:when test="${s.active}">
                    <c:set var="cancelUrl" value="${cancelLinks[s.id.toString()]}" />
                    <c:choose>
                      <c:when test="${not empty cancelUrl}">
                        <a href="${cancelUrl}" target="_blank" rel="noopener">Manage</a>
                      </c:when>
                      <c:otherwise>-</c:otherwise>
                    </c:choose>
                  </c:when>
                  <c:otherwise>-</c:otherwise>
                </c:choose>
              </td>

              <td>
                <c:choose>
                  <c:when test="${s.active}">
                    <form method="post" action="<c:url value='/app/subscriptions/cancel'/>"
                          onsubmit="return confirm('<fmt:message key="subs.confirmCancel"/>: ${s.name}?');"
                          style="display:inline;">
                      <input type="hidden" name="id" value="${s.id}" />
                      <button type="submit" class="btn"><fmt:message key="subs.cancelInApp"/></button>
                    </form>
                  </c:when>
                  <c:otherwise>
                    <form method="post" action="<c:url value='/app/subscriptions/reactivate'/>"
                          onsubmit="return confirm('<fmt:message key="subs.confirmReactivate"/>: ${s.name}?');"
                          style="display:inline;">
                      <input type="hidden" name="id" value="${s.id}" />
                      <button type="submit" class="btn"><fmt:message key="subs.reactivate"/></button>
                    </form>
                  </c:otherwise>
                </c:choose>

                <form method="post" action="<c:url value='/app/subscriptions/delete'/>"
                      onsubmit="return confirm('<fmt:message key="subs.confirmDelete"/>: ${s.name}?');"
                      style="display:inline; margin-left:6px;">
                  <input type="hidden" name="id" value="${s.id}" />
                  <button type="submit" class="btn"><fmt:message key="common.delete"/></button>
                </form>
              </td>
            </tr>
          </c:forEach>

          </tbody>
        </table>
      </div>
    </div>
  </c:if>

</div>

<script>
  function closeAllRename() {
    const boxes = document.querySelectorAll("[id^='renameBox-']");
    boxes.forEach(b => b.style.display = "none");
  }

  function closeAllCategory() {
    const boxes = document.querySelectorAll("[id^='categoryBox-']");
    boxes.forEach(b => b.style.display = "none");
  }

  function openRename(id) {
    closeAllRename();
    closeAllCategory();

    const box = document.getElementById("renameBox-" + id);
    if (!box) return;
    box.style.display = "block";

    const input = box.querySelector("input[name='name']");
    if (input) {
      input.focus();
      input.select();
    }
  }

  function closeRename(id) {
    const box = document.getElementById("renameBox-" + id);
    if (!box) return;
    box.style.display = "none";
  }

  function openCategory(id) {
    closeAllCategory();
    closeAllRename();

    const box = document.getElementById("categoryBox-" + id);
    if (!box) return;
    box.style.display = "block";

    const select = box.querySelector("select[name='category']");
    if (select) {
      select.focus();
    }
  }

  function closeCategory(id) {
    const box = document.getElementById("categoryBox-" + id);
    if (!box) return;
    box.style.display = "none";
  }

  function renameKeydown(e, id) {
    if (!e) return;
    if (e.key === "Escape") {
      e.preventDefault();
      closeRename(id);
    }
  }

  function categoryKeydown(e, id) {
    if (!e) return;
    if (e.key === "Escape") {
      e.preventDefault();
      closeCategory(id);
    }
  }

  document.addEventListener("click", function(e){
    const t = e.target;
    if (!t) return;

    if (t.closest && (
            t.closest("[id^='renameBox-']") ||
            t.closest("[id^='categoryBox-']") ||
            t.closest("button[onclick*='openRename']") ||
            t.closest("button[onclick*='openCategory']")
    )) {
      return;
    }

    closeAllRename();
    closeAllCategory();
  });
</script>
<script src="<c:url value='/assets/mobile-nav.js?v=mobile-v2'/>"></script>
  <script src="<c:url value='/assets/subscription-details.js?v=3'/>"></script>
</body>
</html>


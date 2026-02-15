<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<html>
<head>
  <fmt:setBundle basename="messages" />
  <title><fmt:message key="subs.title"/></title>
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
        <div class="sub"><fmt:message key="subs.subtitle"/></div>
      </div>
    </div>

    <div class="nav">
      <a href="<c:url value='/app'/>"><fmt:message key="nav.dashboard"/></a>
      <a href="<c:url value='/app/suggestions'/>"><fmt:message key="nav.suggestions"/></a>
      <a href="<c:url value='/app/transactions/import-csv'/>"><fmt:message key="nav.importCsv"/></a>

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
            <tr>
              <!-- Rename UI -->
              <td>
                <div style="display:flex; flex-direction:column; gap:8px;">
                  <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
                    <b><c:out value="${s.name}" /></b>
                    <button type="button"
                            class="btn"
                            onclick="openRename('${s.id}')">
                      <fmt:message key="subs.rename"/>
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
                  <c:when test="${s.active}"><span class="pill ok"><fmt:message key="subs.active"/></span></c:when>
                  <c:otherwise><span class="pill warn"><fmt:message key="subs.inactive"/></span></c:otherwise>
                </c:choose>
              </td>

              <td>
                <c:choose>
                  <c:when test="${s.active}">
                    <c:set var="cancelUrl" value="${cancelLinks[s.id.toString()]}" />
                    <c:if test="${not empty cancelUrl}">
                      <a href="${cancelUrl}" target="_blank" rel="noopener"><fmt:message key="subs.cancelLink"/></a>
                    </c:if>
                    <c:if test="${empty cancelUrl}">-</c:if>
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
  // Lukker alle rename-bokser
  function closeAllRename() {
    const boxes = document.querySelectorAll("[id^='renameBox-']");
    boxes.forEach(b => b.style.display = "none");
  }

  function openRename(id) {
    closeAllRename();
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

  function renameKeydown(e, id) {
    if (!e) return;
    if (e.key === "Escape") {
      e.preventDefault();
      closeRename(id);
    }
  }

  // Klikk utenfor lukker åpne bokser
  document.addEventListener("click", function(e){
    const t = e.target;
    if (!t) return;
    // Hvis du klikker inni rename box eller på rename-knapp, ikke lukk
    if (t.closest && (t.closest("[id^='renameBox-']") || (t.tagName === "BUTTON" && t.getAttribute("onclick") && t.getAttribute("onclick").includes("openRename")))) {
      return;
    }
    closeAllRename();
  });
</script>

</body>
</html>
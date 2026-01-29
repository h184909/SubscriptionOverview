<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
  <title>Velg bank</title>
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
        <div class="sub">Velg bank</div>
      </div>
    </div>
    <div class="nav">
      <a href="<c:url value='/app'/>">Dashboard</a>
      <a href="<c:url value='/app/suggestions'/>">Forslag</a>
    </div>
  </div>

  <div class="card">
    <h3>Velg bank</h3>
    <div class="muted">Miljø: <b><c:out value="${env}" /></b></div>

    <c:if test="${empty institutions}">
      <hr class="sep"/>
      <div class="muted">Fant ingen institusjoner.</div>
    </c:if>

    <c:if test="${not empty institutions}">
      <hr class="sep"/>
      <div class="tablewrap">
        <table>
          <thead>
          <tr>
            <th>Navn</th>
            <th>ID</th>
            <th>Handling</th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="i" items="${institutions}">
            <tr>
              <td><b><c:out value="${i.name}" /></b></td>
              <td class="muted"><c:out value="${i.id}" /></td>
              <td>
                <a class="btn btn-primary"
                   href="<c:url value='/openbanking/connect'><c:param name='institutionId' value='${i.id}'/></c:url>">
                  Koble til
                </a>
              </td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
      </div>
    </c:if>
  </div>
</div>

</body>
</html>

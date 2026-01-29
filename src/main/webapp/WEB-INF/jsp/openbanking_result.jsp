<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<html>
<head>
    <title>OpenBanking</title>
</head>
<body>

<h2>OpenBanking</h2>

<c:if test="${not empty msg}">
    <p><b>${msg}</b></p>
</c:if>

<c:if test="${not empty accounts}">
    <h3>Kontoer</h3>
    <table border="1" cellpadding="6">
        <tr>
            <th>ID</th>
            <th>Nickname</th>
            <th>Currency</th>
            <th>Handling</th>
        </tr>

        <c:forEach var="a" items="${accounts}">
            <tr>
                <td>${a.id}</td>
                <td>${a.nickname}</td>
                <td>${a.currency}</td>
                <td>
                    <a href="<c:url value='/openbanking/test/transactions'>
                                <c:param name='accountId' value='${a.id}'/>
                             </c:url>">
                        Importer transaksjoner
                    </a>
                </td>
            </tr>
        </c:forEach>
    </table>
</c:if>

<p><a href="<c:url value='/app'/>">Tilbake til dashboard</a></p>

</body>
</html>

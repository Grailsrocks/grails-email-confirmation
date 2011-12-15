<html>
<head>
    <meta name="layout" content="main"/>
</head>
<body>
<g:if test="${success}">
	Thank you for confirming your email address ${email}
</g:if>
<g:else>
	Sorry but we have been unable to confirm your email address. Please check the link
	in the email you received is correct.
</g:else>
</body>
</html>
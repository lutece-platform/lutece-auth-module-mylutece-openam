<%@ page errorPage="../site/ErrorPagePortal.jsp" %>
${ pageContext.response.sendRedirect( myLuteceOpenamApp.doRegisterUser( pageContext.request ) ) }

/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.mylutece.modules.openam.web;

import javax.security.auth.login.LoginException;

import fr.paris.lutece.plugins.mylutece.modules.openam.authentication.OpenamAuthentication;
import fr.paris.lutece.plugins.mylutece.modules.openam.authentication.OpenamUser;
import fr.paris.lutece.plugins.mylutece.modules.openam.service.OpenamService;
import fr.paris.lutece.portal.service.security.ISecurityTokenService;
import fr.paris.lutece.portal.service.security.LoginRedirectException;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.util.json.AbstractJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Entry points of the OpenAM JSP: JSON login, authentication status, registration of the user signed in on OpenAM.
 */
@RequestScoped
@Named( "myLuteceOpenamApp" )
public class MyLuteceOpenamApp
{
    private static final String PARAMETER_USERNAME = "username";
    private static final String PARAMETER_PASSWORD = "password";
    private static final String TOKEN_ACTION_LOGIN = "dologin";
    private static final String JSON_ERROR_AUTHENTICATION_NOT_ENABLE = "AUTHENTICATION_NOT_ENABLE";
    private static final String JSON_ERROR_LOGIN_ERROR = "LOGIN_ERROR";
    private static final String URL_MYLUTECE_LOGIN = "../site/plugins/mylutece/DoMyLuteceLogin.jsp?auth_provider=" + OpenamService.PLUGIN_NAME;

    @Inject
    private OpenamAuthentication _openAmAuthentication;

    @Inject
    private OpenamService _openamService;

    @Inject
    private ISecurityTokenService _securityTokenService;

    /**
     * Check if the current user is authenticated
     * 
     * @param request
     *            The request
     * @return A JSON string containing true in the field result if the user is authenticated
     */
    public String isUserAuthenticated( HttpServletRequest request )
    {
        AbstractJsonResponse jsonResponse;

        if ( SecurityService.isAuthenticationEnable( ) )
        {
            jsonResponse = new JsonResponse( SecurityService.getInstance( ).getRegisteredUser( request ) != null );
        }
        else
        {
            jsonResponse = new OpenamErrorJsonResponse( JSON_ERROR_AUTHENTICATION_NOT_ENABLE );
        }

        return JsonUtil.buildJsonResponse( jsonResponse );
    }

    /**
     * Signs the user in on OpenAM with the username and password of the request, and sets the OpenAM cookie.
     * 
     * @param request
     *            The request
     * @param response
     *            The response
     * @return A JSON string containing true if the user is authenticated, or the error code and a new token
     */
    public String doLogin( HttpServletRequest request, HttpServletResponse response )
    {
        String strUsername = request.getParameter( PARAMETER_USERNAME );
        String strPassword = request.getParameter( PARAMETER_PASSWORD );

        AbstractJsonResponse jsonResponse = null;

        if ( SecurityService.isAuthenticationEnable( ) )
        {
            try
            {
                LuteceUser user = _openAmAuthentication.login( strUsername, strPassword, request );

                if ( user != null )
                {
                    SecurityService.getInstance( ).registerUser( request, user );
                    _openamService.setConnectionCookie( ( (OpenamUser) user ).getSubjectId( ), response );

                    jsonResponse = new JsonResponse( Boolean.TRUE );
                }
            }
            catch( LoginException | LoginRedirectException e )
            {
                jsonResponse = new OpenamErrorJsonResponse( JSON_ERROR_LOGIN_ERROR, _securityTokenService.getToken( request, TOKEN_ACTION_LOGIN ) );
            }
        }
        else
        {
            jsonResponse = new OpenamErrorJsonResponse( JSON_ERROR_AUTHENTICATION_NOT_ENABLE );
        }

        return JsonUtil.buildJsonResponse( jsonResponse );
    }

    /**
     * Registers the user already signed in on OpenAM.
     *
     * @param request
     *            The request
     * @return the url of the MyLutece login of this provider, to redirect to
     */
    public String doRegisterUser( HttpServletRequest request )
    {
        LuteceUser user = _openAmAuthentication.getHttpAuthenticatedUser( request );

        if ( user != null )
        {
            SecurityService.getInstance( ).registerUser( request, user );
        }

        return URL_MYLUTECE_LOGIN;
    }
}

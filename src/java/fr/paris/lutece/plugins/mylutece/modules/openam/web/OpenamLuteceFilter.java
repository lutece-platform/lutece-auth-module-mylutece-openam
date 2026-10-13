/*
 * Copyright (c) 2002-2021, City of Paris
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

import fr.paris.lutece.plugins.mylutece.modules.openam.authentication.OpenamAuthentication;
import fr.paris.lutece.plugins.mylutece.modules.openam.authentication.OpenamUser;
import fr.paris.lutece.plugins.mylutece.modules.openam.service.IOpenamLuteceUserSessionService;
import fr.paris.lutece.plugins.mylutece.modules.openam.service.OpenamService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ParisConnectLuteceFilters
 *
 */
public class OpenamLuteceFilter implements Filter
{
    private OpenamService _openamService;
    private OpenamAuthentication _openamAuthentication;
    private IOpenamLuteceUserSessionService _userSessionService;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void destroy( )
    {
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void doFilter( ServletRequest servletRequest, ServletResponse response, FilterChain chain ) throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( request );

        if ( ( user == null ) || !_userSessionService.isLuteceUserUpToDate( request.getSession( true ).getId( ) ) )
        {
            user = _openamAuthentication.getHttpAuthenticatedUser( request );

            if ( user != null )
            {
                SecurityService.getInstance( ).registerUser( request, user );
            }
            else
            {
                String strConnexionCookie = _openamService.getConnectionCookie( request );

                if ( !StringUtils.isEmpty( strConnexionCookie ) )
                {
                    _openamService.removeConnectionCookie( (HttpServletResponse) response );
                }
            }
        }
        else
            if ( user instanceof OpenamUser )
            {

                if ( !_openamService.isTokenValidated( ( (OpenamUser) user ).getSubjectId( ) ) )
                {
                    _openamService.removeConnectionCookie( (HttpServletResponse) response );
                    SecurityService.getInstance( ).logoutUser( request );
                }
                else
                {

                    String strConnexionCookie = _openamService.getConnectionCookie( request );

                    if ( ( !StringUtils.isEmpty( ( (OpenamUser) user ).getSubjectId( ) ) )
                            && ( ( strConnexionCookie == null ) || ( !strConnexionCookie.equals( ( (OpenamUser) user ).getSubjectId( ) ) ) ) )
                    {
                        _openamService.setConnectionCookie( ( (OpenamUser) user ).getSubjectId( ), (HttpServletResponse) response );
                    }
                }
            }

        chain.doFilter( servletRequest, response );
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void init( FilterConfig config ) throws ServletException
    {
        _openamService = CDI.current( ).select( OpenamService.class ).get( );
        _openamAuthentication = CDI.current( ).select( OpenamAuthentication.class ).get( );
        _userSessionService = CDI.current( ).select( IOpenamLuteceUserSessionService.class ).get( );
    }
}

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
package fr.paris.lutece.plugins.mylutece.modules.openam.service;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fr.paris.lutece.plugins.mylutece.authentication.MultiLuteceAuthentication;
import fr.paris.lutece.plugins.mylutece.modules.openam.authentication.OpenamAuthentication;
import fr.paris.lutece.plugins.mylutece.modules.openam.authentication.OpenamUser;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * OpenamService
 */
@ApplicationScoped
public class OpenamService
{
    public static final String PLUGIN_NAME = "mylutece-openam";
    public static final String ERROR_ALREADY_SUBSCRIBE = "ALREADY_SUBSCRIBE";
    public static final String ERROR_DURING_SUBSCRIBE = "ERROR_DURING_SUBSCRIBE";
    public static final String AUTHENTICATION_BEAN_NAME = "mylutece-openam.authentication";
    private static final String PROPERTY_AGENT_ENABLE = "mylutece-openam.agentEnable";
    private static final String PROPERTY_COOKIE_OPENAM_NAME = "mylutece-openam.cookieName";
    private static final String PROPERTY_COOKIE_OPENAM_DOMAIN = "mylutece-openam.cookieDomain";
    private static final String PROPERTY_COOKIE_OPENAM_PATH = "mylutece-openam.cookiePath";
    private static final String PROPERTY_COOKIE_OPENAM_MAX_AGE = "mylutece-openam.cookieMaxAge";
    private static final String PROPERTY_COOKIE_OPENAM_MAX_SECURE = "mylutece-openam.cookieSecure";
    public static final String PROPERTY_USER_KEY_NAME = "mylutece-openam.attributeKeyUsername";
    public static final String PROPERTY_USER_MAPPING_ATTRIBUTES = "mylutece-openam.userMappingAttributes";
    public static final String CONSTANT_LUTECE_USER_PROPERTIES_PATH = "mylutece-openam.attribute";
    private static final String SEPARATOR = ",";
    @Inject
    @ConfigProperty( name = PROPERTY_AGENT_ENABLE, defaultValue = "false" )
    private boolean _bAgentEnable;

    @Inject
    @ConfigProperty( name = PROPERTY_COOKIE_OPENAM_NAME )
    private Optional<String> _configCookieName;

    @Inject
    @ConfigProperty( name = PROPERTY_COOKIE_OPENAM_DOMAIN )
    private Optional<String> _configCookieDomain;

    @Inject
    @ConfigProperty( name = PROPERTY_COOKIE_OPENAM_PATH )
    private Optional<String> _configCookiePath;

    @Inject
    @ConfigProperty( name = PROPERTY_COOKIE_OPENAM_MAX_AGE, defaultValue = "1800" )
    private int _nCookieMaxAge;

    @Inject
    @ConfigProperty( name = PROPERTY_COOKIE_OPENAM_MAX_SECURE, defaultValue = "true" )
    private boolean _bCookieSecure;

    @Inject
    @ConfigProperty( name = PROPERTY_USER_KEY_NAME )
    private Optional<String> _configAttributeUserKeyName;

    @Inject
    @ConfigProperty( name = PROPERTY_USER_MAPPING_ATTRIBUTES )
    private Optional<String> _configUserMappingAttributes;

    private String _strCookieName;
    private String _strCookieDomain;
    private String _strCookiePath;
    private String _strAttributeUserKeyName;
    private Map<String, List<String>> _mapAttributeUserMapping;

    @Inject
    @Named( AUTHENTICATION_BEAN_NAME )
    private OpenamAuthentication _authentication;

    @Inject
    private OpenamAPIService _openamAPIService;

    @Inject
    private Instance<IIdentityProviderService> _identityProviderServices;

    /**
     * Resolves the configuration and builds the mapping of the OpenAM attributes to the Lutece user attributes.
     */
    @PostConstruct
    void loadConfiguration( )
    {
        _strCookieName = _configCookieName.orElse( null );
        _strCookieDomain = _configCookieDomain.orElse( null );
        _strCookiePath = _configCookiePath.orElse( null );
        _strAttributeUserKeyName = _configAttributeUserKeyName.orElse( null );

        String strUserMappingAttributes = _configUserMappingAttributes.orElse( null );
        _mapAttributeUserMapping = new HashMap<>( );

        if ( StringUtils.isNotBlank( strUserMappingAttributes ) )
        {
            for ( String strUserProperty : strUserMappingAttributes.split( SEPARATOR ) )
            {
                String strOpenamAttribute = AppPropertiesService.getProperty( CONSTANT_LUTECE_USER_PROPERTIES_PATH + "." + strUserProperty );

                if ( StringUtils.isNotBlank( strOpenamAttribute ) )
                {
                    _mapAttributeUserMapping.computeIfAbsent( strOpenamAttribute, k -> new ArrayList<>( ) ).add( strUserProperty );
                }
            }
        }
    }

    /**
     * Registers the authentication when the application starts.
     *
     * @param context
     *            the servlet context
     */
    public void registerAuthentication( @Observes @Initialized( ApplicationScoped.class ) ServletContext context )
    {
        MultiLuteceAuthentication.registerAuthentication( _authentication );
    }

    /**
     * Process login
     *
     * @param request
     *            The HTTP request
     * @param strUserName
     *            The user's name
     * @param strUserPassword
     *            The user's password
     * @param openamAuthentication
     *            The authentication
     * @return The LuteceUser
     */
    public OpenamUser doLogin( HttpServletRequest request, String strUserName, String strUserPassword, OpenamAuthentication openamAuthentication )
            throws OpenamAuthenticationAgentException
    {
        String strTokenId;
        OpenamUser user = null;

        Map<String, String> headerUserInformations = null;

        if ( isAgentEnabled( ) )
        {
            headerUserInformations = getUserInformationInHeaderRequest( request );

            if ( ( headerUserInformations != null ) && !headerUserInformations.isEmpty( ) && headerUserInformations.containsKey( _strAttributeUserKeyName ) )
            {
                user = new OpenamUser( headerUserInformations.get( _strAttributeUserKeyName ), openamAuthentication, getConnectionCookie( request ) );
                addUserAttributes( headerUserInformations, user );
            }
            else
            {
                throw new OpenamAuthenticationAgentException( );
            }
        }

        else
        {
            try
            {
                strTokenId = _openamAPIService.doLogin( strUserName, strUserPassword );

                if ( strTokenId != null )
                {
                    Map<String, String> userInformations = _openamAPIService.getUserInformations( strTokenId, strUserName, _strCookieName,
                            _mapAttributeUserMapping, _strAttributeUserKeyName );

                    if ( ( userInformations != null ) && userInformations.containsKey( _strAttributeUserKeyName ) )
                    {
                        user = new OpenamUser( userInformations.get( _strAttributeUserKeyName ), openamAuthentication, strTokenId );
                        addUserAttributes( userInformations, user );
                    }
                }
            }
            catch( OpenamAPIException ex )
            {
                OpenamAPI._logger.error( "Error During Login Openam {}", ex.getMessage( ) );
            }
        }

        return user;
    }

    /**
     * Logout to openam
     *
     * @param user
     *            the User
     */
    public void doLogout( OpenamUser user )
    {
        try
        {
            _openamAPIService.doDisconnect( _strCookieName, user.getSubjectId( ) );
        }
        catch( OpenamAPIException ex )
        {
            OpenamAPI._logger.error( "Error During Logout Openam {}", ex.getMessage( ) );
        }
    }

    /**
     * Gets the authenticated user
     *
     * @param request
     *            The HTTP request
     * @param openamAuthentication
     *            The Authentication
     * @return The LuteceUser
     */
    public OpenamUser getHttpAuthenticatedUser( HttpServletRequest request, OpenamAuthentication openamAuthentication )
    {
        OpenamUser user = null;
        Map<String, String> headerUserInformations = null;

        if ( isAgentEnabled( ) )
        {
            headerUserInformations = getUserInformationInHeaderRequest( request );
        }

        if ( ( headerUserInformations != null ) && !headerUserInformations.isEmpty( ) && headerUserInformations.containsKey( _strAttributeUserKeyName ) )
        {
            user = new OpenamUser( headerUserInformations.get( _strAttributeUserKeyName ), openamAuthentication, getConnectionCookie( request ) );
            addUserAttributes( headerUserInformations, user );
        }
        else
        {
            String strTokenId = getConnectionCookie( request );

            if ( !StringUtils.isEmpty( strTokenId ) )
            {
                try
                {
                    String strUserId = _openamAPIService.isValidate( strTokenId );

                    if ( strUserId != null )
                    {
                        Map<String, String> userInformations = _openamAPIService.getUserInformations( strTokenId, strUserId, _strCookieName,
                                _mapAttributeUserMapping, _strAttributeUserKeyName );

                        if ( ( userInformations != null ) && userInformations.containsKey( _strAttributeUserKeyName ) )
                        {
                            user = new OpenamUser( userInformations.get( _strAttributeUserKeyName ), openamAuthentication, strTokenId );
                            addUserAttributes( userInformations, user );
                        }
                    }
                }
                catch( OpenamAPIException ex )
                {
                    OpenamAPI._logger.error( "Error getting Openam user Informations {}", ex.getMessage( ) );
                }
            }
        }

        return user;
    }

    /**
     * Extract the value of the connection cookie
     *
     * @param request
     *            The HTTP request
     * @return The cookie's value
     */
    public String getConnectionCookie( HttpServletRequest request )
    {
        Cookie [ ] cookies = request.getCookies( );
        String strOpenamCookie = null;

        if ( cookies != null )
        {
            for ( Cookie cookie : cookies )
            {
                if ( cookie.getName( ).equals( _strCookieName ) )
                {
                    strOpenamCookie = cookie.getValue( );
                    OpenamAPI._logger.debug( "getHttpAuthenticatedUser : cookie '{}' found - value={}", _strCookieName, strOpenamCookie );
                }
            }
        }

        return strOpenamCookie;
    }

    /**
     * true if the token is validated
     * 
     * @param strTokenId
     *            the token id
     * @return true if the token is validated
     */
    public boolean isTokenValidated( String strTokenId )
    {

        if ( !StringUtils.isEmpty( strTokenId ) )
        {
            try
            {
                String strUserId = _openamAPIService.isValidate( strTokenId );
                return !StringUtils.isEmpty( strUserId );

            }
            catch( OpenamAPIException ex )
            {
                OpenamAPI._logger.error( "Error getting Openam user Informations {}", ex.getMessage( ) );
            }
        }

        return false;

    }

    /**
     * set a paris connect cokkie in the HttpServletResponse
     *
     * @param strPCUID
     *            the user PCUID
     * @param response
     *            The HTTP response
     */
    public void setConnectionCookie( String strPCUID, HttpServletResponse response )
    {
        Cookie openamCookie = new Cookie( _strCookieName, strPCUID );
        openamCookie.setDomain( _strCookieDomain );
        openamCookie.setSecure( _bCookieSecure );
        openamCookie.setMaxAge( _nCookieMaxAge );
        openamCookie.setPath( _strCookiePath );

        response.addCookie( openamCookie );
    }

    /**
     * set a paris connect cokkie in the HttpServletResponse
     *
     * @param strPCUID
     *            the user PCUID
     * @param response
     *            The HTTP response
     */
    public void removeConnectionCookie( HttpServletResponse response )
    {
        Cookie openamCookie = new Cookie( _strCookieName, null );
        openamCookie.setDomain( _strCookieDomain );
        openamCookie.setSecure( _bCookieSecure );
        openamCookie.setMaxAge( 0 );
        openamCookie.setPath( _strCookiePath );
        response.addCookie( openamCookie );
    }

    /**
     * Fill user's data
     *
     * @param user
     *            The User
     * @param strUserData
     *            Data in JSON format
     */
    private void addUserAttributes( Map<String, String> userInformations, OpenamUser user )
    {
        for ( Entry<String, String> entry : userInformations.entrySet( ) )
        {
            if ( _mapAttributeUserMapping.containsKey( entry.getKey( ) ) )
            {
                for ( String strUserInfo : _mapAttributeUserMapping.get( entry.getKey( ) ) )
                {
                    user.setUserInfo( strUserInfo, entry.getValue( ) );
                }
            }
        }

        Map<String, String> mapIdentitiesInformations;
        mapIdentitiesInformations = getIdentityInformations( user.getName( ), _mapAttributeUserMapping );
        if ( mapIdentitiesInformations != null )
        {
            user.getUserInfos( ).putAll( mapIdentitiesInformations );
            for ( Entry<String, String> entry : mapIdentitiesInformations.entrySet( ) )
            {
                if ( _mapAttributeUserMapping.containsKey( entry.getKey( ) ) )
                {
                    for ( String strUserInfo : _mapAttributeUserMapping.get( entry.getKey( ) ) )
                    {
                        user.setUserInfo( strUserInfo, entry.getValue( ) );
                    }
                }
            }

            userInformations.putAll( mapIdentitiesInformations );

        }
    }

    /**
     *
     * @return true if the user Agent is enabled
     */
    private boolean isAgentEnabled( )
    {
        return _bAgentEnable;
    }

    /**
     * Reads the user attributes set in the request headers by the OpenAM agent.
     *
     * @param request
     *            The HTTP request
     * @return the user attributes found in the headers
     */
    private Map<String, String> getUserInformationInHeaderRequest( HttpServletRequest request )
    {
        Map<String, String> userInformations = new HashMap<>( );
        Enumeration<String> headerNames = request.getHeaderNames( );

        String strKey;

        while ( headerNames.hasMoreElements( ) )
        {
            strKey = headerNames.nextElement( );

            if ( _mapAttributeUserMapping.containsKey( strKey ) || strKey.equals( _strAttributeUserKeyName ) )
            {
                userInformations.put( strKey, request.getHeader( strKey ) );
            }
        }

        if ( OpenamAPI._bDebug )
        {
            headerNames = request.getHeaderNames( );
            OpenamAPI._logger.debug( "Openam Headers Informations" );

            while ( headerNames.hasMoreElements( ) )
            {
                strKey = headerNames.nextElement( );
                OpenamAPI._logger.debug( "{}={}", strKey, request.getHeader( strKey ) );
            }
        }

        return userInformations;
    }

    /**
     * Gets the user attributes provided by the first identity provider.
     *
     * @param strName
     *            the user name
     * @param attributeUserMapping
     *            the OpenAM attributes mapped to Lutece user attributes
     * @return the user attributes, empty when no identity provider is deployed
     */
    public Map<String, String> getIdentityInformations( String strName, Map<String, List<String>> attributeUserMapping )
    {
        for ( IIdentityProviderService identityProviderService : _identityProviderServices )
        {
            return identityProviderService.getIdentityInformations( strName );
        }

        return new HashMap<>( );
    }
}

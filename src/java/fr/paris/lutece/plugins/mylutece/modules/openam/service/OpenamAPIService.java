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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * OpenamAPIService
 */
@ApplicationScoped
public class OpenamAPIService
{
    public static final String USER_ATTRINUTE_NAME = "userdetails.attribute.name";
    public static final String USER_ATTRINUTE_VALUE = "userdetails.attribute.value";
    public static final String USER_UID = "uid";
    public static final String PCUID = "pcuid";
    public static final String MESSAGE = "message";
    public static final String PARAMETER_SUBJECT_ID = "subjectid";
    public static final String ERROR_TECHNICAL = "TECHNICAL_ERROR";
    private static final String KEY_TOKEN_ID = "tokenId";
    private static final String KEY_VALID = "valid";
    private static final String KEY_UID = "uid";
    private static final String KEY_RESULT = "result";
    private static final String SEPARATOR_COMMA = ",";

    private static final String HEADER_EMAIL = "X-OpenAM-Username";
    private static final String HEADER_PASSWORD = "X-OpenAM-Password";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_CONTENT_TYPE_JSON_VALUE = "application/json";
    private static final ObjectMapper MAPPER = new ObjectMapper( );

    @Inject
    @Named( OpenamAPIProducer.BEAN_API_AUTHENTICATE )
    private OpenamAPI _authenticateAPI;

    @Inject
    @Named( OpenamAPIProducer.BEAN_API_SESSION )
    private OpenamAPI _sessionAPI;

    @Inject
    @Named( OpenamAPIProducer.BEAN_API_USERS )
    private OpenamAPI _usersAPI;

    /**
     * Process login
     * 
     * @param strUserName
     *            The User's name
     * @param strUserPassword
     *            The User's password
     * @return The token id provided by the API, or null
     * @throws OpenamAPIException
     *             if an error occurs
     */
    public String doLogin( String strUserName, String strUserPassword ) throws OpenamAPIException
    {
        Map<String, String> headerParameters = new HashMap<>( );

        headerParameters.put( HEADER_EMAIL, strUserName );
        headerParameters.put( HEADER_PASSWORD, strUserPassword );
        headerParameters.put( HEADER_CONTENT_TYPE, HEADER_CONTENT_TYPE_JSON_VALUE );

        JsonNode jo = parse( _authenticateAPI.callMethod( "", null, headerParameters, true ) );

        return jo.has( KEY_TOKEN_ID ) ? jo.get( KEY_TOKEN_ID ).asText( ) : null;
    }

    /**
     * Checks the connection cookie
     * 
     * @param strSubjectId
     *            The 'account' cookie value
     * @return The UID if the cookie is valid otherwise null
     * @throws OpenamAPIException
     *             if an error occurs
     */
    public String isValidate( String strSubjectId ) throws OpenamAPIException
    {
        Map<String, String> headerParameters = new HashMap<>( );
        headerParameters.put( HEADER_CONTENT_TYPE, HEADER_CONTENT_TYPE_JSON_VALUE );

        JsonNode jo = parse( _sessionAPI.callMethod( strSubjectId + "?_action=validate", null, headerParameters, true ) );

        if ( Boolean.parseBoolean( jo.path( KEY_VALID ).asText( ) ) )
        {
            return jo.path( KEY_UID ).asText( null );
        }

        return null;
    }

    /**
     * Process user logout
     * 
     * @param strCookieName
     *            the OpenAM cookie name
     * @param strSubjectId
     *            the user token id
     * @return The result provided by the API, or null
     * @throws OpenamAPIException
     *             if an error occurs
     */
    public String doDisconnect( String strCookieName, String strSubjectId ) throws OpenamAPIException
    {
        Map<String, String> headerParameters = new HashMap<>( );
        headerParameters.put( strCookieName, strSubjectId );
        headerParameters.put( HEADER_CONTENT_TYPE, HEADER_CONTENT_TYPE_JSON_VALUE );

        JsonNode jo = parse( _sessionAPI.callMethod( "?_action=logout", null, headerParameters, true ) );

        return jo.has( KEY_RESULT ) ? jo.get( KEY_RESULT ).asText( ) : null;
    }

    /**
     * Get user infos
     * 
     * @param strSubjectId
     *            the user token id
     * @param strUserId
     *            The UserID
     * @param strCookieName
     *            the OpenAM cookie name
     * @param mapUserMapping
     *            the OpenAM attributes mapped to Lutece user attributes
     * @param strUserAttributeKey
     *            the OpenAM attribute holding the user name
     * @return the user attributes read from OpenAM
     * @throws OpenamAPIException
     *             if an error occurs
     */
    public Map<String, String> getUserInformations( String strSubjectId, String strUserId, String strCookieName,
            Map<String, List<String>> mapUserMapping, String strUserAttributeKey ) throws OpenamAPIException
    {
        Map<String, String> headerParameters = new HashMap<>( );
        headerParameters.put( strCookieName, strSubjectId );

        Map<String, String> mapInfos = new HashMap<>( );

        String strResponse = _usersAPI.callMethod( strUserId + "?_fields=" + buildRestrictListOfAttributesReturn( strUserAttributeKey, mapUserMapping ), null,
                headerParameters, true, true );
        JsonNode jo = parse( strResponse );

        putValues( jo, strUserAttributeKey, mapInfos );

        if ( mapUserMapping != null )
        {
            for ( Entry<String, List<String>> entry : mapUserMapping.entrySet( ) )
            {
                putValues( jo, entry.getKey( ), mapInfos );
            }
        }

        return mapInfos;
    }

    /**
     * Puts the values of an OpenAM attribute into a map, the last value winning.
     *
     * @param jo
     *            the OpenAM user
     * @param strKey
     *            the attribute name
     * @param mapInfos
     *            the map to fill
     */
    private static void putValues( JsonNode jo, String strKey, Map<String, String> mapInfos )
    {
        if ( jo.has( strKey ) )
        {
            JsonNode node = jo.get( strKey );

            if ( node.isArray( ) )
            {
                for ( JsonNode value : node )
                {
                    mapInfos.put( strKey, value.asText( ) );
                }
            }
            else
            {
                mapInfos.put( strKey, node.asText( ) );
            }
        }
    }

    /**
     * Parses an API response.
     *
     * @param strResponse
     *            the response in JSON format
     * @return the parsed response, a missing node when the response is empty
     * @throws OpenamAPIException
     *             if the response is not JSON
     */
    private static JsonNode parse( String strResponse ) throws OpenamAPIException
    {
        if ( strResponse == null || strResponse.isBlank( ) )
        {
            return MissingNode.getInstance( );
        }

        try
        {
            return MAPPER.readTree( strResponse );
        }
        catch( JsonProcessingException e )
        {
            throw new OpenamAPIException( e.getMessage( ) );
        }
    }

    /**
     * Builds the list of attributes requested from OpenAM.
     *
     * @param strUserAttributeKey
     *            the OpenAM attribute holding the user name
     * @param mapUserMapping
     *            the OpenAM attributes mapped to Lutece user attributes
     * @return the attributes separated by commas
     */
    private static String buildRestrictListOfAttributesReturn( String strUserAttributeKey, Map<String, List<String>> mapUserMapping )
    {
        StringBuilder strFields = new StringBuilder( );
        strFields.append( strUserAttributeKey );

        if ( ( mapUserMapping != null ) && !mapUserMapping.isEmpty( ) )
        {
            for ( Entry<String, List<String>> entry : mapUserMapping.entrySet( ) )
            {
                strFields.append( SEPARATOR_COMMA );
                strFields.append( entry.getKey( ) );
            }
        }

        return strFields.toString( );
    }
}

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
package fr.paris.lutece.plugins.mylutece.modules.openam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.test.ReflectionTestUtils;

/**
 * Tests the reading of the OpenAM REST API responses.
 */
public class OpenamAPIServiceTest extends LuteceTestCase
{
    private final OpenamAPIService _service = new OpenamAPIService( );

    /**
     * An API client answering a fixed response and recording the method called.
     */
    private static final class StubAPI extends OpenamAPI
    {
        private final String _strResponse;
        private String _strMethod;
        private Map<String, String> _mapHeaders;

        /**
         * Builds the stub.
         *
         * @param strResponse
         *            the response to answer
         */
        StubAPI( String strResponse )
        {
            _strResponse = strResponse;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String callMethod( String strMethod, Map<String, String> mapParameters, Map<String, String> mapHeaders, boolean bJSON, boolean bGet )
        {
            _strMethod = strMethod;
            _mapHeaders = mapHeaders;

            return _strResponse;
        }
    }

    /**
     * Replaces the API clients of the service.
     *
     * @param authenticate
     *            the authentication API
     * @param session
     *            the session API
     * @param users
     *            the users API
     */
    private void stub( OpenamAPI authenticate, OpenamAPI session, OpenamAPI users )
    {
        ReflectionTestUtils.setField( _service, "_authenticateAPI", authenticate );
        ReflectionTestUtils.setField( _service, "_sessionAPI", session );
        ReflectionTestUtils.setField( _service, "_usersAPI", users );
    }

    /**
     * The login reads the token id and sends the credentials as headers.
     *
     * @throws OpenamAPIException
     *             if the response cannot be read
     */
    @Test
    public void testDoLogin( ) throws OpenamAPIException
    {
        StubAPI authenticate = new StubAPI( "{\"tokenId\":\"AQIC5w\",\"successUrl\":\"/openam/console\"}" );
        stub( authenticate, null, null );

        assertEquals( "AQIC5w", _service.doLogin( "jdupont", "secret" ) );
        assertEquals( "jdupont", authenticate._mapHeaders.get( "X-OpenAM-Username" ) );
        assertEquals( "secret", authenticate._mapHeaders.get( "X-OpenAM-Password" ) );

        stub( new StubAPI( "{\"code\":401,\"reason\":\"Unauthorized\"}" ), null, null );
        assertNull( _service.doLogin( "jdupont", "wrong" ) );
    }

    /**
     * A valid session gives its uid, an invalid one gives nothing.
     *
     * @throws OpenamAPIException
     *             if the response cannot be read
     */
    @Test
    public void testIsValidate( ) throws OpenamAPIException
    {
        StubAPI session = new StubAPI( "{\"valid\":true,\"uid\":\"jdupont\",\"realm\":\"/\"}" );
        stub( null, session, null );
        assertEquals( "jdupont", _service.isValidate( "AQIC5w" ) );
        assertEquals( "AQIC5w?_action=validate", session._strMethod );

        stub( null, new StubAPI( "{\"valid\":false}" ), null );
        assertNull( _service.isValidate( "expired" ) );
    }

    /**
     * The logout reads the result.
     *
     * @throws OpenamAPIException
     *             if the response cannot be read
     */
    @Test
    public void testDoDisconnect( ) throws OpenamAPIException
    {
        StubAPI session = new StubAPI( "{\"result\":\"Successfully logged out\"}" );
        stub( null, session, null );

        assertEquals( "Successfully logged out", _service.doDisconnect( "iPlanetDirectoryPro", "AQIC5w" ) );
        assertEquals( "AQIC5w", session._mapHeaders.get( "iPlanetDirectoryPro" ) );
    }

    /**
     * The user attributes are read from their arrays, the last value winning, and only the requested ones are kept.
     *
     * @throws OpenamAPIException
     *             if the response cannot be read
     */
    @Test
    public void testGetUserInformations( ) throws OpenamAPIException
    {
        StubAPI users = new StubAPI( "{\"uid\":[\"jdupont\"],\"prenom\":[\"Jean\"],\"nom\":[\"Dupont\",\"Durand\"],\"mail\":[\"x@y\"]}" );
        stub( null, null, users );

        Map<String, List<String>> mapping = new HashMap<>( );
        mapping.put( "prenom", new ArrayList<>( List.of( "user.name.given" ) ) );
        mapping.put( "nom", new ArrayList<>( List.of( "user.name.family" ) ) );

        Map<String, String> infos = _service.getUserInformations( "AQIC5w", "jdupont", "iPlanetDirectoryPro", mapping, "uid" );

        assertEquals( 3, infos.size( ) );
        assertEquals( "jdupont", infos.get( "uid" ) );
        assertEquals( "Jean", infos.get( "prenom" ) );
        assertEquals( "Durand", infos.get( "nom" ) );
        assertEquals( "AQIC5w", users._mapHeaders.get( "iPlanetDirectoryPro" ) );
    }

    /**
     * A response that is not JSON is reported as an API error.
     */
    @Test
    public void testInvalidResponse( )
    {
        stub( new StubAPI( "<html>Bad gateway</html>" ), null, null );

        assertThrows( OpenamAPIException.class, ( ) -> _service.doLogin( "jdupont", "secret" ) );
    }
}

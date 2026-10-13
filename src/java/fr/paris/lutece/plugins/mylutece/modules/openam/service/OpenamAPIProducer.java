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

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

/**
 * Produces the OpenAM REST API clients.
 */
@ApplicationScoped
public class OpenamAPIProducer
{
    public static final String BEAN_API_AUTHENTICATE = "mylutece-openam.apiAuthenticate";
    public static final String BEAN_API_SESSION = "mylutece-openam.apiSession";
    public static final String BEAN_API_USERS = "mylutece-openam.apiUsers";
    private static final String DEFAULT_URL_AUTHENTICATE = "https://openam.example.com:8443/openam/json/authenticate";
    private static final String DEFAULT_URL_SESSION = "https://openam.example.com:8443/openam/json/sessions/";
    private static final String DEFAULT_URL_USERS = "https://openam.example.com:8443/openam/json/users/";

    /**
     * Produces the authentication API client.
     *
     * @param strUrl
     *            the API URL
     * @return the API client
     */
    @Produces
    @ApplicationScoped
    @Named( BEAN_API_AUTHENTICATE )
    public OpenamAPI produceApiAuthenticate( @ConfigProperty( name = "mylutece-openam.apiAuthenticate.url" ) Optional<String> strUrl )
    {
        return buildApi( "authenticate", strUrl.orElse( DEFAULT_URL_AUTHENTICATE ) );
    }

    /**
     * Produces the session API client.
     *
     * @param strUrl
     *            the API URL
     * @return the API client
     */
    @Produces
    @ApplicationScoped
    @Named( BEAN_API_SESSION )
    public OpenamAPI produceApiSession( @ConfigProperty( name = "mylutece-openam.apiSession.url" ) Optional<String> strUrl )
    {
        return buildApi( "session", strUrl.orElse( DEFAULT_URL_SESSION ) );
    }

    /**
     * Produces the users API client.
     *
     * @param strUrl
     *            the API URL
     * @return the API client
     */
    @Produces
    @ApplicationScoped
    @Named( BEAN_API_USERS )
    public OpenamAPI produceApiUsers( @ConfigProperty( name = "mylutece-openam.apiUsers.url" ) Optional<String> strUrl )
    {
        return buildApi( "users", strUrl.orElse( DEFAULT_URL_USERS ) );
    }

    /**
     * Builds an API client.
     *
     * @param strName
     *            the API name
     * @param strUrl
     *            the API URL
     * @return the API client
     */
    private static OpenamAPI buildApi( String strName, String strUrl )
    {
        OpenamAPI api = new OpenamAPI( );
        api.setName( strName );
        api.setUrl( strUrl );

        return api;
    }
}

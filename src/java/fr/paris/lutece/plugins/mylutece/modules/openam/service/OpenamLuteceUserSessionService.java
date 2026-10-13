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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Tracks the HTTP sessions of each OpenAM user, to refresh them when OpenAM notifies a change.
 */
@ApplicationScoped
@Named( OpenamLuteceUserSessionService.BEAN_LUTECE_USER_SESSION_SERVICE )
public class OpenamLuteceUserSessionService implements IOpenamLuteceUserSessionService
{
    public static final String BEAN_LUTECE_USER_SESSION_SERVICE = "mylutece-openam.openamLuteceUserSessionService";
    private final Map<String, OpenamLuteceUserSession> _hashSession = new ConcurrentHashMap<>( );
    private final Map<String, Set<String>> _hashLuteceUserName = new ConcurrentHashMap<>( );

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLuteceUserUpToDate( String strSession )
    {
        OpenamLuteceUserSession session = _hashSession.get( strSession );

        if ( session != null && !session.isUpToDate( ) )
        {
            session.setUpToDate( true );

            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLuteceUserSession( String strLuteceUserName, String strSession )
    {
        _hashLuteceUserName.computeIfAbsent( strLuteceUserName, k -> ConcurrentHashMap.newKeySet( ) ).add( strSession );
        _hashSession.put( strSession, new OpenamLuteceUserSession( strSession, strLuteceUserName, true ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLuteceUserSession( String strSession )
    {
        OpenamLuteceUserSession session = _hashSession.remove( strSession );

        if ( session != null )
        {
            _hashLuteceUserName.computeIfPresent( session.getLuteceUserName( ), ( k, setSession ) -> {
                setSession.remove( strSession );

                return setSession.isEmpty( ) ? null : setSession;
            } );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyLuteceUserUpdating( String strLuteceUserName )
    {
        Set<String> setSession = _hashLuteceUserName.get( strLuteceUserName );

        if ( setSession != null )
        {
            for ( String strSession : setSession )
            {
                OpenamLuteceUserSession session = _hashSession.get( strSession );

                if ( session != null )
                {
                    session.setUpToDate( false );
                }
            }
        }
    }
}

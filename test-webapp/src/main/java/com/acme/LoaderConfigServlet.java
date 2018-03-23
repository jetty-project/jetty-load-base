//
//  ========================================================================
//  Copyright (c) 1995-2017 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package com.acme;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mortbay.jetty.load.generator.listeners.LoadConfig;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Store load config to communicate between loader and probe
 */
@SuppressWarnings( "serial" )
public class LoaderConfigServlet
    extends HttpServlet
{

    private LoadConfig loadConfig;

    /* ------------------------------------------------------------ */
    @Override
    public void init( ServletConfig config )
        throws ServletException
    {
        super.init( config );
    }

    /* ------------------------------------------------------------ */
    @Override
    public void doPost( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        this.loadConfig = new ObjectMapper() //
            .configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false ) //
            .readValue( request.getInputStream(), LoadConfig.class );


    }

    /* ------------------------------------------------------------ */
    @Override
    public void doGet( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        new ObjectMapper() //
            .configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false ) //
            .writeValue( response.getWriter(), this.loadConfig );
    }
}

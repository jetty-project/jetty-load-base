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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServerInfoServlet
    extends HttpServlet
{

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        new ObjectMapper().writeValue(resp.getOutputStream(), new ServerInfo());
    }


    public static class ServerInfo
    {
        @JsonProperty
        private String jettyVersion = System.getProperty("jetty.version");

        @JsonProperty
        private int availableProcessors = Runtime.getRuntime().availableProcessors();

        @JsonProperty
        private long totalMemory = Runtime.getRuntime().totalMemory();

        @JsonProperty
        private String gitHash = System.getProperty("jetty.git.hash");

        @JsonProperty
        private String javaVersion = System.getProperty("java.version");

    }
}

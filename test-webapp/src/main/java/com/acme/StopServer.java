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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** 
 * Dump Servlet Request.
 */
@SuppressWarnings("serial")
public class StopServer
    extends HttpServlet
{
    /* ------------------------------------------------------------ */
    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
    }

    /* ------------------------------------------------------------ */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request, response);
    }

    /* ------------------------------------------------------------ */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (Boolean.parseBoolean(request.getParameter("STOP")))
        {
            ServletOutputStream out = response.getOutputStream();
            out.println("Server stopped");
            out.flush();
            try
            {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }
}

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

public class PostServlet extends HttpServlet
{
    @Override
    protected void doPost( HttpServletRequest req, HttpServletResponse resp )
        throws ServletException, IOException
    {
        char[] charBuffer = new char[128];
        BufferedReader bufferedReader = req.getReader();
        int bytesRead, totalRead = 0;
        while ( ( bytesRead = bufferedReader.read(charBuffer)) != -1 ) {
            // no op we simply read the content and not storing it
            totalRead += bytesRead;
        }
        System.out.println( "totalRead " + totalRead );
    }
}

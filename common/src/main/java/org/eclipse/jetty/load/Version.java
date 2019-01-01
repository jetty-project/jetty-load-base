//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.load;


import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class Version {
    private static Version INSTANCE = new Version();

    public static Version getInstance() {
        return INSTANCE;
    }

    private String versionNumber;
    private String buildNumber;
    private String buildTimestamp;
    private String javaVersion = System.getProperty( "java.version" );

    private Version() {
        try {
            try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    "org/eclipse/jetty/load/build.properties")) {
                Properties buildProperties = new Properties();
                buildProperties.load(inputStream);
                this.versionNumber = buildProperties.getProperty("version");
                this.buildNumber = buildProperties.getProperty("buildNumber");
                this.buildTimestamp = formatTimestamp(buildProperties.getProperty("timestamp"));
            }
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }

    private static String formatTimestamp(String timestamp) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date(Long.valueOf(timestamp)));
        } catch (NumberFormatException e) {
            return "unknown";
        }
    }

    @Override
    public String toString() {
        return String.format("Version{versionNumber=%s, buildNumber=%s, buildTimestamp=%s, javaVersion=%s}",
                versionNumber,
                buildNumber,
                buildTimestamp,
                javaVersion);
    }
}

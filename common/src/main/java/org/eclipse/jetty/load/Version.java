package org.eclipse.jetty.load;


import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class Version
{

    private static final Logger LOG = Log.getLogger( Version.class );

    private String versionNumber, buildNumber, buildTimestamp;


    private static class Holder
    {
        static Version instance = new Version();
    }


    public static Version getInstance()
    {
        return Holder.instance;
    }

    private Version()
    {
        try
        {
            try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "org/eclipse/jetty/load/build.properties" ))
            {
                Properties buildProperties = new Properties();
                buildProperties.load( inputStream );
                this.versionNumber = buildProperties.getProperty( "version" );
                this.buildNumber = buildProperties.getProperty( "buildNumber" );
                this.buildTimestamp = formatTimestamp( buildProperties.getProperty( "timestamp" ) );
            }
        }
        catch ( IOException e )
        {
            LOG.ignore( e );
        }
    }

    private static String formatTimestamp( String timestamp )
    {
        try
        {
            return new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssXXX" ) //
                .format( new Date( Long.valueOf( timestamp ) ) );
        }
        catch ( NumberFormatException e )
        {
            LOG.debug( e );
            return "unknown";
        }
    }

    @Override
    public String toString()
    {
        return "Version{" + "versionNumber='" + versionNumber + '\'' + ", buildNumber='" + buildNumber + '\''
            + ", buildTimestamp='" + buildTimestamp + '\'' + '}';
    }
}

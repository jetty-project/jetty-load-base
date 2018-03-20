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
    private String javaVersion;

    private Version() {
        try {
            try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    "org/eclipse/jetty/load/build.properties")) {
                Properties buildProperties = new Properties();
                buildProperties.load(inputStream);
                this.versionNumber = buildProperties.getProperty("version");
                this.buildNumber = buildProperties.getProperty("buildNumber");
                this.buildTimestamp = formatTimestamp(buildProperties.getProperty("timestamp"));
                this.javaVersion = System.getProperty( "java.version" );
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
        return String.format("Version{versionNumber=%s, buildNumber=%s, buildTimestamp=%s}",
                versionNumber,
                buildNumber,
                buildTimestamp);
    }
}

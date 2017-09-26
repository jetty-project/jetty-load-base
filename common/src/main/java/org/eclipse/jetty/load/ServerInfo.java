package org.eclipse.jetty.load;

public class ServerInfo
{
    private String jettyVersion;

    private int availableProcessors;

    private long totalMemory;

    public String getJettyVersion()
    {
        return jettyVersion;
    }

    public int getAvailableProcessors()
    {
        return availableProcessors;
    }

    public long getTotalMemory()
    {
        return totalMemory;
    }

    public void setJettyVersion( String jettyVersion )
    {
        this.jettyVersion = jettyVersion;
    }

    public void setAvailableProcessors( int availableProcessors )
    {
        this.availableProcessors = availableProcessors;
    }

    public void setTotalMemory( long totalMemory )
    {
        this.totalMemory = totalMemory;
    }
}

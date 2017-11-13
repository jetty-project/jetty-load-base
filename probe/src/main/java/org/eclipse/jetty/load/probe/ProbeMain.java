package org.eclipse.jetty.load.probe;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.load.LiveLatencyDisplayListener;
import org.eclipse.jetty.load.Version;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.listeners.LoadConfig;
import org.mortbay.jetty.load.generator.listeners.LoadResult;
import org.mortbay.jetty.load.generator.listeners.ServerInfo;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarter;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarterArgs;
import org.mortbay.jetty.load.generator.store.ResultStore;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ProbeMain
{
    private static final Logger LOGGER = Log.getLogger( ProbeMain.class );

    public static void main( String[] args )
        throws Exception
    {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        ProbeArgs starterArgs = parse( args );

        ServerInfo serverInfo = ServerInfo.retrieveServerInfo( starterArgs.getScheme(), //
                                                               starterArgs.getHost(), //
                                                               starterArgs.getPort(), //
                                                               "/test/info/" );

        LOGGER.info( "run load test on server:{}", serverInfo );
        LOGGER.info( "Probe version: {}", Version.getInstance() );

        LoadGenerator.Builder builder = LoadGeneratorStarter.prepare( starterArgs );
        LiveLatencyDisplayListener listener = new LiveLatencyDisplayListener().serverInfo( serverInfo )
            .loadConfigType( LoadConfig.Type.PROBE );
        builder = builder.resourceListener( listener ).listener( listener );

        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay( listener, 3, 3, TimeUnit.SECONDS );
        LOGGER.info( "start load generator run" );
        long start = System.currentTimeMillis();
        try {
            LoadGeneratorStarter.run( builder );
        } finally {
            long end = System.currentTimeMillis();
            LOGGER.info( "end load generator run {} seconds", //
                         TimeUnit.SECONDS.convert( end - start, TimeUnit.MILLISECONDS ) );
            task.cancel( false );
            scheduler.shutdown();
        }

        // we can record result
        // this can be moved to a resultStore implementation
        LoadResult loadResult = listener.getLoadResult();
        String comment = starterArgs.getDynamicParams().get( "loadresult.comment" );
        if ( StringUtils.isNotEmpty( comment ) )
        {
            loadResult.setComment( comment );
        }

        StringWriter stringWriter = new StringWriter();
        try
        {
            new ObjectMapper().writeValue( stringWriter, loadResult );
        }
        catch ( IOException e )
        {
            LOGGER.info( "skip cannot write LoadResult as json", e );
        }

        LOGGER.info( "loadResult json: {}", stringWriter.toString() );

        if ( starterArgs.getResultFilePath() != null )
        {
            Path path = Paths.get( starterArgs.getResultFilePath() );
            Files.deleteIfExists( path );
            Files.write( path, stringWriter.toString().getBytes() );
        }

        List<ResultStore> resultStores = ResultStore.getActives( starterArgs.getDynamicParams() );

        resultStores.stream() //
            .forEach( resultStore -> resultStore.initialize( starterArgs.getDynamicParams() ) );

        resultStores.stream() //
            .forEach( resultStore -> resultStore.save( loadResult ) );

        System.exit( 0 );
    }


    public static class ProbeArgs
        extends LoadGeneratorStarterArgs
    {
        @Parameter( names = { "--result-path", "-rp" }, description = "Path to store json result file" )
        private String resultFilePath;

        @DynamicParameter( names = "-D", description = "Dynamic parameters go here" )
        public Map<String, String> dynamicParams = new HashMap<>();

        public String getResultFilePath()
        {
            return resultFilePath;
        }

        public void setResultFilePath( String resultFilePath )
        {
            this.resultFilePath = resultFilePath;
        }

        public Map<String, String> getDynamicParams()
        {
            return dynamicParams;
        }

        public void setDynamicParams( Map<String, String> dynamicParams )
        {
            this.dynamicParams = dynamicParams;
        }
    }

    public static ProbeArgs parse( String[] args )
    {
        ProbeArgs starterArgs = new ProbeArgs();
        JCommander jCommander = new JCommander( starterArgs );
        jCommander.setAcceptUnknownOptions( true );
        jCommander.parse( args );
        if ( starterArgs.isHelp() )
        {
            jCommander.usage();
            return null;
        }
        return starterArgs;
    }
}

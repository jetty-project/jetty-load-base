package org.eclipse.jetty.load.probe;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.load.LiveLatencyDisplayListener;
import org.eclipse.jetty.load.LoadResult;
import org.eclipse.jetty.load.ServerInfo;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarter;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarterArgs;

public class ProbeMain {
    private static final Logger LOGGER = Log.getLogger( ProbeMain.class);
    public static void main(String[] args) throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        ProbeArgs starterArgs = parse(args);

        ServerInfo serverInfo = ServerInfo.retrieveServerInfo( starterArgs.getScheme(), //
                                                               starterArgs.getHost(), //
                                                               starterArgs.getPort(), //
                                                               "/test/info/" );

        LOGGER.info( "run load test on server:{}", serverInfo );

        LoadGenerator.Builder builder = LoadGeneratorStarter.prepare(starterArgs);
        LiveLatencyDisplayListener listener = new LiveLatencyDisplayListener().serverInfo( serverInfo );
        builder = builder.resourceListener(listener).listener(listener);

        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(listener, 3, 3, TimeUnit.SECONDS);
        LOGGER.info( "start load generator run" );
        long start = System.currentTimeMillis();
        try {
            LoadGeneratorStarter.run(builder);
        } finally {
            long end = System.currentTimeMillis();
            LOGGER.info( "end load generator run {} seconds", //
                         TimeUnit.SECONDS.convert( end - start, TimeUnit.MILLISECONDS ) );
            task.cancel(false);
            scheduler.shutdown();
        }

        // we can record result
        LoadResult loadResult = listener.getLoadResult();
        StringWriter stringWriter = new StringWriter();
        try
        {
            new ObjectMapper().writeValue( stringWriter, loadResult );
        } catch ( IOException e ) {
            LOGGER.info( "skip cannot write LoadResult as json", e );
        }

        LOGGER.info( "loadResult json: {}", stringWriter.toString() );

        if(starterArgs.getResultFilePath()!=null) {
            Path path = Paths.get( starterArgs.getResultFilePath() );
            Files.deleteIfExists( path );
            Files.write( path, stringWriter.toString().getBytes() );
        }

    }


    public static class ProbeArgs extends LoadGeneratorStarterArgs {
        @Parameter(names = {"--result-path", "-rp"}, description = "Path to store json result file")
        private String resultFilePath;

        public String getResultFilePath() {
            return resultFilePath;
        }

        public void setResultFilePath( String resultFilePath ) {
            this.resultFilePath = resultFilePath;
        }
    }

    public static ProbeArgs parse(String[] args) {
        ProbeArgs starterArgs = new ProbeArgs();
        JCommander jCommander = new JCommander( starterArgs);
        jCommander.setAcceptUnknownOptions(true);
        jCommander.parse(args);
        if (starterArgs.isHelp()) {
            jCommander.usage();
            return null;
        }
        return starterArgs;
    }
}

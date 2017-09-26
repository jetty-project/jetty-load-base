package org.eclipse.jetty.load.loader;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.load.ServerInfo;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarter;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarterArgs;

public class LoaderMain {
    private static final Logger LOGGER = Log.getLogger( LoaderMain.class);
    public static void main(String[] args) throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        LoadGeneratorStarterArgs starterArgs = LoadGeneratorStarter.parse(args);
        LoadGenerator.Builder builder = LoadGeneratorStarter.prepare(starterArgs);

        ServerInfo serverInfo = ServerInfo.retrieveServerInfo( starterArgs.getScheme(), //
                                                               starterArgs.getHost(), //
                                                               starterArgs.getPort(), //
                                                               "/test/info/" );

        LOGGER.info( "run load test on server:{}", serverInfo );

        LiveLoadDisplayListener listener = new LiveLoadDisplayListener();
        builder = builder.requestListener(listener).listener( listener );

        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(listener, 1, 2, TimeUnit.SECONDS);
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
        LOGGER.info( "load generator done" );
    }
}

package org.eclipse.jetty.load.probe;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.load.LiveLatencyDisplayListener;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarter;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarterArgs;

public class ProbeMain {
    private static final Logger LOGGER = Log.getLogger( ProbeMain.class);
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        LoadGeneratorStarterArgs starterArgs = LoadGeneratorStarter.parse(args);
        LoadGenerator.Builder builder = LoadGeneratorStarter.prepare(starterArgs);
        LiveLatencyDisplayListener listener = new LiveLatencyDisplayListener();
        builder = builder.resourceListener(listener).listener(listener);

        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(listener, 1, 1, TimeUnit.SECONDS);
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
    }
}

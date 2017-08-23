package org.eclipse.jetty.load.loader;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarter;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarterArgs;

public class LoaderMain {
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        LoadGeneratorStarterArgs starterArgs = LoadGeneratorStarter.parse(args);
        LoadGenerator.Builder builder = LoadGeneratorStarter.prepare(starterArgs);

        LiveLoadDisplayListener listener = new LiveLoadDisplayListener();
        builder = builder.requestListener(listener);

        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(listener, 1, 2, TimeUnit.SECONDS);
        try {
            LoadGeneratorStarter.run(builder);
        } finally {
            task.cancel(false);
            scheduler.shutdown();
        }
    }
}

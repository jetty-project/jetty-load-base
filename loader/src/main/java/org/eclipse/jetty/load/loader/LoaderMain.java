package org.eclipse.jetty.load.loader;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.Parameter;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.load.Version;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.listeners.ServerInfo;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarter;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarterArgs;

public class LoaderMain {
    private static final Logger LOGGER = Log.getLogger(LoaderMain.class);

    public static void main(String[] args) throws Exception {
        MBeanContainer mbeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());

        LoaderArgs loaderArgs = LoadGeneratorStarter.parse(args, LoaderArgs::new);
        LoadGenerator.Builder builder = LoadGeneratorStarter.prepare(loaderArgs);

        QueuedThreadPool executor = null;
        if (loaderArgs.sharedThreads > 0) {
            executor = new QueuedThreadPool(loaderArgs.sharedThreads);
            executor.setName("loader");
            executor.start();
            builder.executor(executor);
            mbeanContainer.beanAdded(null, executor);
        }

        Scheduler scheduler = new ScheduledExecutorScheduler("loader-scheduler", false);
        scheduler.start();
        builder.scheduler(scheduler);
        mbeanContainer.beanAdded(null, scheduler);

        try {
            ServerInfo serverInfo = ServerInfo.retrieveServerInfo(loaderArgs.getScheme(),
                    loaderArgs.getHost(),
                    loaderArgs.getPort(),
                    "/test/info/");

            LOGGER.info("run load test on server: {}", serverInfo);
            LOGGER.info("loader version: {}", Version.getInstance());

            LiveLoadDisplayListener listener = new LiveLoadDisplayListener();
            builder = builder.listener(listener).resourceListener(listener).requestListener(listener);

            // Print loader activity periodically.
            schedule(scheduler, new Runnable() {
                @Override
                public void run() {
                    listener.run();
                    schedule(scheduler, this);
                }
            });

            LoadGenerator loadGenerator = builder.build();
            loadGenerator.addBean(mbeanContainer);

            LOGGER.info("start load generator run");
            long start = System.nanoTime();
            LoadGeneratorStarter.run(loadGenerator);
            long elapsed = System.nanoTime() - start;
            LOGGER.info("end load generator run {} seconds", TimeUnit.NANOSECONDS.toSeconds(elapsed));
        } finally {
            scheduler.stop();
            if (executor != null) {
                executor.stop();
            }
        }
    }

    private static void schedule(Scheduler scheduler, Runnable task) {
        scheduler.schedule(task, 2, TimeUnit.SECONDS);
    }

    private static class LoaderArgs extends LoadGeneratorStarterArgs {
        @Parameter(
                names = {"--shared-threads", "-st"},
                description = "Max threads of the shared thread pool"
        )
        private int sharedThreads;
    }
}

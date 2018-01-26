package org.eclipse.jetty.load.probe;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.load.MonitoredQueuedThreadPool;
import org.eclipse.jetty.load.Version;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.listeners.LoadConfig;
import org.mortbay.jetty.load.generator.listeners.LoadResult;
import org.mortbay.jetty.load.generator.listeners.ServerInfo;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarter;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarterArgs;
import org.mortbay.jetty.load.generator.store.ResultStore;

public class ProbeMain {
    private static final Logger LOGGER = Log.getLogger(ProbeMain.class);

    public static void main(String[] args) throws Exception {
        ProbeArgs probeArgs = LoadGeneratorStarter.parse(args, ProbeArgs::new);
        LoadGenerator.Builder builder = LoadGeneratorStarter.prepare(probeArgs);

        QueuedThreadPool executor = null;
        if (probeArgs.sharedThreads > 0) {
            executor = new MonitoredQueuedThreadPool(probeArgs.sharedThreads);
            executor.setName("loader");
            executor.start();
            builder.executor(executor);
        }

        Scheduler scheduler = new ScheduledExecutorScheduler();
        scheduler.start();
        builder.scheduler(scheduler);

        HttpClient httpClient = new HttpClient();
        httpClient.start();

        try {
            ServerInfo serverInfo = ServerInfo.retrieveServerInfo(probeArgs.getScheme(),
                    probeArgs.getHost(),
                    probeArgs.getPort(),
                    "/test/info/");

            LOGGER.info("run load test on server:{}", serverInfo);
            LOGGER.info("Probe version: {}", Version.getInstance());

            LiveProbeDisplayListener listener = new LiveProbeDisplayListener().serverInfo(serverInfo)
                    .loadConfigType(LoadConfig.Type.PROBE);
            builder = builder.listener(listener).resourceListener(listener).requestListener(listener);

            // Print probe activity periodically.
            schedule(scheduler, new Runnable() {
                @Override
                public void run() {
                    listener.run();
                    schedule(scheduler, this);
                }
            });

            httpClient.newRequest(probeArgs.getHost(), probeArgs.getPort())
                    .scheme(probeArgs.getScheme())
                    .path("/stats/start")
                    .send();

            LOGGER.info("start load generator run");
            long start = System.nanoTime();
            LoadGeneratorStarter.run(builder);
            long end = System.nanoTime();
            LOGGER.info("end load generator run {} seconds", TimeUnit.NANOSECONDS.toSeconds(end - start));

            httpClient.newRequest(probeArgs.getHost(), probeArgs.getPort())
                    .scheme(probeArgs.getScheme())
                    .path("/stats/stop")
                    .send();

            LoadResult loadResult = listener.getLoadResult();
            String comment = probeArgs.dynamicParams.get("loadresult.comment");
            if (StringUtils.isNotEmpty(comment)) {
                loadResult.setComment(comment);
            }

            StringWriter stringWriter = new StringWriter();
            new ObjectMapper().writeValue(stringWriter, loadResult);
            String jsonLoadResult = stringWriter.toString();
            LOGGER.info("loadResult json: {}", jsonLoadResult);

            String resultFilePath = probeArgs.resultFilePath;
            if (resultFilePath != null) {
                Path path = Paths.get(resultFilePath);
                Files.deleteIfExists(path);
                Files.write(path, jsonLoadResult.getBytes(StandardCharsets.UTF_8));
            }

            List<ResultStore> resultStores = ResultStore.getActives(probeArgs.dynamicParams);
            resultStores.forEach(resultStore -> resultStore.initialize(probeArgs.dynamicParams));
            resultStores.forEach(resultStore -> resultStore.save(loadResult));
        } finally {
            if (executor instanceof MonitoredQueuedThreadPool) {
                printThreadPoolStats((MonitoredQueuedThreadPool)executor);
            }
            httpClient.stop();
            scheduler.stop();
        }
    }

    private static void schedule(Scheduler scheduler, Runnable task) {
        scheduler.schedule(task, 2, TimeUnit.SECONDS);
    }

    private static void printThreadPoolStats(MonitoredQueuedThreadPool threadPool) {
        LOGGER.info("thread pool - tasks = {} | concurrent threads max = {} | queue size max = {} | queue latency avg/max = {}/{} ms | task time avg/max = {}/{} ms",
                threadPool.getTasks(),
                threadPool.getMaxActiveThreads(),
                threadPool.getMaxQueueSize(),
                TimeUnit.NANOSECONDS.toMillis(threadPool.getAverageQueueLatency()),
                TimeUnit.NANOSECONDS.toMillis(threadPool.getMaxQueueLatency()),
                TimeUnit.NANOSECONDS.toMillis(threadPool.getAverageTaskLatency()),
                TimeUnit.NANOSECONDS.toMillis(threadPool.getMaxTaskLatency()));
    }

    private static class ProbeArgs extends LoadGeneratorStarterArgs {
        @Parameter(names = {"--shared-threads", "-st"}, description = "Max threads of the shared thread pool")
        private int sharedThreads;

        @Parameter(names = {"--result-path", "-rp"}, description = "Path to store json result file")
        private String resultFilePath;

        @DynamicParameter(names = "-D", description = "Dynamic parameters go here")
        private Map<String, String> dynamicParams = new HashMap<>();
    }
}

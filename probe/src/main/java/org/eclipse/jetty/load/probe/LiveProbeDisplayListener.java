package org.eclipse.jetty.load.probe;

import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.load.Monitor;
import org.eclipse.jetty.toolchain.perf.HistogramSnapshot;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.Resource;
import org.mortbay.jetty.load.generator.listeners.CollectorInformations;
import org.mortbay.jetty.load.generator.listeners.LoadConfig;
import org.mortbay.jetty.load.generator.listeners.LoadResult;
import org.mortbay.jetty.load.generator.listeners.ServerInfo;

public class LiveProbeDisplayListener extends Request.Listener.Adapter implements Resource.NodeListener, LoadGenerator.BeginListener, LoadGenerator.EndListener, Runnable {
    private static final Logger LOGGER = Log.getLogger(LiveProbeDisplayListener.class);

    private final Recorder recorder;
    private final Histogram histogram;
    private Histogram interval;
    private Monitor.Start begin;
    private Monitor.Start start;
    private ServerInfo serverInfo;
    private LoadResult loadResult;
    private LoadConfig.Type loadConfigType;

    public LiveProbeDisplayListener() {
        this(TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.SECONDS.toNanos(60), 3);
    }

    public LiveProbeDisplayListener(long low, long high, int digits) {
        histogram = new Histogram(low, high, digits);
        recorder = new Recorder(low, high, digits);
        start = Monitor.start();
    }

    public LiveProbeDisplayListener serverInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
        return this;
    }

    public LiveProbeDisplayListener loadConfigType(LoadConfig.Type loadConfigType) {
        this.loadConfigType = loadConfigType;
        return this;
    }

    public void onResourceNode(Resource.Info info) {
        long responseTime = info.getResponseTime() - info.getRequestTime();
        recorder.recordValue(responseTime);
    }

    public void run() {
        Monitor.Stop stop = start.stop();

        interval = recorder.getIntervalHistogram(interval);
        histogram.add(interval);

        long elapsed = interval.getEndTimeStamp() - interval.getStartTimeStamp();
        long responseRate = elapsed > 0 ? interval.getTotalCount() * 1000 / elapsed : -1;

        LOGGER.info(String.format("response rate=%s, cpu=%.2f%%, jit=%d ms, time min/mdn/max=%d/%d/%d \u00B5s",
                responseRate,
                stop.cpuPercent,
                stop.deltaJITTime,
                TimeUnit.NANOSECONDS.toMicros(interval.getMinValue()),
                TimeUnit.NANOSECONDS.toMicros(interval.getValueAtPercentile(50)),
                TimeUnit.NANOSECONDS.toMicros(interval.getMaxValue())));

        this.start = Monitor.start();
    }

    @Override
    public void onBegin(LoadGenerator loadGenerator) {
        begin = Monitor.start();
    }

    @Override
    public void onEnd(LoadGenerator loadGenerator) {
        Monitor.Stop end = begin.stop();
        long elapsed = histogram.getEndTimeStamp() - histogram.getStartTimeStamp();
        long rate = elapsed > 0 ? histogram.getTotalCount() * 1000 / elapsed : -1;
        LOGGER.info(String.format("rate=%d, cpu=%.2f%%, jit=%d ms", rate, end.cpuPercent, end.deltaJITTime));
        HistogramSnapshot snapshot = new HistogramSnapshot(histogram, 20, "response time",
                "\u00B5s", TimeUnit.NANOSECONDS::toMicros);
        System.err.println(snapshot);

        CollectorInformations collectorInformations = new CollectorInformations(histogram);
        loadResult = new LoadResult(serverInfo, collectorInformations,
                new LoadConfig(loadGenerator.getConfig()).type(loadConfigType));
    }

    public LoadResult getLoadResult() {
        return loadResult;
    }
}

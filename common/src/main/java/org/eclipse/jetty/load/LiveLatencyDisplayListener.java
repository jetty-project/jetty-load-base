package org.eclipse.jetty.load;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.eclipse.jetty.toolchain.perf.HistogramSnapshot;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.Resource;

import java.util.concurrent.TimeUnit;

public class LiveLatencyDisplayListener
    implements Resource.NodeListener, LoadGenerator.BeginListener, LoadGenerator.EndListener, Runnable {
    private final Recorder recorder;
    private final Histogram histogram;
    private Histogram interval;
    private Monitor.Start begin;
    private Monitor.Start start;

    public LiveLatencyDisplayListener() {
        this(TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.SECONDS.toNanos(60), 3);
    }

    public LiveLatencyDisplayListener( long low, long high, int digits) {
        histogram = new Histogram(low, high, digits);
        recorder = new Recorder(low, high, digits);
        start = Monitor.start();
    }

    public void onResourceNode(Resource.Info info) {
        long responseTime = info.getResponseTime() - info.getRequestTime();
        recorder.recordValue(responseTime);
    }

    public void run() {
        Monitor.Stop stop = start.stop();

        interval = recorder.getIntervalHistogram(interval);
        histogram.add(interval);

        System.err.printf("response time: min/max=%d/%d \u00B5s, jit=%d ms, cpu=%.2f%%%n",
                TimeUnit.NANOSECONDS.toMicros(interval.getMinValue()),
                TimeUnit.NANOSECONDS.toMicros(interval.getMaxValue()),
                stop.deltaJITTime,
                stop.cpuPercent);

        start = Monitor.start();
    }

    @Override
    public void onBegin(LoadGenerator loadGenerator) {
        begin = Monitor.start();
    }

    @Override
    public void onEnd(LoadGenerator loadGenerator) {
        Monitor.Stop end = begin.stop();
        System.err.printf("jit=%d ms, cpu=%.4f ms%n", end.deltaJITTime, end.cpuPercent);
        HistogramSnapshot snapshot = new HistogramSnapshot(histogram, 20, "response time", "\u00B5s", TimeUnit.NANOSECONDS::toMicros);
        System.err.println(snapshot);
    }
}

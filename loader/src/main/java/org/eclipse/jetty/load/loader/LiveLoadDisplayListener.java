package org.eclipse.jetty.load.loader;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.load.Monitor;

public class LiveLoadDisplayListener extends Request.Listener.Adapter implements Runnable {
    private final AtomicInteger requestQueue = new AtomicInteger();
    private Monitor.Start monitor = Monitor.start();
    private final Recorder recorder;
    private final Histogram histogram;
    private Histogram interval;

    public LiveLoadDisplayListener() {
        this( TimeUnit.MICROSECONDS.toNanos( 1), TimeUnit.SECONDS.toNanos( 60), 3);
    }

    public LiveLoadDisplayListener( long low, long high, int digits) {
        histogram = new Histogram(low, high, digits);
        recorder = new Recorder(low, high, digits);
    }

    @Override
    public void onCommit( Request request ) {
        // we only care about total count and start/end so just record 1 :-)
        this.recorder.recordValue( 1 );
    }

    @Override
    public void onQueued(Request request) {
        requestQueue.incrementAndGet();
    }

    @Override
    public void onBegin(Request request) {
        requestQueue.decrementAndGet();
    }

    public void run() {
        Monitor.Stop stop = monitor.stop();

        interval = recorder.getIntervalHistogram(interval);
        histogram.add(interval);

        long totalRequestCommitted = interval.getTotalCount();
        long start = interval.getStartTimeStamp();
        long end = interval.getEndTimeStamp();
        long timeInSeconds = TimeUnit.SECONDS.convert( end - start, TimeUnit.MILLISECONDS );
        long qps = totalRequestCommitted / timeInSeconds;

        System.err.printf("request queue: %d, jit=%s ms, qps=%s, committed=%s, cpu=%.2f%%%n",
                requestQueue.get(),
                stop.deltaJITTime,
                qps,
                totalRequestCommitted,
                stop.cpuPercent);

        monitor = Monitor.start();
    }
}

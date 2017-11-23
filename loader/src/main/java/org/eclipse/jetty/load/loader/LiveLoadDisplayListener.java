package org.eclipse.jetty.load.loader;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.load.Monitor;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.mortbay.jetty.load.generator.LoadGenerator;

public class LiveLoadDisplayListener extends Request.Listener.Adapter implements Runnable, LoadGenerator.EndListener, LoadGenerator.BeginListener {
    private static final Logger LOGGER = Log.getLogger( LiveLoadDisplayListener.class);
    private final LongAdder requestQueue = new LongAdder();
    private Monitor.Start monitor = Monitor.start();
    private LongAdder intervalAdder = new LongAdder(), totalAdder = new LongAdder();
    private long start, intervalStart;


    public LiveLoadDisplayListener() {
        this( TimeUnit.MICROSECONDS.toNanos( 1), TimeUnit.SECONDS.toNanos( 60), 3);
    }

    public LiveLoadDisplayListener( long low, long high, int digits) {
        //
    }

    @Override
    public void onCommit( Request request ) {
        // we only care about total count and start/end so just record 1 :-)
        //this.recorder.recordValue( 1 );
        intervalAdder.increment();
    }

    @Override
    public void onQueued(Request request) {
        requestQueue.increment();
    }

    @Override
    public void onBegin(Request request) {
        requestQueue.decrement();
    }

    public void run() {
        try
        {
            Monitor.Stop stop = monitor.stop();

            long totalRequestCommitted = intervalAdder.longValue();
            long start = intervalStart;
            intervalStart = System.currentTimeMillis();
            long end = System.currentTimeMillis();
            long timeInSeconds = TimeUnit.SECONDS.convert( end - start, TimeUnit.MILLISECONDS );
            if (timeInSeconds <= 0)
            {
                LOGGER.debug( "O s so no qps" );
                return;
            }
            long qps = totalRequestCommitted / timeInSeconds;
            intervalAdder.reset();
            LOGGER.info( String.format("request queue: %d, jit=%s ms, qps=%s, committed=%s, cpu=%.2f%%%n",
                    requestQueue.intValue(),
                    stop.deltaJITTime,
                    qps,
                    totalRequestCommitted,
                    stop.cpuPercent));

            monitor = Monitor.start();
            totalAdder.add( totalRequestCommitted );
        }
        catch ( Exception e )
        {
            LOGGER.warn( e );
        }
    }

    @Override
    public void onBegin( LoadGenerator generator ) {
        start = System.currentTimeMillis();
        intervalStart = System.currentTimeMillis();
    }

    @Override
    public void onEnd( LoadGenerator loadGenerator ) {
        long totalRequestCommitted = totalAdder.longValue();
        long end = System.currentTimeMillis();
        long timeInSeconds = TimeUnit.SECONDS.convert( end - start, TimeUnit.MILLISECONDS );
        long qps = totalRequestCommitted / timeInSeconds;
        LOGGER.info( "Average qps: {}", qps);
    }
}

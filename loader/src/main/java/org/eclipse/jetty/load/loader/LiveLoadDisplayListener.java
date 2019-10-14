//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.load.loader;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.load.Monitor;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.Resource;

public class LiveLoadDisplayListener extends Request.Listener.Adapter implements Runnable, LoadGenerator.EndListener, Resource.NodeListener
{
    private static final Logger LOGGER = Log.getLogger(LiveLoadDisplayListener.class);
    private final LongAdder requestQueue = new LongAdder();
    private Monitor.Start monitor = Monitor.start();
    private final Recorder recorder;
    private final Histogram histogram;
    private Histogram interval;

    public LiveLoadDisplayListener()
    {
        this(TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.SECONDS.toNanos(300), 3);
    }

    public LiveLoadDisplayListener(long low, long high, int digits)
    {
        histogram = new Histogram(low, high, digits);
        recorder = new Recorder(low, high, digits);
    }

    @Override
    public void onQueued(Request request)
    {
        requestQueue.increment();
    }

    @Override
    public void onBegin(Request request)
    {
        requestQueue.decrement();
    }

    @Override
    public void onResourceNode(Resource.Info info)
    {
        long responseTime = info.getResponseTime() - info.getRequestTime();
        try
        {
            recorder.recordValue(responseTime);
        }
        catch (IndexOutOfBoundsException e)
        {
           LOGGER.info("ignore error storing response time {}", responseTime);
        }
    }

    public void run()
    {
        try
        {
            Monitor.Stop stop = monitor.stop();

            interval = recorder.getIntervalHistogram(interval);
            histogram.add(interval);

            long elapsed = interval.getEndTimeStamp() - interval.getStartTimeStamp();
            long rate = elapsed > 0 ? interval.getTotalCount() * 1000 / elapsed : -1;

            LOGGER.info(String.format("request queue: %d, rate=%d, cpu=%.2f%%, jit=%d ms, response min/mdn/max=%d/%d/%d \u00B5s",
                    requestQueue.longValue(),
                    rate,
                    stop.cpuPercent,
                    stop.deltaJITTime,
                    TimeUnit.NANOSECONDS.toMicros(interval.getMinValue()),
                    TimeUnit.NANOSECONDS.toMicros(interval.getValueAtPercentile(50)),
                    TimeUnit.NANOSECONDS.toMicros(interval.getMaxValue())));

            monitor = Monitor.start();
        }
        catch (Exception x)
        {
            LOGGER.warn(x);
        }
    }

    @Override
    public void onEnd(LoadGenerator loadGenerator)
    {
        long requests = histogram.getTotalCount();
        long time = TimeUnit.MILLISECONDS.toSeconds(histogram.getEndTimeStamp() - histogram.getStartTimeStamp());
        long rate = time > 0 ? requests / time : -1;
        LOGGER.info("average request rate: {}", rate);
    }
}

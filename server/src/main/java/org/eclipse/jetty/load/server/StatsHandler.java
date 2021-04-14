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

package org.eclipse.jetty.load.server;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;
import org.eclipse.jetty.load.Monitor;
import org.eclipse.jetty.load.MonitoredQueuedThreadPool;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.toolchain.perf.HistogramSnapshot;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.ThreadPool;

public class StatsHandler extends AbstractHandler {
    private static final Logger LOGGER = Log.getLogger(StatsHandler.class.getName());

    private final AtomicInteger stats = new AtomicInteger();
    private final StatsListener statsListener = new StatsListener();
    private Monitor.Start start;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        Arrays.stream(getServer().getConnectors()).forEach(c -> c.addBean(statsListener));
    }

//    @Override
//    protected void doNonErrorHandle(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) {
//        jettyRequest.setHandled(true);
//        service(jettyRequest);
//    }

    @Override
    public void handle( String s, Request request, HttpServletRequest httpServletRequest,
                        HttpServletResponse httpServletResponse )
        throws IOException, ServletException
    {
        service(request);
        request.setHandled(true);
    }

    private void service( Request request) {
        ThreadPool threadPool = getServer().getThreadPool();
        String uri = request.getRequestURI();
        if (uri.endsWith("/start")) {
            Monitor.Start start = startStats();
            if (start != null) {
                this.start = start;
                if (threadPool instanceof MonitoredQueuedThreadPool) {
                    ((MonitoredQueuedThreadPool)threadPool).reset();
                }
                statsListener.reset();
                printStart(start);
                System.gc();
                statsListener.enabled = true;
            }
        } else if (uri.endsWith("/stop")) {
            Monitor.Stop stop = stopStats();
            if (stop != null) {
                statsListener.enabled = false;
                start = null;
                printStop(stop);
                if (threadPool instanceof MonitoredQueuedThreadPool) {
                    printThreadPoolStats((MonitoredQueuedThreadPool)threadPool);
                }
                printProcessingStats();
            }
        }
    }

    private Monitor.Start startStats() {
        return stats.getAndIncrement() == 0 ? Monitor.start() : null;
    }

    private void printStart(Monitor.Start start) {
        LOGGER.info("========================================");
        LOGGER.info("monitoring started at: {}", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(start.date));
        LOGGER.info("operative system: {}", start.os);
        LOGGER.info("jvm: {}", start.jvm);
        LOGGER.info("cores: {}", start.cores);
        LOGGER.info("heap: {}/{}", gibi(start.heap.getUsed()), gibi(start.heap.getMax()));
        LOGGER.info("- - - - - - - - - - - - - - - - - - - - ");
    }

    private Monitor.Stop stopStats() {
        return stats.decrementAndGet() == 0 ? start.stop() : null;
    }

    private void printStop(Monitor.Stop stop) {
        LOGGER.info("- - - - - - - - - - - - - - - - - - - - ");
        LOGGER.info("monitoring ended at: {}", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(stop.date));
        LOGGER.info("elapsed Time: {} ms", TimeUnit.NANOSECONDS.toMillis(stop.deltaTime));
        LOGGER.info("time in jit compilation: {} ms", stop.deltaJITTime);
        LOGGER.info("average cpu load: {}/{}", percent(stop.deltaCPUTime, stop.deltaTime), 100 * stop.cores);
        LOGGER.info("========================================");
    }

    private void printThreadPoolStats(MonitoredQueuedThreadPool threadPool) {
        LOGGER.info("thread pool - tasks = {} | concurrent threads max = {} | queue size max = {} | queue latency avg/max = {}/{} ms | task time avg/max = {}/{} ms",
                threadPool.getTasks(),
                threadPool.getMaxActiveThreads(),
                threadPool.getMaxQueueSize(),
                TimeUnit.NANOSECONDS.toMillis(threadPool.getAverageQueueLatency()),
                TimeUnit.NANOSECONDS.toMillis(threadPool.getMaxQueueLatency()),
                TimeUnit.NANOSECONDS.toMillis(threadPool.getAverageTaskLatency()),
                TimeUnit.NANOSECONDS.toMillis(threadPool.getMaxTaskLatency()));
    }

    private void printProcessingStats() {
        LOGGER.info("request processing times:{}{}", System.lineSeparator(), new HistogramSnapshot(statsListener.histogram.copy(), 20, "requests", "\u00B5s", TimeUnit.NANOSECONDS::toMicros));
    }

    private float gibi(long bytes) {
        return (float)bytes / 1024 / 1024 / 1024;
    }

    private float percent(long dividend, long divisor) {
        if (divisor != 0) {
            return (float)dividend * 100 / divisor;
        }
        return Float.NaN;
    }

    private static class StatsListener implements HttpChannel.Listener {
        private final ConcurrentMap<Request, Long> stats = new ConcurrentHashMap<>();
        private final Histogram histogram = new ConcurrentHistogram(TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.SECONDS.toNanos(60), 3);
        private boolean enabled;

        private void reset() {
            stats.clear();
            histogram.reset();
            enabled = false;
        }

        @Override
        public void onRequestBegin(Request request) {
            if (enabled) {
                stats.put(request, System.nanoTime());
            }
        }

        @Override
        public void onComplete(Request request) {
            if (enabled) {
                Long start = stats.remove(request);
                if (start != null) {
                    histogram.recordValue(System.nanoTime() - start);
                }
            }
        }
    }
}

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

package org.eclipse.jetty.load.probe;

import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.load.Monitor;
import org.eclipse.jetty.toolchain.perf.HistogramSnapshot;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.Resource;
import org.mortbay.jetty.load.generator.listeners.CollectorInformations;
import org.mortbay.jetty.load.generator.listeners.LoadConfig;
import org.mortbay.jetty.load.generator.listeners.LoadResult;
import org.mortbay.jetty.load.generator.listeners.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveProbeDisplayListener extends Request.Listener.Adapter implements Resource.NodeListener, LoadGenerator.BeginListener, LoadGenerator.EndListener, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveProbeDisplayListener.class);

    private final Recorder recorder;
    private final Histogram histogram;
    private Histogram interval;
    private Monitor.Start begin;
    private Monitor.Start start;
    private ServerInfo serverInfo;
    private LoadResult loadResult;
    private LoadConfig.Type loadConfigType;
    private String transport;
    private long created = System.currentTimeMillis();

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

    public LiveProbeDisplayListener transport(String transport) {
        this.transport = transport;
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

        LOGGER.info(String.format("response rate=%s, cpu=%.2f%%, jit=%d ms, time min/mdn/max=%d/%d/%d \u00B5s, started= %d s",
                responseRate,
                stop.cpuPercent,
                stop.deltaJITTime,
                TimeUnit.NANOSECONDS.toMicros(interval.getMinValue()),
                TimeUnit.NANOSECONDS.toMicros(interval.getValueAtPercentile(50)),
                TimeUnit.NANOSECONDS.toMicros(interval.getMaxValue()),
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-created)));

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
        HistogramSnapshot snapshot = new HistogramSnapshot(histogram.copy(), 20, "response time",
                "\u00B5s", TimeUnit.NANOSECONDS::toMicros);
        System.err.println(snapshot);

        CollectorInformations collectorInformations = new CollectorInformations(histogram.copy());
        loadResult = new LoadResult(serverInfo, collectorInformations,
                new LoadConfig(loadGenerator.getConfig()).type(loadConfigType).transport( transport ));
    }

    public LoadResult getLoadResult() {
        return loadResult;
    }
}

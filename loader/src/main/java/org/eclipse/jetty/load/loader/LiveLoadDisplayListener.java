package org.eclipse.jetty.load.loader;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.load.Monitor;

public class LiveLoadDisplayListener extends Request.Listener.Adapter implements Runnable {
    private final AtomicInteger requestQueue = new AtomicInteger();
    private Monitor.Start monitor = Monitor.start();

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

        System.err.printf("request queue: %d, jit=%s ms, cpu=%.2f%%%n",
                requestQueue.get(),
                stop.deltaJITTime,
                stop.cpuPercent);

        monitor = Monitor.start();
    }
}

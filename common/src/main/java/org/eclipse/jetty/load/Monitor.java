package org.eclipse.jetty.load;

import java.lang.management.ManagementFactory;
import java.time.ZonedDateTime;

public class Monitor {
    public static Start start() {
        return new Start();
    }

    private static class Base {
        public final int cores = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
        public final long time = System.nanoTime();
        public final ZonedDateTime date = ZonedDateTime.now();
        public final long jitTime = ManagementFactory.getCompilationMXBean().getTotalCompilationTime();
        public final long cpuTime = CPUTime.get();
    }

    public static class Start extends Base {
        private Start() {
        }

        public Stop stop() {
            return new Stop(this);
        }
    }

    public static class Stop extends Base {
        public final Start start;
        public final long deltaTime;
        public final long deltaJITTime;
        public final long deltaCPUTime;
        public final double cpuPercent;

        private Stop(Start start) {
            this.start = start;
            this.deltaTime = time - start.time;
            this.deltaJITTime = jitTime - start.jitTime;
            this.deltaCPUTime = cpuTime - start.cpuTime;
            this.cpuPercent = deltaTime == 0 ? Double.NaN : (double)deltaCPUTime * 100 / deltaTime / cores;
        }
    }
}

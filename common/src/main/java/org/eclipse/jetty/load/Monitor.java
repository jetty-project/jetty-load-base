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

package org.eclipse.jetty.load;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.time.ZonedDateTime;

public class Monitor
{
    public static Start start()
    {
        return new Start();
    }

    private static class Base
    {
        public final int cores = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
        public final long time = System.nanoTime();
        public final ZonedDateTime date = ZonedDateTime.now();
        public final long jitTime = ManagementFactory.getCompilationMXBean().getTotalCompilationTime();
        public final long cpuTime = CPUTime.get();
        public final String jvm = String.format("%s %s %s %s", System.getProperty("java.vm.vendor"), System.getProperty("java.vm.name"), System.getProperty("java.vm.version"), System.getProperty("java.runtime.version"));
        private final OperatingSystemMXBean osMBean = ManagementFactory.getOperatingSystemMXBean();
        public final String os = String.format("%s %s %s", osMBean.getName(), osMBean.getVersion(), osMBean.getArch());
        public final MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }

    public static class Start extends Base
    {
        private Start()
        {
        }

        public Stop stop()
        {
            return new Stop(this);
        }
    }

    public static class Stop extends Base
    {
        public final Start start;
        public final long deltaTime;
        public final long deltaJITTime;
        public final long deltaCPUTime;
        public final double cpuPercent;

        private Stop(Start start)
        {
            this.start = start;
            this.deltaTime = time - start.time;
            this.deltaJITTime = jitTime - start.jitTime;
            this.deltaCPUTime = cpuTime - start.cpuTime;
            this.cpuPercent = deltaTime == 0 ? Double.NaN : (double)deltaCPUTime * 100 / deltaTime / cores;
        }
    }
}

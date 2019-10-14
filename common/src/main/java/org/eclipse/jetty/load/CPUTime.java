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
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class CPUTime
{
    private static final Provider provider;

    static
    {
        Provider temp;
        try
        {
            Class<?> klass = Class.forName("java.lang.ProcessHandle");
            temp = new JDK9Provider(klass, klass.getMethod("current").invoke(null));
        }
        catch (Throwable x)
        {
            temp = new JDK8Provider();
        }
        provider = temp;
    }

    public static long get()
    {
        return provider.getCPUTime().orElse(Duration.ZERO).toNanos();
    }

    private interface Provider
    {
        Optional<Duration> getCPUTime();
    }

    private static class JDK9Provider implements Provider
    {
        private final Class<?> klass;
        private final Object process;

        public JDK9Provider(Class<?> klass, Object process)
        {
            this.klass = klass;
            this.process = process;
        }

        @Override
        public Optional<Duration> getCPUTime()
        {
            try
            {
                Method method = klass.getMethod("info");
                Object info = method.invoke(process);
                return (Optional<Duration>)method.getReturnType().getMethod("totalCpuDuration").invoke(info);
            }
            catch (Throwable x)
            {
                return Optional.empty();
            }
        }
    }

    private static class JDK8Provider implements Provider
    {
        @Override
        public Optional<Duration> getCPUTime()
        {
            return Optional.of(ManagementFactory.getOperatingSystemMXBean())
                    .filter(os -> os instanceof com.sun.management.OperatingSystemMXBean)
                    .map(os -> (com.sun.management.OperatingSystemMXBean)os)
                    .map(os -> Duration.of(os.getProcessCpuTime(), ChronoUnit.NANOS));
        }
    }
}

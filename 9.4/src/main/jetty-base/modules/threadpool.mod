[description]
Defines the server thread pool

[lib]
lib/jetty-load-base-common-*.jar

[xml]
etc/jetty-threadpool.xml

[ini-template]
### ThreadPool configuration
## Minimum number of threads
# jetty.threadPool.minThreads=10

## Maximum number of threads
# jetty.threadPool.maxThreads=200

## Thread idle timeout (in milliseconds)
# jetty.threadPool.idleTimeout=60000

## Thread pool detailed dump
# jetty.threadPool.detailedDump=false

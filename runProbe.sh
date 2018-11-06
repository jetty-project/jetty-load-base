java -showversion -Xmx10G -Xms10G -XX:+PrintCommandLineFlags -XX:+UseParallelOldGC -jar probe/target/jetty-load-base-probe-1.0.0-SNAPSHOT-uber.jar -tr h2c --running-time 300 --resource-groovy-path probe/src/main/resources/info.groovy --resource-rate 500 --threads 8 --users-per-thread 1 --channels-per-user 6 --host 10.0.0.20 --port 8080 --loader-resources-path loader/src/main/resources/loader.groovy --loader-rate 500 --loader-number 1 --rate-ramp-up 10

#!/bin/sh
exec java -jar jetty-load-generator-starter-${load.generator.version}-uber.jar -h localhost -p 8080 -pgp ./profile.groovy -t http -rt 10 -rtu s -tr 40 -u 100

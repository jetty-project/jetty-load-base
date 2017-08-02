#!/bin/sh
exec java -jar jetty-load-generator-starter-${load.generator.version}-uber.jar -ds -rgp ./loader.groovy -rt 10 -rr 40 -u 100

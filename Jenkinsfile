#!groovy


def jettyVersionParam = params.JETTY_VERSION ?: "9.4.14.v20181114"
//def jettyBaseFullVersionMap = ['9.4.11.v20180605':'9.4'] // '9.4.12.v20180830' '9.4.11.v20180605'
//def jettyBaseFullVersionMap = ["$jettyVersionParam":jettyVersionParam.substring( 0, 3 )] // '9.4.12.v20180830' '9.4.11.v20180605'
def jettyBaseFullVersionMap = [:]
jettyBaseFullVersionMap.put( jettyVersionParam, jettyVersionParam.startsWith( "10." ) ? "10.0" : jettyVersionParam.substring( 0, 3 ))

runningTime = params.RUNNING_TIME ?: "300"
loaderRate = params.LOADER_RATE ?: "300"
transport = params.TRANSPORT ?: "http"
jdk = params.JDK ?:"jdk11.0.1" // "jdk11" jdk8u112
jdkLoad = params.JDKLOAD ?:"jdk11.0.1" // "jdk11" jdk8u112
loaderNumber = params.LOADER_NUMBER ?: 3

// default values to avoid pipeline error
jenkinsBuildId= env.BUILD_ID
loadServerHostName = env.LOAD_TEST_SERVER_HOST
loadServerPort = env.LOAD_TEST_SERVER_PORT
loaderRunningTimes = [runningTime]
loaderRates = [loaderRate]
probeResourceRate = "500"
loaderThreads = "8"
loaderUsersPerThread = "4"
loaderChannelsPerUser = "12"
loaderMaxRequestsInQueue = "100000"
loaderVmOptions = "-showversion -Xmx10G -Xms10G -XX:+PrintCommandLineFlags -XX:+UseParallelOldGC"
//loaderInstancesNumbers = loaderNumber //[loaderNumber] could be an array
serverStarted = "false"
probeFinished = "false"
serverWd = "/home/jenkins/load_test_wd"
rateRampUp = 30
idleTimeout = 30000



parallel setup_loader_node :{
  node('load-test-loader-node') {
    stage( 'setup loader' ) {
      sh "rm -rf *"
      git url: "https://github.com/jetty-project/jetty-load-base.git", branch: 'master'
      withMaven( maven: 'maven3', jdk: "$jdkLoad", publisherStrategy: 'EXPLICIT',
                 mavenLocalRepo: '.repository' , globalMavenSettingsConfig: 'oss-settings.xml') {
        sh "mvn -q clean install -U -DskipTests -pl :jetty-load-base-loader -am"
        sh "mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -Dartifact=org.mortbay.jetty.load:jetty-load-base-loader:1.0.0-SNAPSHOT:jar:uber -DoutputDirectory=./ -Dmdep.stripVersion=true"
      }
      stash name: 'loader-jar', includes: 'jetty-load-base-loader-uber.jar'
      stash name: 'populate-script', includes: 'loader/src/main/scripts/populate.sh'
    }
  }
}, setup_probe_node: {
  node( 'load-test-probe-node' ) {
    stage( 'setup probe' ) {
      sh "rm -rf *"
      git url: "https://github.com/jetty-project/jetty-load-base.git", branch: 'master'
      withMaven( maven: 'maven3', jdk: "$jdkLoad", publisherStrategy: 'EXPLICIT',
                 mavenLocalRepo: '.repository' , globalMavenSettingsConfig: 'oss-settings.xml') {
        sh "mvn -q clean install -U -DskipTests -pl :jetty-load-base-probe -am"
        sh "mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -U -Dartifact=org.mortbay.jetty.load:jetty-load-base-probe:1.0.0-SNAPSHOT:jar:uber -DoutputDirectory=./ -Dmdep.stripVersion=true"
      }
      stash name: 'probe-jar', includes: 'jetty-load-base-probe-uber.jar'
    }
  }
}, failFast: true

//for (i = 0; i <5; i++) {
  //echo "iteration number $i"
  jettyBaseFullVersionMap.each { jettyVersion, jettyBaseVersion ->
    getLoadTestNode( jettyBaseVersion, jettyVersion, jdk, jdkLoad, jenkinsBuildId, loaderNumber, loaderRunningTimes )
  }
//}

node("master") {
  loadtestresult()
}



def getLoadTestNode(jettyBaseVersion,jettyVersion,jdk, jdkLoad,jenkinsBuildId,loaderInstancesNumber,loaderRunningTimes) {


  node( 'load-test-server-node' ) {
    stage( "build jetty app for version $jettyVersion" ) {
      dir (serverWd) {
        sh "rm -rf *"
        git url: "https://github.com/jetty-project/jetty-load-base.git", branch: 'master'
        //sh "rm -rf .repository"
        withMaven( maven: 'maven3', jdk: "$jdk", publisherStrategy: 'EXPLICIT',
                   mavenLocalRepo: '.repository') { // , globalMavenSettingsConfig: 'oss-settings.xml'
          // TODO make this configuration easier
          sh "mvn -q clean install -U -pl :jetty-load-base-$jettyBaseVersion,:test-webapp -am -Djetty.version=$jettyVersion"
        }
      }
    }
  }


  //for(loaderInstancesNumber in loaderInstancesNumbers) {
    for(loaderRunningTime in loaderRunningTimes){
      for (loaderRate in loaderRates){
        def loaderNodesFinished = new boolean[loaderInstancesNumber]
        def loaderNodesStarted = new boolean[loaderInstancesNumber]
        def loaderNodes = [:]

        echo "START load test for jettyVersion: $jettyVersion and loaderRate $loaderRate and loaderInstancesNumber $loaderInstancesNumber"

        try {
          for ( int i = 0; i < loaderInstancesNumber; i++ )
          {
            loaderNodesFinished[i] = false
            loaderNodesStarted[i] = false
            loaderNodes["loader-$i"] = getLoaderNode( i, loaderNodesFinished, loaderRate, jdkLoad, loaderRunningTime, loaderNodesStarted );
          }

          parallel server: {
            node( 'load-test-server-node' ) {
              dir (serverWd) {
                try {
                  stage( "starting jetty app ${jettyVersion}" ) {
                    withEnv( ["JAVA_HOME=${tool "$jdk"}"] ) {
                      serverVmOptions =
                              "-agentpath:/home/jenkins/async-profiler-1.4/build/libasyncProfiler.so=start,svg,file=$serverWd/profiler_$jettyVersion"+"_$loaderRate"+"_$loaderRunningTime"+"_$loaderInstancesNumber" +
                                      ".svg"
                      jettyStart = "${env.JAVA_HOME}/bin/java $serverVmOptions -jar ../jetty-home-$jettyVersion/start.jar"
                      if ( jettyBaseVersion == "9.2" || jettyBaseVersion == "9.3" ) jettyStart = "${env.JAVA_HOME}/bin/java $serverVmOptions -jar ../jetty-distribution-$jettyVersion/start.jar"
                      // TODO make this configuration easier
                      echo "start line: $jettyStart"
                      sh "cd $jettyBaseVersion/target/jetty-base && $jettyStart &"
                      echo "jetty server started version ${jettyVersion}"
                      // sleep to wait server started
                      sleep 60
                      waitUntil {
                        if ("$transport" == "h2c")
                          sh "curl -vv --http2-prior-knowledge --retry 100 --retry-connrefused --retry-delay 2 http://$loadServerHostName:$loadServerPort"
                        else
                          sh "curl -vv --retry 100 --retry-connrefused --retry-delay 2 http://$loadServerHostName:$loadServerPort"
                        return true
                      }
                      echo "get populate.sh"
                      unstash name: 'populate-script'
                      sh "bash loader/src/main/scripts/populate.sh $loadServerHostName"
                      echo "server data populated"
                      serverStarted = "true"
                      // we wait the end of all loader run
                      waitUntil {
                        allFinished = true;
                        for ( i = 0; i < loaderNodesFinished.length; i++ )
                        {
                          nodeFinished = loaderNodesFinished[i]
                          if ( !nodeFinished )
                          {
                            echo "not finished loader-$i"
                            allFinished = false
                          }
                        }
                        return allFinished && probeFinished == "true"
                      }
                      // stopping server
                      echo "Stopping server"
                      sh "curl -vv --retry 100 --retry-connrefused --retry-delay 2 http://$loadServerHostName:$loadServerPort/test/stopServer?STOP=true"
                    }
                  }
                }
                catch ( Exception e )
                {
                  echo "failure running server: " + e.getMessage()
                  throw e
                }
              }
            }
          }, probe: {
            node( 'load-test-probe-node' ) {
              try {
                stage( 'run probe' ) {
                  unstash name: 'probe-jar'
                  waitUntil {
                    echo "server not started probe is waiting"
                    return serverStarted.equals("true")
                  }
                  echo "start running probe"
                  timeout( time: 6, unit: 'HOURS' ) {
                    withEnv( ["JAVA_HOME=${tool "$jdkLoad"}"] ) {
                      sh "${env.JAVA_HOME}/bin/java $loaderVmOptions -jar jetty-load-base-probe-uber.jar -tr $transport -Djenkins.buildId=$jenkinsBuildId -Dorg.mortbay.jetty.load.generator.store.ElasticResultStore=true -Delastic.host=10.0.0.10 --rate-ramp-up $rateRampUp --running-time $loaderRunningTime --resource-groovy-path probe/src/main/resources/info.groovy --resource-rate $probeResourceRate --threads $loaderThreads --users-per-thread 1 --channels-per-user 6 --host $loadServerHostName --port $loadServerPort --loader-resources-path loader/src/main/resources/loader.groovy --loader-rate $loaderRate --loader-number $loaderInstancesNumber"
                    }
                  }
                  echo "end running probe"
                }
              }
              catch ( Exception e )
              {
                echo "failure running probe: " + e.getMessage()
                throw e
              } finally {
                probeFinished = "true"
              }
            }
          }, loader: {
            parallel loaderNodes
          }, failFast: true

          echo "END load test for jettyVersion: $jettyVersion and loaderRate $loaderRate"

          node( 'load-test-server-node' ) {
            dir( serverWd ) {
              archiveArtifacts artifacts: "*.svg"
            }
          }

        } catch ( Exception e ) {
          echo "FAIL load test:" + e.getMessage()
          //throw e
        }finally {
          serverStarted = "false"
          probeFinished = "false"
        }

      }
    }
  //}
}

def getLoaderNode(index,loaderNodesFinished,loaderRate,jdk,loaderRunningTime,loaderNodesStarted) {
  return {
    node('load-test-loader-node') {
      try
      {
        stage( "run loader rate ${loaderRate} for ${loaderRunningTime}s" ) {
          unstash name: 'loader-jar'
          waitUntil {
            echo "server not started loader $index is waiting"
            return serverStarted.equals( "true" )
          }
          echo "set loaderNodesStarted $index to true"
          loaderNodesStarted[index] = true
          timeout( time: 6, unit: 'HOURS' ) {
            withEnv( ["JAVA_HOME=${tool "$jdk"}"] ) {
              sh "${env.JAVA_HOME}/bin/java $loaderVmOptions -jar jetty-load-base-loader-uber.jar -tr $transport --rate-ramp-up $rateRampUp --running-time $loaderRunningTime --resource-groovy-path loader/src/main/resources/loader.groovy --resource-rate $loaderRate --threads $loaderThreads --users-per-thread $loaderUsersPerThread --channels-per-user $loaderChannelsPerUser --host $loadServerHostName --port $loadServerPort --max-requests-queued $loaderMaxRequestsInQueue -it $idleTimeout"
            }
          }
        }
      }
      catch ( Exception e )
      {
        echo "failure running loader with rate $loaderRate, index $index, msg: " + e.getMessage()
        throw e
      }
      finally
      {
        echo "loader $index finished on " + loaderNodesFinished.length
        loaderNodesFinished[index] = true;
      }
    }
  }
}

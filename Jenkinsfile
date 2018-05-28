#!groovy


def jettyBaseFullVersionMap = ['9.4.11-NO-LOGGER-SNAPSHOT':'9.4','9.4.11-SNAPSHOT':'9.4','9.4.10.v20180503':'9.4'] // ['9.2':'9.2.22.v20170606', '9.3':'9.3.20.v20170531', '9.4':'9.4.8.v20171121', '9.4':'9.4.10-SNAPSHOT']

// default values to avoid pipeline error
jenkinsBuildId= env.BUILD_ID
loadServerHostName = env.LOAD_TEST_SERVER_HOST
loadServerPort = env.LOAD_TEST_SERVER_PORT
loaderRunningTime = "600"//"300"
loaderRates = ["500"]
probeResourceRate = "500"
loaderThreads = "8"
loaderUsersPerThread = "4"
loaderChannelsPerUser = "10"
loaderMaxRequestsInQueue = "50000"
loaderVmOptions = "-showversion -Xmx4G -Xms4G -XX:+PrintCommandLineFlags -XX:+UseParallelOldGC"
loaderInstancesNumbers = [3]
rateRampUp = 30
jdk = "jdk8"

// used to shared status of loader nodes with the server instance to stop the server run
def loaderNodesFinished = new boolean[2];

// choices are newline separated
parameters {
  //choice(name: 'jettyVersion', choices: '9.2.22.v20170606\n9.3.20.v20170531\n9.4.8.v20171121\n9.4.9-SNAPSHOT', description: 'Which Jetty Version?')
  //choice(name: 'jettyBaseVersion', choices: '9.2\n9.3\n9.4', description: 'Which Jetty Version?')
  string(name: 'loaderInstancesNumber', defaultValue: '1', description: 'Number of loader instances')
  string(name: 'loaderRunningTime', defaultValue: '120', description: 'Time to run loader in seconds')
  string(name: 'loaderRate', defaultValue: '100', description: 'Loader Rate')
  string(name: 'loaderThreads', defaultValue: '4', description: 'Loader Threads number')
  string(name: 'loaderUsersPerThread', defaultValue: '4', description: 'Loader Users number')
  string(name: 'loaderChannelsPerUser', defaultValue: '8', description: 'Loader Channel per user')
  string(name: 'loaderMaxRequestsInQueue', defaultValue: '10000', description: 'Loader max requests in queue')
  string(name: 'loaderVmOptions', defaultValue: '-showversion -Xmx4G -Xms4G -XX:+PrintCommandLineFlags -XX:+UseParallelOldGC', description: 'Loader VM Options')
}

jettyBaseFullVersionMap.each {
  jettyVersion,jettyBaseVersion -> getLoadTestNode( loaderNodesFinished, jettyBaseVersion, jettyVersion, jdk, jenkinsBuildId, loaderInstancesNumbers)
}


node("master") {
  loadtestresult()
}



def getLoadTestNode(loaderNodesFinished,jettyBaseVersion,jettyVersion,jdk,jenkinsBuildId,loaderInstancesNumbers) {
  for(loaderInstancesNumber in loaderInstancesNumbers) {
    loaderNodesFinished = new boolean[loaderInstancesNumber];
    for ( loaderRate in loaderRates ) {

        echo "START load test for jettyVersion: $jettyVersion and loaderRate $loaderRate"

        // possible multiple loader node
        def loaderNodes = [:]
        for ( int i = 0; i < loaderInstancesNumber; i++ )
        {
          loaderNodesFinished[i] = false
          loaderNodes["loader-" + i] = getLoaderNode( i, loaderNodesFinished, loaderRate, jdk );
        }

        parallel server: {
          node( 'load-test-server-node' ) {
            stage( 'build jetty app' ) {
              git url: "https://github.com/jetty-project/jetty-load-base.git", branch: 'master'
              withMaven( maven: 'maven3.5', jdk: "$jdk", publisherStrategy: 'EXPLICIT',
                         mavenLocalRepo: '.repository' ) {
                sh "mvn clean install -q -pl :jetty-load-base-$jettyBaseVersion,test-webapp -am -Djetty.version=$jettyVersion"
              }
            }
            stage( "starting jetty app ${jettyVersion}" ) {
              withEnv( ["JAVA_HOME=${tool "$jdk"}"] ) {
                jettyStart = "${env.JAVA_HOME}/bin/java -jar ../jetty-home-$jettyVersion/start.jar"
                if ( jettyBaseVersion == "9.2" || jettyBaseVersion == "9.3" ) jettyStart = "${env.JAVA_HOME}/bin/java -jar ../jetty-distribution-$jettyVersion/start.jar"
                sh "cd $jettyBaseVersion/target/jetty-base && $jettyStart &"
                // we wait the end of all loader run
                waitUntil {
                  allFinished = true;
                  for ( i = 0; i < loaderNodesFinished.length; i++ )
                  {
                    nodeFinished = loaderNodesFinished[i]
                    if ( !nodeFinished )
                    {
                      echo "not finished " + i
                      allFinished = false
                    }
                  }
                  return allFinished
                }
              }
            }
          }
        }, loader: {
          parallel loaderNodes
        }, probe: {
          node( 'load-test-probe-node' ) {
            stage( 'setup probe' ) {
              sh "rm -rf .repository"
              withMaven( maven: 'maven3.5', jdk: "$jdk", publisherStrategy: 'EXPLICIT',
                         mavenLocalRepo: '.repository', globalMavenSettingsConfig: 'oss-settings.xml' ) {
                sh "mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -U -Dartifact=org.mortbay.jetty.load:jetty-load-base-probe:1.0.0-SNAPSHOT:jar:uber -DoutputDirectory=./ -Dmdep.stripVersion=true"
              }
              waitUntil {
                sh "wget --retry-connrefused -O foo.html --tries=150 --waitretry=10 http://$loadServerHostName:$loadServerPort"
                return true
              }
            }
            stage( 'run probe' ) {
              echo "start running probe"
              try
              {
                timeout( time: 60, unit: 'MINUTES' ) {
                  withEnv( ["JAVA_HOME=${tool "$jdk"}"] ) {
                    sh "${env.JAVA_HOME}/bin/java $loaderVmOptions -jar jetty-load-base-probe-uber.jar -Djenkins.buildId=$jenkinsBuildId -Dorg.mortbay.jetty.load.generator.store.ElasticResultStore=true -Delastic.host=10.0.0.10 --rate-ramp-up $rateRampUp --running-time $loaderRunningTime --resource-groovy-path probe/src/main/resources/info.groovy --resource-rate $probeResourceRate --threads $loaderThreads --users-per-thread 1 --channels-per-user 6 --host $loadServerHostName --port $loadServerPort --loader-resources-path loader/src/main/resources/loader.groovy --loader-rate $loaderRate --loader-number $loaderInstancesNumber"
                  }
                }
                echo "end running probe"
              }
              catch ( Exception e )
              {
                echo "failure running probe"
                //throw e
              }
              finally
              {
                //loaderNodesFinished[index] = true;
              }
            }
          }
        }, failFast: true

        echo "END load test for jettyVersion: $jettyVersion and loaderRate $loaderRate"
      }
    }
}

def getLoaderNode(index,loaderNodesFinished,loaderRate,jdk) {
  return {
    node('load-test-loader-node') {
      stage ('setup loader') {
        sh "rm -rf .repository"
        withMaven( maven: 'maven3.5', jdk: "$jdk", publisherStrategy: 'EXPLICIT',
                   mavenLocalRepo: '.repository', globalMavenSettingsConfig: 'oss-settings.xml' ) {
          sh "mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -U -Dartifact=org.mortbay.jetty.load:jetty-load-base-loader:1.0.0-SNAPSHOT:jar:uber -DoutputDirectory=./ -Dmdep.stripVersion=true"
        }
        waitUntil {
          sh "wget --retry-connrefused -O foo.html --tries=150 --waitretry=10 http://$loadServerHostName:$loadServerPort"
          return true
        }
        sh 'wget -O populate.sh "https://raw.githubusercontent.com/jetty-project/jetty-load-base/master/loader/src/main/scripts/populate.sh"'
        sh "bash populate.sh $loadServerHostName"
      }
      stage ("run loader rate ${loaderRate}") {
        echo "loader $index started"
        try
        {
          timeout(time: 60, unit: 'MINUTES') {
            withEnv( ["JAVA_HOME=${tool "$jdk" }"] ) {
              sh "${env.JAVA_HOME}/bin/java $loaderVmOptions -jar jetty-load-base-loader-uber.jar --rate-ramp-up $rateRampUp --running-time $loaderRunningTime --resource-groovy-path loader/src/main/resources/loader.groovy --resource-rate $loaderRate --threads $loaderThreads --users-per-thread $loaderUsersPerThread --channels-per-user $loaderChannelsPerUser --host $loadServerHostName --port $loadServerPort --max-requests-queued $loaderMaxRequestsInQueue"
            }
          }
          echo "loader $index finished on " + loaderNodesFinished.length
        } catch ( Exception e ) {
          echo "failure running loader with rate $loaderRate"
        } finally {
          loaderNodesFinished[index] = true;
        }
      }
    }
  }
}

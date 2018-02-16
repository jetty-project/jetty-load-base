#!groovy


def jettyBaseFullVersionMap = ['9.2':'9.2.22.v20170606', '9.3':'9.3.20.v20170531', '9.4':'9.4.8.v20171121', '9.4':'9.4.9-SNAPSHOT']

// default values to avoid pipeline error
loadServerHostName = env.LOAD_TEST_SERVER_HOST
loadServerPort = env.LOAD_TEST_SERVER_PORT
loaderRunningTime = "300"
loaderRates = ["100","150","200","250"]
probeResourceRate = "500"
loaderThreads = "8"
loaderUsersPerThread = "4"
loaderChannelsPerUser = "10"
loaderMaxRequestsInQueue = "50000"
loaderVmOptions = "-showversion -Xmx4G -Xms4G -XX:+PrintCommandLineFlags -XX:+UseParallelOldGC"
loaderInstancesNumber = 2
rateRampUp = 30

// used to shared status of loader nodes with the server instance to stop the server run
def loaderNodesFinished = new boolean[loaderInstancesNumber];

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

node() {
  jettyBaseFullVersionMap.each {
    jettyBaseVersion, jettyVersion -> getLoadTestNode( loaderNodesFinished, jettyBaseVersion, jettyVersion )
  }
}

def getLoadTestNode(loaderNodesFinished,jettyBaseVersion,jettyVersion) {
  node() {
    for ( loaderRate in loaderRates )
    {
      echo "load test for jettyVersion: $jettyVersion and loaderRate $loaderRate"

      // possible multiple loader node
      def loaderNodes = [:]
      for ( int i = 0; i < loaderInstancesNumber; i++ )
      {
        loaderNodesFinished[i] = false
        loaderNodes["loader-" + i] = getLoaderNode( i, loaderNodesFinished, loaderRate );
      }

      parallel server: {
        node( 'load-test-server-node' ) {
          stage( 'build jetty app' ) {
            git url: "https://github.com/jetty-project/jetty-load-base.git", branch: 'master'
            withMaven( maven: 'maven3', jdk: 'jdk8',
                       mavenLocalRepo: '.repository' ) {
              sh "mvn clean install -q -pl :jetty-load-base-$jettyBaseVersion,test-webapp -am -Djetty.version=$jettyVersion"
            }
          }
          stage( 'starting jetty app' ) {
            withEnv(["JAVA_HOME=${ tool 'jdk8' }"]) {
              jettyStart = "${env.JAVA_HOME}/bin/java -jar ../jetty-home-$jettyVersion/start.jar"
              if ( jettyBaseVersion == "9.2" || jettyBaseVersion == "9.3" ) jettyStart = "${env.JAVA_HOME}/bin/java -jar ../jetty-distribution-$jettyVersion/start.jar"
              sh "cd $jettyBaseVersion/target/jetty-base && $jettyStart &"
              // we wait the end of all loader run
              waitUntil {
                allFinished = true;
                for ( item in loaderNodesFinished )
                {
                  if ( !item )
                  {
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
          stage ('setup probe') {
            //echo "probe node"
            git url: "https://github.com/jetty-project/jetty-load-base.git", branch: 'master'
            sh 'rm -f jetty-base-loader-probe.jar && wget -O jetty-base-loader-probe.jar -q "https://oss.sonatype.org/service/local/artifact/maven/content?r=jetty-snapshots&g=org.mortbay.jetty.load&a=jetty-load-base-probe&v=1.0.0-SNAPSHOT&p=jar&c=uber"'
            waitUntil {
              sh "wget --retry-connrefused -O foo.html --tries=150 --waitretry=10 http://$loadServerHostName:$loadServerPort"
              return true
            }
          }
          stage ('run probe') {
            try
            {
              withEnv( ["JAVA_HOME=${tool 'jdk8'}"] ) {
                sh "${env.JAVA_HOME}/bin/java $loaderVmOptions -jar jetty-base-loader-probe.jar --rate-ramp-up $rateRampUp --running-time $loaderRunningTime --resource-groovy-path probe/src/main/resources/info.groovy --resource-rate $probeResourceRate --threads $loaderThreads --users-per-thread 1 --channels-per-user 6 --host $loadServerHostName --port $loadServerPort --max-requests-queued $loaderMaxRequestsInQueue"
              }
            } catch ( Exception e ) {
              echo "failure running probe"
              throw e
            } finally {
              //loaderNodesFinished[index] = true;
            }
          }
        }
      },
      failFast: true
    }
  }
}

def getLoaderNode(index,loaderNodesFinished,loaderRate) {
  return {
    node('load-test-loader-node') {
      stage ('setup loader') {
        git url: "https://github.com/jetty-project/jetty-load-base.git", branch: 'master'
        /*
        // we do not need to build this it's build and deployed apart
        withMaven(
            maven: 'maven3',
            mavenLocalRepo: '.repository') {
            sh "mvn clean install -pl :jetty-load-base-loader -am"
        } */
        sh 'rm -f jetty-base-loader.jar && wget -O jetty-base-loader.jar -q "https://oss.sonatype.org/service/local/artifact/maven/content?r=jetty-snapshots&g=org.mortbay.jetty.load&a=jetty-load-base-loader&v=1.0.0-SNAPSHOT&p=jar&c=uber"'
        waitUntil {
          sh "wget --retry-connrefused -O foo.html --tries=150 --waitretry=10 http://$loadServerHostName:$loadServerPort"
          return true
        }
        sh "bash loader/src/main/scripts/populate.sh $loadServerHostName"
      }
      stage ('run loader') {
        try
        {
          withEnv( ["JAVA_HOME=${tool 'jdk8'}"] ) {
            sh "${env.JAVA_HOME}/bin/java $loaderVmOptions -jar jetty-base-loader.jar --rate-ramp-up $rateRampUp --running-time $loaderRunningTime --resource-groovy-path loader/src/main/resources/loader.groovy --resource-rate $loaderRate --threads $loaderThreads --users-per-thread $loaderUsersPerThread --channels-per-user $loaderChannelsPerUser --host $loadServerHostName --port $loadServerPort --max-requests-queued $loaderMaxRequestsInQueue"
          }
        } catch ( Exception e ) {
          echo "failure running loader with rate $loaderRate"
        } finally {
          loaderNodesFinished[index] = true;
        }
      }
    }
  }
}

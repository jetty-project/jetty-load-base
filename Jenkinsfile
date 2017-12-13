#!groovy

// default values to avoid pipeline error
jettyVersion = "9.2.22.v20170606"
jettyBaseVersion = "9.2"
loadServerHostName = env.LOAD_TEST_SERVER_HOST
loadServerPort = env.LOAD_TEST_SERVER_PORT
loaderRunningTime = "120"
loaderRate = "100"
loaderThreads = "4"
loaderUsers = "4"
loaderChannelsPerUser = "8"
loaderMaxRequestsInQueue = "10000"
loaderVmOptions = "-showversion -Xmx4G -Xms4G -XX:+PrintCommandLineFlags -XX:+UseParallelOldGC"
loaderInstancesNumber = 1

def loaderNodesFinished = new boolean[loaderInstancesNumber];

// choices are newline separated
parameters {
  choice(name: 'jettyVersion', choices: '9.2.22.v20170606\n9.3.20.v20170531\n9.4.8.v20171121\n9.4.9-SNAPSHOT', description: 'Which Jetty Version?')
  choice(name: 'jettyBaseVersion', choices: '9.2\n9.3\n9.4', description: 'Which Jetty Version?')
  string(name: 'loaderInstancesNumber', defaultValue: '1', description: 'Number of loader instances')
  string(name: 'loaderRunningTime', defaultValue: '120', description: 'Time to run loader in seconds')
  string(name: 'loaderRate', defaultValue: '100', description: 'Loader Rate')
  string(name: 'loaderThreads', defaultValue: '4', description: 'Loader Threads number')
  string(name: 'loaderUsers', defaultValue: '4', description: 'Loader Users number')
  string(name: 'loaderChannelsPerUser', defaultValue: '8', description: 'Loader Channel per user')
  string(name: 'loaderMaxRequestsInQueue', defaultValue: '10000', description: 'Loader max requests in queue')
  string(name: 'loaderVmOptions', defaultValue: '-showversion -Xmx4G -Xms4G -XX:+PrintCommandLineFlags -XX:+UseParallelOldGC', description: 'Loader VM Options')
}

node() {

  // possible multiple loader node
  def loaderNodes = [:]
  for (int i = 0; i < loaderInstancesNumber; i++) {
    loaderNodesFinished[i] = false
    loaderNodes["loader-"+i] = getLoaderNode(i,loaderNodesFinished);
  }

  parallel server: {
    node('server-node') {
      stage ('build jetty app') {
        echo "$jettyVersion"
        git url:"https://github.com/jetty-project/jetty-load-base.git", branch: 'master'
        withMaven(
                maven: 'maven3',
                mavenLocalRepo: '.repository') {
          sh "mvn clean install -q -pl :jetty-load-base-$jettyBaseVersion,test-webapp -am -Djetty.version=$jettyVersion"
        }
      }
      stage ('starting jetty app') {
        jettyStart ="java -jar ../jetty-home-$jettyVersion/start.jar"
        if (jettyBaseVersion == "9.2" || jettyBaseVersion == "9.3")
          jettyStart ="java -jar ../jetty-distribution-$jettyVersion/start.jar"
        sh "cd $jettyBaseVersion/target/jetty-base && $jettyStart &"
        waitUntil {
          allFinished = true;
          for(item in loaderNodesFinished){
            if(!item) {
              allFinished = false
            }
          }
          return allFinished
        }
      }
    }
  }, loader: {
    parallel loaderNodes
  }, probe: {
    node('probe-node') {
      echo "probe node"
    }
  },
  failFast: true
}


def getLoaderNode(index,loaderNodesFinished) {
  return {
    node('loader-node') {
      stage ('setup loader') {
        git url: "https://github.com/jetty-project/jetty-load-base.git", branch: 'master'
        /*
        // we do not need to build this it's build and deployed apart
        withMaven(
            maven: 'maven3',
            mavenLocalRepo: '.repository') {
            sh "mvn clean install -pl :jetty-load-base-loader -am"
        } */
        waitUntil {
          sh "wget --retry-connrefused --tries=150 -q --waitretry=10 http://$loadServerHostName:$loadServerPort"
          return true
        }
        sh "bash loader/src/main/scripts/populate.sh $loadServerHostName"
        sh 'rm -f jetty-base-loader.jar && wget -O jetty-base-loader.jar -q "https://oss.sonatype.org/service/local/artifact/maven/content?r=jetty-snapshots&g=org.mortbay.jetty.load&a=jetty-load-base-loader&v=1.0.0-SNAPSHOT&p=jar&c=uber"'
      }
      stage ('run loader') {
        sh "java $loaderVmOptions -jar jetty-base-loader.jar --running-time $loaderRunningTime --resource-groovy-path loader/src/main/resources/loader.groovy --resource-rate $loaderRate --threads $loaderThreads --users $loaderUsers --channels-per-user $loaderChannelsPerUser --host $loadServerHostName --port $loadServerPort --max-requests-queued $loaderMaxRequestsInQueue"
        loaderNodesFinished[index] = true;
      }
    }
  }
}


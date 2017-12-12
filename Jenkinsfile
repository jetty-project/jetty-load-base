#!groovy

node() {
  def stopJob = false;
  // default values to avoid pipeline error
  jettyVersion = "9.2.22.v20170606"
  jettyBaseVersion = "9.2"

  parameters {
    // choices are newline separated
    choice(choices: '9.2.22.v20170606\n9.3.20.v20170531\n9.4.8.v20171121\n9.4.9-SNAPSHOT', description: 'Which Jetty Version?', name: 'jettyVersion')
    choice(choices: '9.2\n9.3\n9.4', description: 'Which Jetty Version?', name: 'jettyBaseVersion')
  }
  parallel server: {
    node('server-node') {
      stage ('build jetty app') {
        echo "$jettyVersion"
        git url:"https://github.com/jetty-project/jetty-load-base.git", branch: 'master'
        withMaven(
                maven: 'maven3',
                mavenLocalRepo: '.repository') {
          sh "mvn clean install -pl :jetty-load-base-$jettyBaseVersion,test-webapp -am -Djetty.version=$jettyVersion"
        }
      }
      stage ('starting jetty app') {
        jettyStart ="java -jar ../jetty-home-$jettyVersion/start.jar"
        if (jettyBaseVersion == "9.2" || jettyBaseVersion == "9.3")
          jettyStart ="java -jar ../jetty-distribution-$jettyVersion/start.jar"
        sh "cd $jettyBaseVersion/target/jetty-base && $jettyStart &"
        waitUntil {
          return stopJob
        }
      }
    }
  }, loader: {
    node('loader-node') {

      git url:"https://github.com/jetty-project/jetty-load-base.git", branch: 'master'
      /*
      // we do not need to build this it's build and deployed apart
      withMaven(
          maven: 'maven3',
          mavenLocalRepo: '.repository') {
          sh "mvn clean install -pl :jetty-load-base-loader -am"
      } */
      waitUntil {
        sh "wget --retry-connrefused --tries=150 -q --waitretry=10 http://54.197.97.185:8080"
        return true
      }
      sh "bash loader/src/main/scripts/populate.sh 54.197.97.185"
      sh 'rm -f jetty-base-loader.jar && wget -O jetty-base-loader.jar "https://oss.sonatype.org/service/local/artifact/maven/content?r=jetty-snapshots&g=org.mortbay.jetty.load&a=jetty-load-base-loader&v=1.0.0-SNAPSHOT&p=jar&c=uber"'
      sh "java -showversion -Xmx4G -Xms4G -XX:+PrintCommandLineFlags -XX:+UseParallelOldGC -jar jetty-base-loader.jar --running-time 120 --resource-groovy-path loader/src/main/resources/loader.groovy --resource-rate 100 --threads 4 --users 4 --channels-per-user 6 --host 54.197.97.185 --port 8080 --max-requests-queued 1000"
      currentBuild.result = 'SUCCESS'
      stopJob = true;
      //return 0
    }
  }, probe: {
    node('probe-node') {
      echo "probe node"
    }
  },
  failFast: true
}
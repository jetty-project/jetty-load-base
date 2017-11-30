#!groovy

node {

  stage('Checkout') {
    checkout scm
  }

  stage ('Build') {

    withMaven(
            maven: 'maven3',
            jdk: 'jdk8',
            mavenSettingsConfig: 'oss-settings.xml') {
      sh "mvn clean deploy"
    }
  }

}
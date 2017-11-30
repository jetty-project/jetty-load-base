#!groovy

node {
  def mvntool = tool name: 'maven3', type: 'hudson.tasks.Maven$MavenInstallation'
  def jdktool = tool name: 'jdk8', type: 'hudson.model.JDK'

  stage('Checkout') {
    checkout scm
  }

  stage ('Build') {

    withMaven(
            maven: 'maven3',
            jdk: 'jdk8',
            mavenSettingsConfig: 'OssGlobalSettings') {
      sh "mvn clean install"
    }
  }

}
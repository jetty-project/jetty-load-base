pipeline {
  agent any
  parameters {
    /*choice(name: 'target_host',
           choices: 'one\ntwo\nthree\nfour',
           description: 'What door do you choose?')
    booleanParam(name: 'CAN_DANCE',
                 defaultValue: true,
                 description: 'Checkbox parameter')*/
    string(name: 'target_host',
           defaultValue: 'localhost!',
           description: 'Which host')
  }
  stages {
    stage('Example') {
      steps {
        echo 'Hello World!'
        echo "The DJ says: ${params.target_host}"
      }
    }
  }
}
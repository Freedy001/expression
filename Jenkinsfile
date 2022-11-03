pipeline {
    agent any
    stages {
        stage('build') {
            steps {
                powershell  'mvn --version'
                powershell 'pwd'
                for (a in 0..10) {
                    powershell 'pwd'
                }
            }
        }
    }
}

pipeline {
    agent any
    stages {
        stage('build') {
            steps {
                powershell  'mvn --version'
                powershell 'pwd'
            }
        }
    }
}

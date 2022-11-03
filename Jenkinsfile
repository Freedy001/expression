pipeline {
    agent any
    environment { 
        JAVA_HOME = 'C:\Users\wuyuejiang\.jdks\corretto-17.0.4.1'
    }
    stages {
        stage('build') {
            steps {
                powershell  'mvn --version'
                powershell 'pwd'
                powershell 'mvn -DskipTests package'
            }
        }
    }
}

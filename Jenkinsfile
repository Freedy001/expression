pipeline {
    agent any
    environment { 
        JAVA_HOME = 'C:\\Users\\wuyuejiang\\.jdks\\corretto-17.0.4.1'
    }
    parameters {
        choice choices: ['unstable', 'stable'], description: '', name: 'type'
        string defaultValue: 'aaa', description: '分支名称', name: 'branch', trim: true
        booleanParam(defaultValue: false, description: '是否开启调试，默认为否。', name: 'debug')
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

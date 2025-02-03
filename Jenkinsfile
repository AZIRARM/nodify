pipeline {
    agent any

    tools {
        maven "Maven"
        jdk "Jdk"
        nodejs "Node"
    }

    stages {
        stage('Initialize') {
            steps {
                sh '''
                    set -e
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                    echo "JAVA_HOME = ${JAVA_HOME}"
                '''
            }
        }

        stage('Test') {
            steps {
                    sh '''
                        set -e
                        mvn test
                    '''
            }
        }

        stage('Build') {
            steps {
                    sh '''
                        set -e
                        mvn clean install -DskipTests
                    '''
            }
        }
    }
}

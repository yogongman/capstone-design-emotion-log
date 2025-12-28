pipeline {
    agent any
    stages {
        stage('Checkout') { steps { checkout scm } }
        stage('Deploy') {
            steps {
                // 기존 컨테이너 내리고 -> 새로 빌드해서 -> 올림
                sh 'docker-compose down || true' 
                sh 'docker-compose up -d --build'
                sh 'docker image prune -f'
            }
        }
    }
}
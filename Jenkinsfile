pipeline {
    agent any
    stages {
        stage('Prepare Environment') {
            steps {
                // 저장해둔 .env 파일을 현재 젠킨스 워크스페이스(최상위 폴더)로 복사
                sh 'cp /home/hoon/env_storage/.env ./'
            }
        }
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Deploy') {
            steps {
                // 기존 컨테이너 중단 및 삭제 -> 이미지 재빌드 -> 실행
                sh 'docker-compose down || true' 
                sh 'docker-compose up -d --build'
                sh 'docker image prune -f' // 쓰지 않는 이미지 정리
            }
        }
    }
}
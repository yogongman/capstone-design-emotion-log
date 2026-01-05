# 1단계: 빌드 스테이지 (Build Stage)
FROM gradle:8.2.1-jdk17 AS builder
WORKDIR /app

# 그래들 설정 파일들을 먼저 복사하여 종속성 캐싱 활용
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 실행 권한 부여 및 종속성 다운로드
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드 (테스트는 제외하여 속도 향상)
COPY src src
RUN ./gradlew build -x test --no-daemon

# 2단계: 실행 스테이지 (Run Stage)
FROM eclipse-temurin:17-jdk-focal
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일만 추출하여 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행 환경 설정
ENTRYPOINT ["java", "-jar", "app.jar"]
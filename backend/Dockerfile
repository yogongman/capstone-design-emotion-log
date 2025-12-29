# [Build Stage] Gradle로 소스 코드 빌드
FROM gradle:8-jdk17-alpine AS builder
WORKDIR /app

# 캐싱을 위해 설정 파일 먼저 복사
COPY build.gradle settings.gradle ./
# 의존성 다운로드 (실패해도 넘어가도록 설정)
RUN gradle dependencies --no-daemon || true

# 소스 코드 전체 복사 및 빌드 (Test 제외)
COPY . .
RUN gradle bootJar --no-daemon -x test

# [Run Stage] 실제 실행할 가벼운 환경
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 결과물(jar)만 가져오기
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
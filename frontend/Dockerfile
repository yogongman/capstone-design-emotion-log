# [Build Stage] Node.js로 리액트 빌드
FROM node:18-alpine AS builder
WORKDIR /app

# 라이브러리 설치
COPY package.json package-lock.json ./
RUN npm install

# 소스 코드 복사 및 빌드
COPY . .
RUN npm run build

# [Run Stage] Nginx로 웹서버 실행
FROM nginx:alpine

# Nginx 설정 파일 적용
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 빌드 결과물(dist)을 Nginx 폴더로 복사
# 만약 CRA(Create-React-App)라면 /app/dist 대신 /app/build 로 변경 필요
COPY --from=builder /app/dist /usr/share/nginx/html

CMD ["nginx", "-g", "daemon off;"]
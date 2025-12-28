# 가벼운 Nginx 웹서버 사용
FROM nginx:alpine

# 내가 만든 설정파일과 HTML을 컨테이너 안으로 복사
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/index.html

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]
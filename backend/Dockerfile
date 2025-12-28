# 가벼운 파이썬 이미지 사용
FROM python:3.9-alpine

# 작업 폴더 생성
WORKDIR /app

# 파이썬 스크립트 복사
COPY server.py .

# 실행 (로그가 바로 보이게 -u 옵션 사용)
CMD ["python", "-u", "server.py"]
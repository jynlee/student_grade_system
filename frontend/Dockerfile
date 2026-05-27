# =============================================================================
# 프론트엔드 컨테이너 — nginx로 정적 HTML/CSS/JS 서빙 + /api 리버스 프록시
# =============================================================================
FROM nginx:alpine

# 커스텀 nginx 설정 (정적 파일 + /api → backend:8080 리버스 프록시)
COPY nginx.conf      /etc/nginx/conf.d/default.conf

# 정적 파일 복사
COPY index.html      /usr/share/nginx/html/index.html
COPY register.html   /usr/share/nginx/html/register.html
COPY scores.html     /usr/share/nginx/html/scores.html
COPY view.html       /usr/share/nginx/html/view.html
COPY css/            /usr/share/nginx/html/css/
COPY js/             /usr/share/nginx/html/js/
EXPOSE 80

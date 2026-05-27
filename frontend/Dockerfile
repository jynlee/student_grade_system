# =============================================================================
# 프론트엔드 컨테이너 — nginx로 정적 HTML/CSS/JS 서빙
# =============================================================================
FROM nginx:alpine
# 기본 nginx 설정 그대로 사용 (포트 80, /usr/share/nginx/html 가 document root)
# 정적 파일 복사
COPY index.html      /usr/share/nginx/html/index.html
COPY register.html   /usr/share/nginx/html/register.html
COPY scores.html     /usr/share/nginx/html/scores.html
COPY view.html       /usr/share/nginx/html/view.html
COPY css/            /usr/share/nginx/html/css/
COPY js/             /usr/share/nginx/html/js/
EXPOSE 80

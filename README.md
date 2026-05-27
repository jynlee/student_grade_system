# 학생 성적 관리 시스템 (Student Grade System)

수업 중간고사 프로젝트 — 학생 정보를 등록하고 과목별 점수를 입력하면, 평균과 등급을 계산해 조회할 수 있는 웹 시스템입니다.

## 기술 스택

- **백엔드:** Java 17, Servlet 4.0, Apache Tomcat 9, Maven, JDBC
- **프론트엔드:** HTML5, CSS3, Vanilla JavaScript (정적 파일, nginx 서빙)
- **데이터베이스:** MySQL 8.0
- **인프라:** Docker Compose

## 실행 방법

```bash
# 컨테이너 빌드 및 실행
docker compose up --build

# 종료
docker compose down

# 데이터 초기화 (볼륨 삭제)
docker compose down -v
```

## 접속 URL

| 서비스 | URL |
|--------|-----|
| 프론트엔드 | http://localhost:8080 |
| 백엔드 API | http://localhost:8081/api/... |

## 디렉토리 구조

```
students/
├── backend/      Tomcat WAR 프로젝트 (Maven)
├── frontend/     정적 HTML/CSS/JS (nginx)
├── db/           MySQL 초기 스키마
├── docs/         설계·발표·캡처
└── docker-compose.yml
```

## 문서

- [설계 문서](docs/design.md)
- [발표 자료](docs/presentation.md)
- [실행 화면 캡처](docs/screenshots/)

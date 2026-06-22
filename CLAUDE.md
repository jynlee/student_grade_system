# Claude Code 작업 컨텍스트

이 파일은 새 세션에서 자동으로 읽혀 프로젝트 컨텍스트로 사용됩니다. 다른 컴퓨터에서 `claude`만 실행하면 아래 내용이 즉시 로드됩니다.

## 프로젝트 한줄 요약

학생 정보를 등록하고 과목별 점수를 입력하면 평균·등급을 계산해 보여주는 3-tier 웹 시스템. 수업 중간고사용. Servlet/JDBC/Tomcat/MySQL을 Docker Compose로 묶음.

## 현재 상태 (2026-05-27 기준)

- ✅ 골격: `docker-compose.yml`, `db/init.sql`, 백엔드/프론트엔드 Dockerfile
- ✅ 백엔드: Servlet + Service + DAO + JDBC 일통 구현
- ✅ 프론트엔드: 4개 HTML(`index`, `register`, `scores`, `view`) + JS/CSS
- ✅ 아키텍처: nginx 리버스 프록시(`/api → backend:8080`), `API_BASE = "/api"` 상대경로, CORS 우회 불필요
- ✅ 에러 처리: `api.js`가 네트워크 오류를 `status:0`으로 정규화하여 호출자에게 일관된 분기 제공
- ✅ 문서: `docs/design.md` (등급 산식·반올림 규칙 명시 포함), `docs/presentation.md` 초안
- ✅ **한글 인코딩 버그 수정** (2026-05-27):
  - `db/init.sql` 맨 위에 `SET NAMES utf8mb4;` 추가 → Docker 엔트리포인트 이중인코딩 방지
  - `DbConnection.java` JDBC URL에 `connectionCollation=utf8mb4_0900_ai_ci` 추가 → JDBC 내부 디코더를 utf8mb4로 강제
  - `docker-compose.yml`의 deprecated `version:` 필드 제거
- ⏳ 남은 일거리: 발표 자료 마감 (발표자 이름 기입), 실행 스크린샷 캡처

## 기술 스택 / 포트 / 빌드

- Java 17 + Servlet 4.0 + Tomcat 9 (WAR, Maven 멀티스테이지 빌드)
- 정적 nginx + Vanilla JS (프레임워크 없음)
- MySQL 8.0, 컨테이너 호스트명 `db`, `init.sql`로 스키마/시드 자동 주입
- 호스트 포트: frontend `8080` (메인 진입점), backend `8081` (디버깅용 직접 호출), db `3306` (디버깅용)
- 브라우저는 8080 한곳만 사용 → `/api/*`는 nginx가 컨테이너 내부 `backend:8080`로 프록시
- 모든 빌드는 `docker compose up --build`에서 끝남 — 호스트 JDK/Maven 불필요

## 코드 구조 (탐색용)

```
backend/src/main/
  java/com/school/...     Servlet → Service → DAO → JDBC
  webapp/WEB-INF/web.xml  URL 매핑
frontend/
  index.html              메인 메뉴
  register.html + js/     학생 등록
  scores.html + js/       점수 입력
  view.html + js/         결과 조회 (평균·등급)
db/init.sql               students, scores 테이블 + 시드
```

API 베이스: `/api` (상대경로, nginx가 `backend:8080`으로 프록시). 직접 디버깅 시 `http://localhost:8081/api/...`. CORS는 백엔드에서 `*`로 허용. 등급 산정 로직은 Service 계층(상세는 `docs/design.md` §4 참고).

## 작업 컨벤션

- **응답 언어:** 한국어
- **커밋 단위:** 잘게 쪼개지 말고 의미 있는 단위로 묶을 것 (예: "feat: 점수 입력 검증 + 에러 메시지"). 전체 ~5개 커밋 정도가 적당.
- **불필요한 추상화 금지:** 수업 과제이므로 지금 필요한 만큼만. "확장 가능성"이라는 이유로 인터페이스/팩토리/추상클래스 도입하지 말 것.
- **DB 스키마 변경 시:** `db/init.sql` 수정 후 반드시 `docker compose down -v` (볼륨 비우고 재기동, 그래야 init.sql이 다시 실행됨).
- **PDF·작업 메모는 git에 올리지 않음:** `.gitignore`에서 `*.pdf`, `docs/superpowers/` 제외.

## 자주 쓰는 명령

```bash
docker compose up -d --build           # 백그라운드 기동
docker compose logs -f backend         # 백엔드 로그 추적
docker compose exec db mysql -uroot -pstudentpass students   # MySQL 접속
docker compose down -v                 # DB 볼륨까지 초기화
```

## 트러블슈팅 단축본

- 포트 충돌(8080/8081/3306) → `netstat -ano | findstr <포트>`로 점유 프로세스 확인
- 백엔드가 죽음 → 대개 MySQL 초기화 타이밍. `docker compose up -d backend`로 재기동
- 스키마 변경 미반영 → `down -v` 후 재기동 (init.sql은 빈 볼륨일 때만 실행)
- Docker daemon 못 찾음 → Docker Desktop 실행 확인

상세 가이드는 `README.md` 참고.

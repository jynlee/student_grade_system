# 학생 성적 관리 시스템 (Student Grade System)

수업 중간고사 프로젝트 — 학생 정보를 등록하고 과목별 점수를 입력하면, 평균과 등급을 계산해 조회할 수 있는 웹 시스템입니다.

## 기술 스택

- **백엔드:** Java 17, Servlet 4.0, Apache Tomcat 9, Maven, JDBC
- **프론트엔드:** HTML5, CSS3, Vanilla JavaScript (정적 파일, nginx 서빙)
- **데이터베이스:** MySQL 8.0
- **인프라:** Docker Compose

## 사전 요구사항

| 항목 | 버전 | 비고 |
|------|------|------|
| Docker Desktop | 최신 | Compose v2 포함 — `docker compose` 명령 사용 |
| Git | 2.x+ | 클론용 |
| (선택) JDK 17 | Temurin 등 | 컨테이너 없이 로컬에서 빌드/디버깅할 때만 |
| (선택) Maven 3.9+ | | 컨테이너 빌드를 쓰면 호스트에 불필요 |

> 백엔드는 멀티스테이지 Dockerfile 안에서 Maven으로 빌드되므로, 호스트에 JDK/Maven이 없어도 `docker compose up --build`만으로 동작합니다.

## 빠른 시작 (클론 후 즉시 실행)

```bash
# 1) 저장소 클론
git clone https://github.com/jynlee/student_grade_system.git
cd student_grade_system

# 2) Docker Desktop 실행 중인지 확인 후
docker compose up --build

# 3) 브라우저 접속
#    프론트엔드:  http://localhost:8080
#    백엔드 API: http://localhost:8081/api/...
```

최초 실행 시 이미지 다운로드와 Maven 의존성 캐싱으로 5~10분 정도 걸릴 수 있습니다. 그 뒤로는 30초 내외로 기동됩니다.

## 자주 쓰는 명령

```bash
# 백그라운드 실행
docker compose up -d --build

# 로그 보기 (서비스명: db / backend / frontend)
docker compose logs -f backend

# 정지 (데이터는 유지)
docker compose down

# 정지 + DB 볼륨까지 초기화
docker compose down -v

# 특정 서비스만 재빌드
docker compose build backend && docker compose up -d backend

# MySQL 접속 (컨테이너 안에서)
docker compose exec db mysql -uroot -pstudentpass students
```

## 접속 URL / 포트

| 서비스 | 호스트 포트 | 내부 포트 | 비고 |
|--------|-------------|-----------|------|
| 프론트엔드 (nginx) | 8080 | 80 | http://localhost:8080 |
| 백엔드 (Tomcat) | 8081 | 8080 | http://localhost:8081/ |
| DB (MySQL) | 3306 | 3306 | 디버깅용으로 노출, 운영에서는 닫기 권장 |

DB 계정은 `docker-compose.yml`에 평문으로 들어 있습니다 (수업 과제용). 실 운영이라면 `.env` 분리하세요.

## 디렉토리 구조

```
students/
├── backend/              Tomcat WAR 프로젝트 (Maven, 멀티스테이지 Dockerfile)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/...      Servlet / Service / DAO / JDBC
├── frontend/             정적 HTML/CSS/JS (nginx)
│   ├── Dockerfile
│   ├── index.html / register.html / scores.html / view.html
│   ├── css/
│   └── js/
├── db/
│   └── init.sql          최초 기동 시 자동 실행되는 스키마/초기데이터
├── docs/
│   ├── design.md         설계 문서
│   ├── presentation.md   발표 자료
│   └── screenshots/      실행 화면 캡처
├── docker-compose.yml
├── CLAUDE.md             Claude Code 세션용 컨텍스트 (자동 로드)
└── README.md
```

## 트러블슈팅

**`port is already allocated` — 8080/8081/3306이 이미 사용 중**
다른 프로세스가 점유 중입니다. `netstat -ano | findstr 8080` 으로 확인 후 종료하거나, `docker-compose.yml`의 호스트 포트를 다른 번호로 바꾸세요.

**`docker_engine: 시스템이 지정된 파일을 찾을 수 없습니다`**
Docker Desktop이 켜져 있지 않습니다. 작업표시줄의 Docker 아이콘을 확인하고 실행하세요. 부팅 직후라면 엔진 기동까지 30초~1분 정도 걸립니다.

**백엔드가 DB 연결 실패로 죽음**
첫 기동 시 MySQL 초기화가 끝나기 전에 백엔드가 붙으면 발생합니다. `docker-compose.yml`에 `depends_on.condition: service_healthy`가 걸려 있어 보통은 괜찮지만, 그래도 죽으면 `docker compose up -d backend`로 재기동하세요.

**스키마가 바뀌었는데 반영이 안 됨**
`db/init.sql`은 **볼륨이 비어 있을 때만** 실행됩니다. 스키마 변경 후엔 반드시 `docker compose down -v`로 볼륨까지 삭제하고 다시 올리세요.

**한글이 깨짐**
MySQL 컨테이너는 기본이 `utf8mb4`로 잡혀 있습니다. 깨진다면 클라이언트(IDE 등) 인코딩을 확인하세요.

## 작업 재개 (다른 컴퓨터로 옮긴 직후)

이 저장소는 Claude Code와 함께 작업하도록 구성되어 있습니다. 새 컴퓨터에서 클론한 직후:

```bash
git clone https://github.com/jynlee/student_grade_system.git
cd student_grade_system
docker compose up -d --build      # 환경 기동
claude                            # 프로젝트 루트에서 실행
```

`claude` 실행 시 루트의 `CLAUDE.md`가 자동으로 컨텍스트에 로드되므로, "이 프로젝트 뭐였더라" 설명 없이 바로 다음 작업을 지시할 수 있습니다. 예: "scores API에 검증 로직 추가해줘", "view.html 디자인 정리해줘".

상세 가이드와 진행 중인 작업 메모는 [`CLAUDE.md`](CLAUDE.md) 참고.

## 문서

- [설계 문서](docs/design.md)
- [발표 자료](docs/presentation.md)
- [실행 화면 캡처](docs/screenshots/)

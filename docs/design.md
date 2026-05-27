# 학생 정보 및 성적 관리 시스템 — 설계 문서

## 1. 프로젝트 개요

학생 정보를 등록하고 과목별 점수를 입력하면, 평균과 등급을 자동 계산해 조회할 수 있는 웹 기반 관리 시스템.

본 문서는 시스템의 아키텍처, 데이터 모델, API 설계, 화면 구성, 에러 처리 전략 및 빌드·배포 방법을 정리한 것이다.

**핵심 요구사항:**
- DB 설계: `students`, `scores` 테이블 구성
- 화면 구현: 학생 성적 등록 / 입력 / 결과 조회 UI
- 로직 구현: CRUD API + 등급 계산 알고리즘
- 인프라: Docker Compose 기반 컨테이너 아키텍처

---

## 2. 컨테이너 아키텍처

본 시스템은 3개의 컨테이너로 구성된다.

```
┌──────────────────────────────────────────────────────────────┐
│                       Docker Compose                          │
│                                                                │
│  ┌──────────────────────────┐    ┌────────────────┐           │
│  │  frontend (nginx)        │    │   backend      │           │
│  │  ─ 정적 파일 서빙          │────▶│   (Tomcat 9   │           │
│  │  ─ /api/* 리버스 프록시     │ HTTP │    + WAR)     │           │
│  │  호스트 포트 8080          │    │  내부 8080    │           │
│  └──────────────────────────┘    │  (호스트 8081 │           │
│            ▲                      │   = 디버깅용) │           │
│            │ 브라우저 단일 origin   └────────┬───────┘           │
│            │                              │ JDBC               │
│      [클라이언트]                          ▼                    │
│                                  ┌────────────────┐           │
│                                  │   db (MySQL 8) │           │
│                                  │   호스트 3306  │           │
│                                  └────────┬───────┘           │
│                                           │                    │
│                                           ▼                    │
│                                   ┌──────────────┐            │
│                                   │  mysql_data  │ (volume)   │
│                                   └──────────────┘            │
└──────────────────────────────────────────────────────────────┘
```

| 컨테이너 | 이미지 | 호스트 포트 | 역할 |
|---------|--------|------------|------|
| frontend | `nginx:alpine` | 8080 | 정적 HTML/CSS/JS 서빙 + `/api/*` → backend 리버스 프록시 |
| backend  | `tomcat:9-jdk17` | 8081 (디버깅용) | WAR 배포, REST API 제공 (내부 8080) |
| db       | `mysql:8.0` | 3306 (디버깅용) | 데이터 영속화 (볼륨 마운트) |

**통신 구조:**
- 브라우저는 모든 요청을 단일 origin `http://localhost:8080` 으로 보낸다.
  - 정적 파일은 nginx가 직접 서빙.
  - `/api/*` 는 nginx가 컨테이너 내부 `http://backend:8080/api/*` 로 리버스 프록시.
- 같은 origin이므로 **CORS 우회가 불필요**하다. 백엔드의 `Access-Control-Allow-Origin: *` 헤더는 직접 호출(8081) 디버깅 시를 위해 보험으로 유지.
- 백엔드는 컨테이너 내부 호스트명 `db:3306`으로 MySQL에 접속한다.
- 프론트엔드 코드(`api.js`)의 베이스 URL은 상대경로 `/api` 로 고정 — 배포 환경(호스트/도메인)이 바뀌어도 코드 수정 불필요.

**데이터 영속화:** `mysql_data` 명명 볼륨을 `/var/lib/mysql`에 마운트하여 컨테이너 재시작 후에도 데이터가 유지된다.

---

## 3. 데이터 모델

### 3.1 스키마

```sql
-- 학생 정보 테이블
CREATE TABLE students (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    student_number  VARCHAR(20)  NOT NULL UNIQUE,
    name            VARCHAR(50)  NOT NULL,
    department      VARCHAR(50)  NOT NULL,
    grade_year      INT          NOT NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_grade_year CHECK (grade_year BETWEEN 1 AND 4)
);

-- 성적 테이블
CREATE TABLE scores (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT       NOT NULL,
    subject     VARCHAR(20)  NOT NULL,
    score       INT          NOT NULL,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scores_student  FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT chk_subject        CHECK (subject IN ('국어','영어','수학','과학','사회')),
    CONSTRAINT chk_score_range    CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT uq_student_subject UNIQUE (student_id, subject)
);
```

### 3.2 설계 의도

| 결정 | 이유 |
|------|------|
| `student_number`을 `VARCHAR`로 | 학번이 0으로 시작 가능하며 학교마다 형식이 다르기 때문 |
| `UNIQUE(student_number)` | 학번 중복 방지 (DB 레벨에서 무결성 보장) |
| `ON DELETE CASCADE` | 학생 삭제 시 해당 학생의 성적도 자동 정리 (참조 무결성) |
| `CHECK` 제약 3종 | 과목명·점수 범위·학년 범위를 DB가 직접 강제 (최후 보루) |
| `UNIQUE(student_id, subject)` | 같은 학생이 같은 과목 점수를 중복 보유할 수 없도록 강제 → 갱신은 UPDATE 로만 |
| **등급 컬럼 미저장** | 점수에서 파생되는 값이므로 정규화 원칙상 저장하지 않고 SELECT 시 계산 |

---

## 4. 백엔드 아키텍처

### 4.1 3-Layer 구조

```
HTTP 요청
   │
   ▼
┌─────────────────────┐
│  Servlet (Controller)│  ← @WebServlet, 요청 파싱, JSON 응답, CORS
└─────────────────────┘
   │
   ▼
┌─────────────────────┐
│  Service             │  ← 비즈니스 로직 (등급 계산, 평균, 검증)
└─────────────────────┘
   │
   ▼
┌─────────────────────┐
│  DAO (JDBC)          │  ← SQL 실행, PreparedStatement
└─────────────────────┘
   │
   ▼
  MySQL
```

각 레이어는 명확한 책임 경계를 가지며 단독으로 이해·테스트할 수 있다.

### 4.2 패키지 구조

```
com.school.students
├── servlet/         StudentServlet / ScoreServlet / ReportServlet / SubjectServlet
├── service/         StudentService / ScoreService / ReportService / GradeCalculator
├── dao/             StudentDao / ScoreDao
├── model/           Student / Score / SubjectScore / StudentReport (DTO)
└── util/            DbConnection (JDBC) / JsonUtil (Jackson)
```

### 4.3 REST API

| Method | URL | 설명 |
|--------|-----|------|
| `GET`    | `/api/students`              | 전체 학생 목록 |
| `POST`   | `/api/students`              | 학생 등록 |
| `GET`    | `/api/students/{id}`         | 학생 1명 조회 |
| `PUT`    | `/api/students/{id}`         | 학생 수정 |
| `DELETE` | `/api/students/{id}`         | 학생 삭제 (성적 CASCADE 삭제) |
| `GET`    | `/api/students/{id}/scores`  | 한 학생의 전체 점수 |
| `POST`   | `/api/scores`                | 성적 입력 |
| `PUT`    | `/api/scores/{id}`           | 점수 수정 |
| `DELETE` | `/api/scores/{id}`           | 점수 삭제 |
| `GET`    | `/api/reports/{studentId}`   | 결과 조회 (학생+점수+평균+등급) |
| `GET`    | `/api/subjects`              | 5개 과목 리스트 |

### 4.4 등급 계산 알고리즘

```java
public static String calculateGrade(double score) {
    if (score >= 90) return "A";
    if (score >= 80) return "B";
    if (score >= 70) return "C";
    if (score >= 60) return "D";
    return "F";
}
```

이 메서드는 두 곳에서 재사용된다:
1. 과목별 점수 → 과목 등급
2. 전체 평균 → 종합 등급

**평균 산식 및 반올림 규칙:**

```
평균 = (입력된 과목 점수의 합) / (입력된 과목 수)
      → 소수점 둘째자리에서 반올림하여 소수점 첫째자리까지 표시
```

- 평균은 **입력된 점수만**을 기준으로 한다. 5과목 중 3과목만 입력했다면 분모는 3.
- 반올림은 `Math.round(sum / n * 10.0) / 10.0` 으로 구현 — 예: `89.95 → 90.0` (등급 A), `89.94 → 89.9` (등급 B).
- 점수가 1개도 없는 학생은 평균 `null`, 종합 등급 `"-"` 로 표시한다.
- 등급 경계값은 **이상(≥)** 으로 처리한다. 예: 평균 `90.0` → A, 평균 `89.9` → B.

> 반올림이 등급 경계를 넘어가는 경우(예: 89.95 → 90.0 → A)는 의도된 동작이다. 점수 입력은 정수만 허용되므로 이 경계 이슈가 발생할 수 있는 입력 조합은 제한적이다.

---

## 5. 프론트엔드 구조

### 5.1 파일 구성

```
frontend/
├── index.html        ← 메인 (3개 화면 링크)
├── register.html     ← 학생 등록 + 학생 목록
├── scores.html       ← 성적 입력
├── view.html         ← 결과 조회
├── css/style.css
└── js/
    ├── api.js        ← fetch 래퍼 공통 함수
    ├── register.js
    ├── scores.js
    └── view.js
```

### 5.2 화면별 동작

| 화면 | 주요 동작 |
|------|-----------|
| index.html | 랜딩 페이지, 3개 화면으로의 링크 + 등급 기준 안내 |
| register.html | 학생 등록 폼 + 등록된 학생 목록 표시 + 삭제 |
| scores.html | 학생/과목 select + 점수 입력. 중복 시 PUT으로 갱신 다이얼로그 |
| view.html | 학생 select 후 조회. 과목별 등급 표 + 평균·종합 등급 표시 |

### 5.3 공통 UX

- 모든 화면 상단에 동일한 네비게이션 바 (홈 + 3개 화면)
- 등급 배지는 색상 5종 (A 녹색, B 청색, C 주황, D 진주황, F 빨강)
- 에러는 상단 빨간색 메시지 영역에 표시

---

## 6. 에러 처리 및 검증

### 6.1 다층 방어

검증은 3개 레이어에서 중복으로 이루어진다.

| 레이어 | 검증 내용 |
|--------|-----------|
| 프론트엔드 (JS) | 즉시 피드백 (빈칸, 숫자 형식) |
| 백엔드 Service | 비즈니스 규칙 (점수 0~100, 학년 1~4, 과목 5개 중 하나) |
| DB (CHECK / UNIQUE / FK) | 최후 보루 |

### 6.2 HTTP 상태 코드

| 코드 | 의미 |
|------|------|
| 200 | 정상 GET / PUT / DELETE |
| 201 | POST 성공 (Location 헤더 포함) |
| 400 | 입력 검증 실패 |
| 404 | 리소스 없음 |
| 409 | 학번 중복 또는 학생+과목 중복 |
| 500 | JDBC 예외, 예상치 못한 에러 |

### 6.3 에러 응답 포맷

```json
{
  "error": "VALIDATION_FAILED",
  "message": "점수는 0~100 사이여야 합니다."
}
```

### 6.4 주요 엣지 케이스

1. **학번 중복 등록 시도** → DB UNIQUE 위반 catch → 409
2. **학생+과목 중복 입력** → 409 응답 → 프론트가 PUT으로 갱신 다이얼로그
3. **학생 삭제** → CASCADE로 성적 자동 삭제 (UI 확인 다이얼로그)
4. **점수 0개 학생 조회** → 평균 `null`, 등급 `"-"`
5. **부분 입력 학생 (5과목 중 3개)** → 입력된 과목만으로 평균·등급 계산 + "입력 3/5" 표시
6. **DB 연결 실패** → 백엔드 startup 시 최대 30초간 재시도 루프
7. **CORS preflight (OPTIONS)** → 모든 서블릿이 OPTIONS 처리

### 6.5 JDBC 모범 사례

- 모든 쿼리는 `PreparedStatement` + 파라미터 바인딩 (SQL Injection 방지)
- `try-with-resources` 로 Connection/PreparedStatement/ResultSet 자동 close (리소스 누수 방지)

---

## 7. 빌드 및 배포

### 7.1 빌드 도구

- **Maven** (WAR 패키징)
- 주요 의존성: `javax.servlet-api` (provided) / `mysql-connector-j` / `jackson-databind`

### 7.2 환경 변수

백엔드는 다음 환경변수로 DB 정보를 받는다 (`docker-compose.yml`에서 주입):
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

설정값을 코드에 하드코딩하지 않으므로 운영 환경에서 자연스럽게 분리할 수 있다.

### 7.3 실행

```bash
docker compose up --build
# frontend: http://localhost:8080
# backend:  http://localhost:8081/api/...
```

### 7.4 시드 데이터

`db/init.sql` 은 컨테이너 최초 기동 시 자동 실행되며, 학생 4명과 다양한 점수 데이터를 미리 넣어 다양한 케이스 시연이 가능하도록 한다.

---

## 8. 수업에서 배운 내용 적용 포인트

### 8.1 데이터베이스
- 정규화 (등급은 파생값이므로 저장하지 않음)
- 외래키 + ON DELETE CASCADE 로 참조 무결성
- UNIQUE 제약으로 비즈니스 규칙 강제
- CHECK 제약으로 도메인 무결성

### 8.2 Java / JDBC
- 3-Layer 아키텍처로 책임 분리
- `PreparedStatement` 파라미터 바인딩 (SQL Injection 방지)
- `try-with-resources` 로 리소스 누수 방지
- 등급 계산 로직의 재사용 (한 메서드 → 두 호출 지점)

### 8.3 웹 / REST
- 리소스 중심 URL 설계 (`/api/students/{id}/scores`)
- HTTP 메서드 의미 (GET/POST/PUT/DELETE)
- HTTP 상태 코드 의미 (200/201/400/404/409/500)
- CORS 의 원리와 처리

### 8.4 컨테이너 / 인프라
- Docker Compose 다중 컨테이너 오케스트레이션
- 볼륨 마운트로 데이터 영속화
- 컨테이너 간 호스트명 통신
- 환경변수로 설정 분리

---

## 9. 향후 개선 (YAGNI 로 뺀 항목)

- 인증/인가 (로그인, 세션)
- 페이지네이션·검색·필터
- 점수 변경 이력 추적
- 단위 테스트 (JUnit + Mockito)
- Connection Pool (HikariCP)
- HTTPS / Secrets 매니저

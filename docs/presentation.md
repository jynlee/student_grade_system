# 학생 성적 관리 시스템 — 발표 자료

> 교과목: 중간고사 프로젝트
> 발표자: (이름 기입)

---

## 슬라이드 1. 프로젝트 소개

**학생 정보 및 성적 관리 시스템**

- 학생 등록 / 성적 입력 / 결과 조회 가 가능한 웹 시스템
- 평균과 등급을 자동 계산해 한눈에 확인
- Docker Compose 로 3개 컨테이너 (frontend / backend / db) 오케스트레이션

---

## 슬라이드 2. 기술 스택

| 영역 | 기술 |
|------|------|
| 프론트엔드 | HTML5 / CSS3 / Vanilla JavaScript (nginx 서빙) |
| 백엔드 | Java 17 / Servlet 4.0 / Apache Tomcat 9 / Maven |
| DB | MySQL 8.0 (JDBC) |
| 인프라 | Docker Compose |

**핵심 선택 이유:**
- Plain Java + Servlet → 프레임워크 의존 없이 Servlet API 표준 그대로 학습
- nginx → 정적 파일 서빙의 표준
- JDBC + PreparedStatement → SQL 동작을 가시적으로 다룸

---

## 슬라이드 3. 컨테이너 아키텍처

```
[Browser]
   │ HTTP
   ▼
[frontend: nginx]  ──fetch──▶  [backend: Tomcat + WAR]
                                       │ JDBC
                                       ▼
                                [db: MySQL]
                                       │
                                       ▼
                               (mysql_data 볼륨)
```

- frontend → backend → db 의 단방향 의존
- 컨테이너 간 통신은 서비스명 (`db:3306`) 로 (네트워크 격리)
- `mysql_data` 볼륨 마운트로 데이터 영속화

---

## 슬라이드 4. 데이터 모델

**2개 테이블 (1:N 관계)**

```
students ──(1)──┐
                │ FK (CASCADE)
                ▼
              scores ──(N)
```

**무결성 제약 4종:**

| 제약 | 효과 |
|------|------|
| **UNIQUE(student_number)** | 학번 중복 방지 |
| **FK ON DELETE CASCADE** | 학생 삭제 시 성적 자동 정리 |
| **CHECK** (3개) | 과목 5개 / 점수 0~100 / 학년 1~4 강제 |
| **UNIQUE(student_id, subject)** | 같은 학생의 같은 과목 중복 방지 |

→ 비즈니스 규칙을 코드뿐 아니라 DB 가 함께 보장 ("최후 보루" 다층 방어)

---

## 슬라이드 5. 백엔드 3-Layer 아키텍처

```
Servlet (Controller)
   ↓  요청/응답·CORS·JSON
Service (비즈니스 로직)
   ↓  검증·등급 계산·평균
DAO (JDBC)
   ↓  SQL·PreparedStatement
MySQL
```

**책임 분리의 효과:**
- 각 레이어가 단독으로 이해/수정 가능
- 등급 계산 로직 (`GradeCalculator`) 은 한 곳에 → 과목별·평균 두 곳에서 호출 (DRY)
- 향후 단위 테스트 시 Service 만 격리 테스트 가능

---

## 슬라이드 6. REST API 설계

- **리소스 중심 URL**: `/api/students/{id}/scores`
- **HTTP 메서드**: GET / POST / PUT / DELETE 의미를 그대로 사용
- **상태 코드 일관 사용**: 200 / 201 / 400 / 404 / 409 / 500
- **통합 조회 엔드포인트**: `GET /api/reports/{studentId}` → 학생 정보 + 과목별 점수·등급 + 평균·등급 을 한 번에 반환 (프론트 fetch 횟수 ↓)

---

## 슬라이드 7. 등급 계산 알고리즘

**5단계 절대평가 (A / B / C / D / F)**

```java
public static String calculateGrade(double score) {
    if (score >= 90) return "A";
    if (score >= 80) return "B";
    if (score >= 70) return "C";
    if (score >= 60) return "D";
    return "F";
}
```

- 정적 메서드 → 어디서나 호출 가능, 상태 없음
- 두 호출 지점: ① 과목별 점수 → 과목 등급 ② 평균 → 종합 등급
- 평균은 입력된 과목 수로 계산 (0개면 `null` → "-")

---

## 슬라이드 8. 수업에서 배운 내용 적용 포인트

### DB
- 정규화 (등급은 파생값이므로 저장하지 않음)
- FK + CASCADE, UNIQUE, CHECK 로 다층 무결성

### Java
- 3-Layer 패턴
- `PreparedStatement` 로 SQL Injection 방지
- `try-with-resources` 로 리소스 누수 방지

### 웹
- CORS 의 원리와 처리 (BaseServlet 의 OPTIONS 처리)
- HTTP 메서드/상태코드의 의미

### 컨테이너
- Docker Compose 다중 컨테이너
- 볼륨 마운트로 영속화
- 환경변수로 설정 분리

---

## 슬라이드 9. 트러블슈팅 경험

### 1. MySQL ready 전에 백엔드 시작
- **증상:** backend 컨테이너가 SQLException 으로 실패
- **해결:** docker-compose `healthcheck` + `DbConnection.waitUntilReady()` 30초 재시도 루프

### 2. CORS preflight (OPTIONS)
- **증상:** 브라우저가 fetch 전에 OPTIONS 요청을 보내는데 405 응답
- **해결:** `BaseServlet.service()` 에서 OPTIONS 를 200 으로 즉시 응답

### 3. UNIQUE 위반의 SQLException 을 사용자 메시지로 변환
- **증상:** 학번 중복 시 사용자에게 SQL 에러 메시지가 노출됨
- **해결:** `SQLIntegrityConstraintViolationException` 별도 catch → 409 + "이미 사용 중인 학번입니다."

### 4. 한글 문자열 인코딩
- **증상:** JSON 응답에서 한글 깨짐
- **해결:** `resp.setCharacterEncoding("UTF-8")` + JDBC URL `characterEncoding=utf8`

---

## 슬라이드 10. 향후 개선

- 인증/인가 (현재는 미구현)
- 페이지네이션·검색·필터
- 단위 테스트 (JUnit + Mockito)
- Connection Pool (HikariCP)
- HTTPS / 환경변수 → secrets 매니저
- 점수 변경 이력 (audit log) 추적

---

## 슬라이드 11. 데모 시연

(브라우저 화면 시연 또는 캡처 — `docs/screenshots/` 참조)

1. **홈** → 시스템 소개와 등급 기준 확인
2. **학생 등록** → 학번 중복 시 409 응답 + UI 메시지 시연
3. **성적 입력** → 학생 4명 + 5과목 입력, 중복 시 PUT 갱신 다이얼로그
4. **결과 조회** → 김철수 (5과목 모두), 이영희 (부분), 최지은 (0개) 케이스 시연
5. **학생 삭제** → CASCADE 로 성적도 함께 삭제됨을 확인

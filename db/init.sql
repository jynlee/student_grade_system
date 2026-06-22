-- =============================================================================
-- 학생 성적 관리 시스템 — 초기 스키마 및 시드 데이터
-- 컨테이너 최초 기동 시 자동 실행됨 (MySQL 공식 이미지의 /docker-entrypoint-initdb.d/ 메커니즘)
-- =============================================================================

-- MySQL 클라이언트(Docker 엔트리포인트 포함) charset을 utf8mb4로 강제
-- 이 설정이 없으면 한글 데이터가 latin1로 해석되어 이중 인코딩됨
SET NAMES utf8mb4;

-- 데이터베이스 생성 및 선택
CREATE DATABASE IF NOT EXISTS students
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE students;

-- =============================================================================
-- 학생 정보 테이블
-- =============================================================================
-- 한 행 = 학생 1명
-- student_number(학번)은 UNIQUE 제약으로 중복 방지
-- grade_year는 CHECK 제약으로 1~4학년만 허용
-- =============================================================================
CREATE TABLE IF NOT EXISTS students (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    student_number  VARCHAR(20)  NOT NULL UNIQUE COMMENT '학번 (예: 20240001)',
    name            VARCHAR(50)  NOT NULL        COMMENT '이름',
    department      VARCHAR(50)  NOT NULL        COMMENT '학과',
    grade_year      INT          NOT NULL        COMMENT '학년 (1~4)',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_grade_year CHECK (grade_year BETWEEN 1 AND 4)
);

-- =============================================================================
-- 성적 테이블
-- =============================================================================
-- 한 행 = 학생 1명의 1과목 점수
-- student_id는 students.id를 참조하는 FK (학생 삭제 시 점수도 자동 삭제)
-- subject는 5개 고정 과목 중 하나만 허용 (CHECK 제약)
-- score는 0~100 범위 (CHECK 제약)
-- (student_id, subject) 조합은 UNIQUE → 같은 학생의 같은 과목 중복 입력 방지
-- =============================================================================
CREATE TABLE IF NOT EXISTS scores (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT       NOT NULL,
    subject     VARCHAR(20)  NOT NULL    COMMENT '과목명',
    score       INT          NOT NULL    COMMENT '점수 (0~100)',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scores_student  FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT chk_subject        CHECK (subject IN ('국어','영어','수학','과학','사회')),
    CONSTRAINT chk_score_range    CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT uq_student_subject UNIQUE (student_id, subject)
);

-- =============================================================================
-- 시드 데이터 (시연용)
-- =============================================================================
-- 학생 4명: 다양한 학과/학년
-- 점수 분포: 일부 학생은 5과목 모두, 일부는 부분 입력 → 다양한 케이스 시연 가능
-- =============================================================================
INSERT INTO students (student_number, name, department, grade_year) VALUES
    ('20240001', '김철수', '컴퓨터공학과', 2),
    ('20240002', '이영희', '경영학과',     1),
    ('20230015', '박민수', '컴퓨터공학과', 3),
    ('20220078', '최지은', '전자공학과',   4);

-- 김철수 (id=1): 5과목 모두
INSERT INTO scores (student_id, subject, score) VALUES
    (1, '국어', 92),
    (1, '영어', 85),
    (1, '수학', 78),
    (1, '과학', 88),
    (1, '사회', 95);

-- 이영희 (id=2): 3과목만 (부분 입력 케이스)
INSERT INTO scores (student_id, subject, score) VALUES
    (2, '국어', 72),
    (2, '영어', 65),
    (2, '수학', 58);

-- 박민수 (id=3): 5과목 모두 (낮은 점수 분포)
INSERT INTO scores (student_id, subject, score) VALUES
    (3, '국어', 55),
    (3, '영어', 60),
    (3, '수학', 45),
    (3, '과학', 70),
    (3, '사회', 65);

-- 최지은 (id=4): 점수 0개 (조회 시 등급 '-' 케이스 시연용)
-- 의도적으로 점수 데이터 없음

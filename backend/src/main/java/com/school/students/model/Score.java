package com.school.students.model;

/**
 * 성적 모델.
 * scores 테이블의 한 행에 대응한다.
 *
 * - id: DB의 자동 증가 PK
 * - studentId: 학생 FK
 * - subject: 과목명 (5개 고정값 중 하나)
 * - score: 점수 (0~100)
 */
public class Score {
    private Long id;
    private Long studentId;
    private String subject;
    private Integer score;

    public Score() {}

    public Score(Long id, Long studentId, String subject, Integer score) {
        this.id = id;
        this.studentId = studentId;
        this.subject = subject;
        this.score = score;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}

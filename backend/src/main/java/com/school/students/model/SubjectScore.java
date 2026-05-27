package com.school.students.model;

/**
 * 결과 조회 화면에 표시되는 한 행을 위한 DTO.
 * "이 학생의 이 과목은 X점이며 등급은 Y이다" 라는 정보를 묶어서 표현.
 *
 * GradeCalculator가 score를 받아 grade를 계산해 채워준다.
 */
public class SubjectScore {
    private String subject;
    private Integer score;
    private String grade;

    public SubjectScore() {}

    public SubjectScore(String subject, Integer score, String grade) {
        this.subject = subject;
        this.score = score;
        this.grade = grade;
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}

package com.school.students.model;

import java.util.List;

/**
 * 결과 조회용 통합 DTO.
 *
 * 한 학생의 정보 + 과목별 점수·등급 리스트 + 전체 평균·등급 을 묶어서 반환한다.
 * GET /api/reports/{studentId} 의 응답 형식이 이 클래스의 JSON 직렬화 결과.
 *
 * 평균이 null일 때 (= 입력된 점수가 0개) overallGrade 는 "-" 가 들어간다.
 */
public class StudentReport {
    private Student student;
    private List<SubjectScore> subjectScores;
    private Double average;       // 점수가 0개면 null
    private String overallGrade;  // null 평균이면 "-"

    public StudentReport() {}

    public StudentReport(Student student, List<SubjectScore> subjectScores,
                         Double average, String overallGrade) {
        this.student = student;
        this.subjectScores = subjectScores;
        this.average = average;
        this.overallGrade = overallGrade;
    }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public List<SubjectScore> getSubjectScores() { return subjectScores; }
    public void setSubjectScores(List<SubjectScore> subjectScores) { this.subjectScores = subjectScores; }

    public Double getAverage() { return average; }
    public void setAverage(Double average) { this.average = average; }

    public String getOverallGrade() { return overallGrade; }
    public void setOverallGrade(String overallGrade) { this.overallGrade = overallGrade; }
}

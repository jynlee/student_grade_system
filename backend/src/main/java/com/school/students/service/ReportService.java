package com.school.students.service;

import com.school.students.dao.ScoreDao;
import com.school.students.dao.StudentDao;
import com.school.students.model.Score;
import com.school.students.model.Student;
import com.school.students.model.StudentReport;
import com.school.students.model.SubjectScore;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 결과 조회 서비스 — 학생 정보 + 과목별 점수·등급 + 평균·등급 을 묶어 반환.
 *
 * GET /api/reports/{studentId} 의 핵심 로직.
 * 점수가 1개도 없는 학생은 평균 null, overallGrade "-" 로 채워 반환.
 */
public class ReportService {

    private final StudentDao studentDao = new StudentDao();
    private final ScoreDao scoreDao = new ScoreDao();

    public Optional<StudentReport> build(long studentId) throws SQLException {
        Optional<Student> studentOpt = studentDao.findById(studentId);
        if (studentOpt.isEmpty()) return Optional.empty();
        Student student = studentOpt.get();

        List<Score> scores = scoreDao.findByStudentId(studentId);

        // 각 과목 점수에 등급 부여
        List<SubjectScore> subjectScores = new ArrayList<>();
        List<Integer> rawScores = new ArrayList<>();
        for (Score s : scores) {
            String grade = GradeCalculator.calculateGrade(s.getScore());
            subjectScores.add(new SubjectScore(s.getSubject(), s.getScore(), grade));
            rawScores.add(s.getScore());
        }

        // 평균 및 전체 등급
        Double average = GradeCalculator.average(rawScores);
        String overallGrade = (average == null) ? "-" : GradeCalculator.calculateGrade(average);

        return Optional.of(new StudentReport(student, subjectScores, average, overallGrade));
    }
}

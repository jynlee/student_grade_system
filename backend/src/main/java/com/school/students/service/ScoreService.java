package com.school.students.service;

import com.school.students.dao.ScoreDao;
import com.school.students.dao.StudentDao;
import com.school.students.model.Score;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 성적 도메인 비즈니스 로직.
 *
 * - 학생 존재 확인 (FK 제약 위반을 미리 방지)
 * - 과목 5개 검증
 * - 점수 0~100 범위 검증
 */
public class ScoreService {

    /** 5개 고정 과목. SubjectServlet 에서도 이 리스트를 직접 사용. */
    public static final List<String> SUBJECTS =
        Arrays.asList("국어", "영어", "수학", "과학", "사회");

    private final ScoreDao scoreDao = new ScoreDao();
    private final StudentDao studentDao = new StudentDao();

    public List<Score> listByStudent(long studentId) throws SQLException {
        return scoreDao.findByStudentId(studentId);
    }

    public Optional<Score> get(long id) throws SQLException {
        return scoreDao.findById(id);
    }

    /**
     * 성적 신규 입력. 학생이 존재하지 않으면 IllegalArgumentException.
     * 같은 학생+과목 중복은 DAO에서 UNIQUE 위반 → Servlet 이 catch 해서 409 로 변환.
     */
    public Score create(Score s) throws SQLException {
        validate(s);
        if (studentDao.findById(s.getStudentId()).isEmpty()) {
            throw new IllegalArgumentException("해당 학생을 찾을 수 없습니다. (studentId=" + s.getStudentId() + ")");
        }
        return scoreDao.insert(s);
    }

    public boolean updateScore(long id, int newScore) throws SQLException {
        if (newScore < 0 || newScore > 100) {
            throw new IllegalArgumentException("점수는 0~100 사이여야 합니다.");
        }
        return scoreDao.updateScore(id, newScore) > 0;
    }

    public boolean delete(long id) throws SQLException {
        return scoreDao.delete(id) > 0;
    }

    public Optional<Score> findByStudentAndSubject(long studentId, String subject) throws SQLException {
        return scoreDao.findByStudentAndSubject(studentId, subject);
    }

    private void validate(Score s) {
        if (s == null) throw new IllegalArgumentException("성적 정보가 비어있습니다.");
        if (s.getStudentId() == null) throw new IllegalArgumentException("studentId 가 필요합니다.");
        if (s.getSubject() == null || !SUBJECTS.contains(s.getSubject())) {
            throw new IllegalArgumentException("과목은 다음 중 하나여야 합니다: " + SUBJECTS);
        }
        if (s.getScore() == null || s.getScore() < 0 || s.getScore() > 100) {
            throw new IllegalArgumentException("점수는 0~100 사이여야 합니다.");
        }
    }
}

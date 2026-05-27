package com.school.students.dao;

import com.school.students.model.Score;
import com.school.students.util.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 성적 DAO.
 *
 * 모든 쿼리는 PreparedStatement 사용 (SQL Injection 방지).
 * try-with-resources 로 리소스 자동 close.
 */
public class ScoreDao {

    /** 특정 학생의 모든 점수를 과목 표시 순서대로 반환. */
    public List<Score> findByStudentId(long studentId) throws SQLException {
        // 과목 정렬: 국어, 영어, 수학, 과학, 사회 순으로 표시 (FIELD() 사용)
        String sql = "SELECT id, student_id, subject, score FROM scores " +
                     "WHERE student_id = ? " +
                     "ORDER BY FIELD(subject, '국어','영어','수학','과학','사회')";
        List<Score> list = new ArrayList<>();
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** id로 점수 1개 조회. */
    public Optional<Score> findById(long id) throws SQLException {
        String sql = "SELECT id, student_id, subject, score FROM scores WHERE id = ?";
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /**
     * 성적 신규 입력.
     * (student_id, subject) UNIQUE 위반 → SQLIntegrityConstraintViolationException.
     * CHECK 제약 위반 (과목 5개 외, 점수 범위 외) → SQLException.
     * 호출자가 catch 후 409 또는 400 으로 변환.
     */
    public Score insert(Score s) throws SQLException {
        String sql = "INSERT INTO scores (student_id, subject, score) VALUES (?, ?, ?)";
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, s.getStudentId());
            ps.setString(2, s.getSubject());
            ps.setInt(3, s.getScore());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getLong(1));
            }
        }
        return s;
    }

    /** 점수 수정 (점수값만 변경). */
    public int updateScore(long id, int newScore) throws SQLException {
        String sql = "UPDATE scores SET score = ? WHERE id = ?";
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, newScore);
            ps.setLong(2, id);
            return ps.executeUpdate();
        }
    }

    /** 점수 삭제. */
    public int delete(long id) throws SQLException {
        String sql = "DELETE FROM scores WHERE id = ?";
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    /**
     * 학생 + 과목으로 기존 점수 찾기 (중복 입력 시 PUT으로 갱신할 대상 id를 찾기 위함).
     */
    public Optional<Score> findByStudentAndSubject(long studentId, String subject) throws SQLException {
        String sql = "SELECT id, student_id, subject, score FROM scores " +
                     "WHERE student_id = ? AND subject = ?";
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setString(2, subject);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private Score map(ResultSet rs) throws SQLException {
        return new Score(
            rs.getLong("id"),
            rs.getLong("student_id"),
            rs.getString("subject"),
            rs.getInt("score")
        );
    }
}

package com.school.students.dao;

import com.school.students.model.Student;
import com.school.students.util.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 학생 정보 DAO.
 *
 * JDBC를 직접 사용하며, 모든 쿼리는 PreparedStatement + 파라미터 바인딩으로 작성한다.
 * (SQL Injection 방지)
 *
 * 모든 메서드는 try-with-resources 로 Connection/PreparedStatement/ResultSet 을
 * 명시적으로 닫아 리소스 누수를 방지한다.
 */
public class StudentDao {

    /** 전체 학생 목록을 student_number 오름차순으로 반환. */
    public List<Student> findAll() throws SQLException {
        String sql = "SELECT id, student_number, name, department, grade_year " +
                     "FROM students ORDER BY student_number";
        List<Student> list = new ArrayList<>();
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /** id로 학생 1명 조회. */
    public Optional<Student> findById(long id) throws SQLException {
        String sql = "SELECT id, student_number, name, department, grade_year " +
                     "FROM students WHERE id = ?";
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /**
     * 학생 신규 등록.
     * 학번(student_number) UNIQUE 위반 시 SQLIntegrityConstraintViolationException 발생.
     * 호출자가 catch 후 409 Conflict 로 변환해야 한다.
     *
     * @return 생성된 학생 (id 포함)
     */
    public Student insert(Student s) throws SQLException {
        String sql = "INSERT INTO students (student_number, name, department, grade_year) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getStudentNumber());
            ps.setString(2, s.getName());
            ps.setString(3, s.getDepartment());
            ps.setInt(4, s.getGradeYear());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getLong(1));
            }
        }
        return s;
    }

    /** 학생 정보 수정. 영향 받은 행 수를 반환 (0이면 해당 id 없음). */
    public int update(long id, Student s) throws SQLException {
        String sql = "UPDATE students SET student_number = ?, name = ?, department = ?, grade_year = ? " +
                     "WHERE id = ?";
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getStudentNumber());
            ps.setString(2, s.getName());
            ps.setString(3, s.getDepartment());
            ps.setInt(4, s.getGradeYear());
            ps.setLong(5, id);
            return ps.executeUpdate();
        }
    }

    /**
     * 학생 삭제.
     * FK ON DELETE CASCADE 에 의해 해당 학생의 scores도 함께 자동 삭제된다.
     */
    public int delete(long id) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    /** ResultSet 의 현재 행을 Student 객체로 매핑. */
    private Student map(ResultSet rs) throws SQLException {
        return new Student(
            rs.getLong("id"),
            rs.getString("student_number"),
            rs.getString("name"),
            rs.getString("department"),
            rs.getInt("grade_year")
        );
    }
}

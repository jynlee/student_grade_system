package com.school.students.service;

import com.school.students.dao.StudentDao;
import com.school.students.model.Student;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 학생 도메인 비즈니스 로직.
 *
 * - 입력 검증 (필드 누락, 학년 범위, 학번 형식)
 * - DAO 호출 및 결과 반환
 *
 * 검증 실패는 IllegalArgumentException 으로 throw → Servlet 이 catch 해서 400 응답으로 변환.
 */
public class StudentService {

    private final StudentDao dao = new StudentDao();

    public List<Student> listAll() throws SQLException {
        return dao.findAll();
    }

    public Optional<Student> get(long id) throws SQLException {
        return dao.findById(id);
    }

    public Student create(Student s) throws SQLException {
        validate(s);
        return dao.insert(s);
    }

    /** @return true 면 수정 성공, false 면 해당 id 없음 */
    public boolean update(long id, Student s) throws SQLException {
        validate(s);
        return dao.update(id, s) > 0;
    }

    /** @return true 면 삭제 성공, false 면 해당 id 없음 */
    public boolean delete(long id) throws SQLException {
        return dao.delete(id) > 0;
    }

    /**
     * 입력 필드 검증.
     * DB CHECK 제약과 중복되지만, 사용자 친화적인 에러 메시지를 제공하기 위해 Service 에서도 검증.
     */
    private void validate(Student s) {
        if (s == null) throw new IllegalArgumentException("학생 정보가 비어있습니다.");
        if (isBlank(s.getStudentNumber())) throw new IllegalArgumentException("학번을 입력하세요.");
        if (isBlank(s.getName()))          throw new IllegalArgumentException("이름을 입력하세요.");
        if (isBlank(s.getDepartment()))    throw new IllegalArgumentException("학과를 입력하세요.");
        if (s.getGradeYear() == null || s.getGradeYear() < 1 || s.getGradeYear() > 4) {
            throw new IllegalArgumentException("학년은 1~4 사이여야 합니다.");
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}

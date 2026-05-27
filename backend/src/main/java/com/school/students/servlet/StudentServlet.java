package com.school.students.servlet;

import com.school.students.model.Score;
import com.school.students.model.Student;
import com.school.students.service.ScoreService;
import com.school.students.service.StudentService;
import com.school.students.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 학생 CRUD 서블릿.
 *
 * 매핑 URL:
 *   GET    /api/students            전체 목록
 *   POST   /api/students            등록
 *   GET    /api/students/{id}       1명 조회
 *   PUT    /api/students/{id}       수정
 *   DELETE /api/students/{id}       삭제
 *   GET    /api/students/{id}/scores  해당 학생의 점수 목록
 *
 * /api/students 와 /api/students/* 두 패턴을 모두 매핑해 단일 서블릿이 처리.
 */
@WebServlet(urlPatterns = {"/api/students", "/api/students/*"})
public class StudentServlet extends BaseServlet {

    private final StudentService studentService = new StudentService();
    private final ScoreService scoreService = new ScoreService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long id = extractIdFromPath(req);
            String sub = extractSubResource(req);

            if (id == null) {
                // GET /api/students — 전체 목록
                List<Student> list = studentService.listAll();
                writeJson(resp, 200, list);
                return;
            }

            if ("scores".equals(sub)) {
                // GET /api/students/{id}/scores
                if (studentService.get(id).isEmpty()) {
                    writeError(resp, 404, "NOT_FOUND", "학생을 찾을 수 없습니다.");
                    return;
                }
                List<Score> scores = scoreService.listByStudent(id);
                writeJson(resp, 200, scores);
                return;
            }

            // GET /api/students/{id}
            Optional<Student> student = studentService.get(id);
            if (student.isPresent()) writeJson(resp, 200, student.get());
            else writeError(resp, 404, "NOT_FOUND", "학생을 찾을 수 없습니다.");

        } catch (SQLException e) {
            e.printStackTrace();
            writeError(resp, 500, "DB_ERROR", "데이터베이스 오류: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Student input = JsonUtil.fromJson(req.getInputStream(), Student.class);
            Student created = studentService.create(input);
            resp.setHeader("Location", "/api/students/" + created.getId());
            writeJson(resp, 201, created);
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, "VALIDATION_FAILED", e.getMessage());
        } catch (SQLIntegrityConstraintViolationException e) {
            // 학번 UNIQUE 위반
            writeError(resp, 409, "DUPLICATE_STUDENT_NUMBER", "이미 사용 중인 학번입니다.");
        } catch (SQLException e) {
            e.printStackTrace();
            writeError(resp, 500, "DB_ERROR", "데이터베이스 오류: " + e.getMessage());
        } catch (IOException e) {
            writeError(resp, 400, "INVALID_JSON", "요청 본문이 올바른 JSON 이 아닙니다.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = extractIdFromPath(req);
        if (id == null) {
            writeError(resp, 400, "MISSING_ID", "수정할 학생 id 가 URL 에 없습니다.");
            return;
        }
        try {
            Student input = JsonUtil.fromJson(req.getInputStream(), Student.class);
            boolean ok = studentService.update(id, input);
            if (ok) writeJson(resp, 200, input);
            else writeError(resp, 404, "NOT_FOUND", "학생을 찾을 수 없습니다.");
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, "VALIDATION_FAILED", e.getMessage());
        } catch (SQLIntegrityConstraintViolationException e) {
            writeError(resp, 409, "DUPLICATE_STUDENT_NUMBER", "이미 사용 중인 학번입니다.");
        } catch (SQLException e) {
            e.printStackTrace();
            writeError(resp, 500, "DB_ERROR", "데이터베이스 오류: " + e.getMessage());
        } catch (IOException e) {
            writeError(resp, 400, "INVALID_JSON", "요청 본문이 올바른 JSON 이 아닙니다.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = extractIdFromPath(req);
        if (id == null) {
            writeError(resp, 400, "MISSING_ID", "삭제할 학생 id 가 URL 에 없습니다.");
            return;
        }
        try {
            boolean ok = studentService.delete(id);
            if (ok) writeJson(resp, 200, java.util.Map.of("deleted", id));
            else writeError(resp, 404, "NOT_FOUND", "학생을 찾을 수 없습니다.");
        } catch (SQLException e) {
            e.printStackTrace();
            writeError(resp, 500, "DB_ERROR", "데이터베이스 오류: " + e.getMessage());
        }
    }
}

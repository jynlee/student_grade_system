package com.school.students.servlet;

import com.school.students.model.StudentReport;
import com.school.students.service.ReportService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * 결과 조회 서블릿.
 *
 * GET /api/reports/{studentId}
 *   → 학생 정보 + 과목별 점수·등급 + 평균·등급 통합 응답
 */
@WebServlet(urlPatterns = {"/api/reports/*"})
public class ReportServlet extends BaseServlet {

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long studentId = extractIdFromPath(req);
        if (studentId == null) {
            writeError(resp, 400, "MISSING_ID", "URL 에 studentId 가 필요합니다. 예: /api/reports/1");
            return;
        }
        try {
            Optional<StudentReport> report = reportService.build(studentId);
            if (report.isPresent()) writeJson(resp, 200, report.get());
            else writeError(resp, 404, "NOT_FOUND", "학생을 찾을 수 없습니다.");
        } catch (SQLException e) {
            e.printStackTrace();
            writeError(resp, 500, "DB_ERROR", "데이터베이스 오류: " + e.getMessage());
        }
    }
}

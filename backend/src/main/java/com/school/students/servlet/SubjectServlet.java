package com.school.students.servlet;

import com.school.students.service.ScoreService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 과목 목록 제공 서블릿.
 *
 * GET /api/subjects → 5개 고정 과목 리스트 반환 (프론트의 select 박스 채우기용)
 *
 * 동적으로 변하지 않으므로 ScoreService.SUBJECTS 상수를 그대로 반환.
 */
@WebServlet(urlPatterns = {"/api/subjects"})
public class SubjectServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        writeJson(resp, 200, ScoreService.SUBJECTS);
    }
}

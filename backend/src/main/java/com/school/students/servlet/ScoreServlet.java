package com.school.students.servlet;

import com.school.students.model.Score;
import com.school.students.service.ScoreService;
import com.school.students.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;

/**
 * 성적 CRUD 서블릿.
 *
 * 매핑 URL:
 *   POST   /api/scores         성적 입력
 *   PUT    /api/scores/{id}    점수 수정
 *   DELETE /api/scores/{id}    점수 삭제
 *
 * (학생 단위 조회는 StudentServlet 의 /api/students/{id}/scores 가 담당)
 */
@WebServlet(urlPatterns = {"/api/scores", "/api/scores/*"})
public class ScoreServlet extends BaseServlet {

    private final ScoreService scoreService = new ScoreService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Score input = JsonUtil.fromJson(req.getInputStream(), Score.class);
            Score created = scoreService.create(input);
            resp.setHeader("Location", "/api/scores/" + created.getId());
            writeJson(resp, 201, created);
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, "VALIDATION_FAILED", e.getMessage());
        } catch (SQLIntegrityConstraintViolationException e) {
            // (student_id, subject) UNIQUE 위반 — 같은 학생이 같은 과목 점수 중복 입력 시도
            // 프론트엔드가 이 코드를 보고 PUT 으로 갱신 다이얼로그를 띄움
            writeError(resp, 409, "DUPLICATE_SCORE", "이미 입력된 과목입니다. PUT 으로 갱신하세요.");
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
            writeError(resp, 400, "MISSING_ID", "수정할 점수 id 가 URL 에 없습니다.");
            return;
        }
        try {
            Score input = JsonUtil.fromJson(req.getInputStream(), Score.class);
            if (input.getScore() == null) {
                writeError(resp, 400, "VALIDATION_FAILED", "score 필드가 필요합니다.");
                return;
            }
            boolean ok = scoreService.updateScore(id, input.getScore());
            if (ok) writeJson(resp, 200, java.util.Map.of("id", id, "score", input.getScore()));
            else writeError(resp, 404, "NOT_FOUND", "해당 점수를 찾을 수 없습니다.");
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, "VALIDATION_FAILED", e.getMessage());
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
            writeError(resp, 400, "MISSING_ID", "삭제할 점수 id 가 URL 에 없습니다.");
            return;
        }
        try {
            boolean ok = scoreService.delete(id);
            if (ok) writeJson(resp, 200, java.util.Map.of("deleted", id));
            else writeError(resp, 404, "NOT_FOUND", "해당 점수를 찾을 수 없습니다.");
        } catch (SQLException e) {
            e.printStackTrace();
            writeError(resp, 500, "DB_ERROR", "데이터베이스 오류: " + e.getMessage());
        }
    }
}

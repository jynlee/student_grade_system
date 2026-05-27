package com.school.students.servlet;

import com.school.students.util.JsonUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 모든 API 서블릿의 공통 부모.
 *
 * 책임:
 *  - CORS 헤더 처리 (Access-Control-*)
 *  - OPTIONS preflight 응답
 *  - JSON 응답 작성 헬퍼 (writeJson, writeError)
 *  - URL path parameter 추출 (extractIdFromPath)
 *
 * Servlet 3.0+ 의 @WebServlet 으로 매핑되며, doGet/doPost/doPut/doDelete 는 각 서블릿이 오버라이드.
 * service() 를 오버라이드해 모든 요청에 대해 CORS 헤더를 먼저 세팅한다.
 */
public abstract class BaseServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws javax.servlet.ServletException, IOException {

        // CORS 헤더 — 모든 응답에 부착 (프론트가 다른 origin에서 접근하므로 필수)
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");

        // Preflight (OPTIONS) 요청은 200 OK 로 즉시 응답
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        super.service(req, resp);
    }

    /**
     * 객체를 JSON으로 직렬화해 응답 본문에 쓴다.
     * Content-Type, Charset 을 명시해 한글이 안전하게 전달되도록 한다.
     */
    protected void writeJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(JsonUtil.toJson(body));
    }

    /** 에러 응답 (errorCode, message 또는 field 포함) */
    protected void writeError(HttpServletResponse resp, int status,
                              String errorCode, String message) throws IOException {
        writeJson(resp, status, Map.of("error", errorCode, "message", message));
    }

    /**
     * URL pathInfo 에서 정수형 id를 뽑아낸다.
     * 예) "/123"   → 123
     *     "/123/scores" → 123
     *
     * @return 추출된 id, 없거나 잘못된 형식이면 null
     */
    protected Long extractIdFromPath(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.length() <= 1) return null;
        // 맨 앞 슬래시 제거 후 첫 세그먼트만 추출
        String[] parts = path.substring(1).split("/");
        try {
            return Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * URL pathInfo 의 두 번째 세그먼트를 반환.
     * 예) "/123/scores" → "scores"
     *     "/123"        → null
     */
    protected String extractSubResource(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null) return null;
        String[] parts = path.substring(1).split("/");
        return parts.length >= 2 ? parts[1] : null;
    }
}

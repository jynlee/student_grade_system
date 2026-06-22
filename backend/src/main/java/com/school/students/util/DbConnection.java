package com.school.students.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC Connection 제공 유틸.
 *
 * 환경변수에서 DB 접속 정보를 읽어 매번 새 Connection 을 생성한다.
 * (단순 클래스 프로젝트라 Connection Pool 은 사용하지 않음 — 운영에서는 HikariCP 권장)
 *
 * DB 초기 기동이 늦을 수 있으므로 애플리케이션 시작 시 waitUntilReady() 로
 * 최대 30초간 재시도하여 MySQL이 준비될 때까지 기다린다.
 */
public class DbConnection {

    private static final String DB_HOST     = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT     = System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME     = System.getenv().getOrDefault("DB_NAME", "students");
    private static final String DB_USER     = System.getenv().getOrDefault("DB_USER", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "studentpass");

    // connectionCollation: Connector/J 8.0.26+에서 MySQL 세션 charset/collation을 강제 설정
    // SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci 를 핸드셰이크 시 전송 → 드라이버 내부 디코더도 utf8mb4로 갱신
    private static final String JDBC_URL = String.format(
        "jdbc:mysql://%s:%s/%s?connectionCollation=utf8mb4_0900_ai_ci&serverTimezone=Asia/Seoul",
        DB_HOST, DB_PORT, DB_NAME);

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC 드라이버를 찾을 수 없습니다.", e);
        }
    }

    /**
     * 새 Connection 을 반환한다. 호출자가 try-with-resources 로 닫아야 한다.
     */
    public static Connection get() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * MySQL 컨테이너가 ready 될 때까지 대기.
     * Docker Compose 의 healthcheck 와 별개로 백엔드 startup 시 추가 안전망.
     * 최대 30초 (1초 간격으로 30회) 시도.
     */
    public static void waitUntilReady() {
        int attempts = 30;
        while (attempts-- > 0) {
            try (Connection c = get()) {
                System.out.println("[DbConnection] DB 연결 성공.");
                return;
            } catch (SQLException e) {
                System.out.println("[DbConnection] DB 연결 대기 중... 남은 시도: " + attempts);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("[DbConnection] DB 연결 실패 — 30초 동안 응답 없음.");
    }
}

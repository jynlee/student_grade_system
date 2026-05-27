package com.school.students.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;

/**
 * JSON 직렬화/역직렬화 유틸 — Jackson ObjectMapper 의 얇은 래퍼.
 *
 * 애플리케이션 전역에서 단일 ObjectMapper 인스턴스를 재사용 (스레드 안전).
 * Jackson 설정:
 *  - null 필드도 포함해 직렬화 (프론트가 null 체크 용이)
 *  - 들여쓰기 출력 (디버깅 가독성)
 */
public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /** 객체를 JSON 문자열로 변환 */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (IOException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

    /** JSON 입력 스트림을 객체로 변환 */
    public static <T> T fromJson(InputStream in, Class<T> clazz) throws IOException {
        return MAPPER.readValue(in, clazz);
    }
}

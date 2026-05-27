package com.school.students.service;

import java.util.List;

/**
 * 등급 계산 알고리즘 — 5단계 절대평가.
 *
 * A (90+) / B (80~89) / C (70~79) / D (60~69) / F (60 미만)
 *
 * 동일한 로직이 두 곳에서 재사용됨:
 *  1) 과목별 점수 → 과목 등급
 *  2) 학생 전체 평균 → 전체 등급
 *
 * 상태가 없는 정적 메서드 → 어디서나 안전하게 호출.
 */
public class GradeCalculator {

    /**
     * 점수를 등급 문자열로 변환.
     * @param score 0.0 ~ 100.0 (평균은 double, 과목 점수도 double로 캐스트 가능)
     * @return "A" | "B" | "C" | "D" | "F"
     */
    public static String calculateGrade(double score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    /**
     * 정수 점수 리스트의 평균을 계산.
     * 빈 리스트면 null 반환 (등급은 "-" 로 표시될 수 있도록).
     */
    public static Double average(List<Integer> scores) {
        if (scores == null || scores.isEmpty()) return null;
        double sum = 0;
        for (int s : scores) sum += s;
        // 소수 둘째자리에서 반올림
        return Math.round(sum / scores.size() * 10.0) / 10.0;
    }
}

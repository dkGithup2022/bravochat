package com.chatbot.bravo.service.chat.agent.tool.schedule;

/**
 * schedule 툴의 추출 파라미터 — ToolParamExtractor가 대화에서 뽑아 매핑한다.
 * op에 따라 쓰는 필드가 다르며 나머지는 null.
 *
 * @param op                "add" | "list" | "update" | "apply_update" | "missing"
 * @param question          op=missing 일 때 유저에게 물을 내용
 * @param title             (add) 일정 제목 / (update·apply_update) 새 제목 — 바꿀 때만
 * @param content           (add) 상세 — 선택 / (update·apply_update) 새 상세 — 바꿀 때만
 * @param scheduleType      (add) HEALTH|PERSONAL|WORK|ETC — 선택, 미스매치는 ETC 흡수
 * @param scheduledAt       (add) "YYYY-MM-DDTHH:mm" KST / (update·apply_update) 새 시각 — 바꿀 때만
 * @param from              (list) "YYYY-MM-DD" — 선택, 기본 오늘
 * @param to                (list) "YYYY-MM-DD" — 선택(포함 경계), 기본 오늘+7일
 * @param targetTitle       (update·apply_update) 변경 대상 일정의 제목 — 필수
 * @param targetDate        (update) 대상 일정의 날짜 "YYYY-MM-DD" — 모르면 생략(기본 오늘~+30일 검색)
 * @param targetScheduledAt (apply_update) 대상 일정의 변경 전 시각 "YYYY-MM-DDTHH:mm" — 필수,
 *                          1단계 확인 메시지에 있던 값
 */
public record ScheduleParams(
        String op,
        String question,
        String title,
        String content,
        String scheduleType,
        String scheduledAt,
        String from,
        String to,
        String targetTitle,
        String targetDate,
        String targetScheduledAt
) {
}

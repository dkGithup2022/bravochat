package com.chatbot.bravo.service.chat.agent.tool.schedule;

/**
 * schedule 툴의 추출 파라미터 — ToolParamExtractor가 대화에서 뽑아 매핑한다.
 * op에 따라 쓰는 필드가 다르며 나머지는 null.
 *
 * @param op          "add" | "list" | "missing"
 * @param question    op=missing 일 때 유저에게 물을 내용
 * @param title       (add) 일정 제목
 * @param content     (add) 상세 — 선택
 * @param scheduleType(add) HEALTH|PERSONAL|WORK|ETC — 선택, 미스매치는 ETC 흡수
 * @param scheduledAt (add) "YYYY-MM-DDTHH:mm" — 한국시간(Asia/Seoul) 로컬
 * @param from        (list) "YYYY-MM-DD" — 선택, 기본 오늘
 * @param to          (list) "YYYY-MM-DD" — 선택(미포함 경계), 기본 오늘+7일
 */
public record ScheduleParams(
        String op,
        String question,
        String title,
        String content,
        String scheduleType,
        String scheduledAt,
        String from,
        String to
) {
}

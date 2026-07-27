package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.model.llm.LlmMessage;

import java.util.List;

/**
 * 모든 툴에 동일하게 전달되는 실행 컨텍스트.
 *
 * @param userId       요청 사용자
 * @param turnId       현재 턴 — 툴이 만드는 데이터의 생성 출처 기록용 (예: schedules.turn_id)
 * @param conversation 지금까지의 대화 메시지(읽기용) — 메모리 + 이번 입력 + 턴 컨텍스트
 *                     + 같은 턴에서 이미 실행된 툴 마커 블록 포함
 */
public record ToolContext(
        Long userId,
        Long turnId,
        List<LlmMessage> conversation
) {
}

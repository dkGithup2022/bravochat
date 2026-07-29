package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.model.llm.LlmMessage;

import java.util.List;
import java.util.function.Consumer;

/**
 * 모든 툴에 동일하게 전달되는 실행 컨텍스트.
 *
 * @param userId           요청 사용자
 * @param turnId           현재 턴 — 툴이 만드는 데이터의 생성 출처 기록용 (예: schedules.turn_id)
 * @param conversation     지금까지의 대화 메시지(읽기용) — 메모리 + 이번 입력 + 턴 컨텍스트
 *                         + 같은 턴에서 이미 실행된 툴 마커 블록 포함
 * @param progressRecorder 툴 실행 중간 기록 채널 — 넘긴 문자열이 turn_events(TOOL_PROGRESS)로
 *                         즉시 저장된다(디버깅용). 실행 중간에 죽어도 그 시점까지의 기록이 남는다.
 */
public record ToolContext(
        Long userId,
        Long turnId,
        List<LlmMessage> conversation,
        Consumer<String> progressRecorder
) {
    /** 기록 채널 없이 쓰는 생성자(테스트 등) — progress는 no-op. */
    public ToolContext(Long userId, Long turnId, List<LlmMessage> conversation) {
        this(userId, turnId, conversation, content -> { });
    }

    /** 중간 정리 결과를 TOOL_PROGRESS 이벤트로 남긴다. */
    public void progress(String content) {
        progressRecorder.accept(content);
    }
}

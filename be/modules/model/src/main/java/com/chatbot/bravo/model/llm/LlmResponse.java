package com.chatbot.bravo.model.llm;

/**
 * LLM 1회 호출 응답.
 * MVP: 도구 미지원 → hasToolUse 항상 false, 첫 응답이 최종.
 * Phase 2: toolCalls 목록을 추가해 툴 루프를 돌린다.
 */
public record LlmResponse(
        String assistantText,
        boolean hasToolUse
) {
    public static LlmResponse finalText(String assistantText) {
        return new LlmResponse(assistantText, false);
    }
}

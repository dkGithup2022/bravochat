package com.chatbot.bravo.service.chat;

/**
 * LLM 호출 계약 (플레이스홀더).
 *
 * <p>OpenAI 등 실제 연동은 별도로 관리한다(decision 5). 메서드 시그니처는
 * SendMessage 툴 루프 설계가 확정된 후 정의한다 — 메시지 컨텍스트 구성, 툴 스키마 전달,
 * 응답(텍스트 or 툴 호출) 표현 방식이 함께 결정되어야 하므로 지금은 의도적으로 비워둔다.
 */
public interface LlmClient {
    // TODO: 툴 루프 설계 확정 후 chat(...) 시그니처 정의 + 구현체(infrastructure) 추가
}

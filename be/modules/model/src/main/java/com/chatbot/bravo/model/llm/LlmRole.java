package com.chatbot.bravo.model.llm;

/** LLM 대화 메시지의 역할. (SYSTEM은 시스템 프롬프트로 별도 처리) */
public enum LlmRole {
    USER,
    ASSISTANT,
    /** 툴 실행 교환 블록. 프롬프트 기반이라 전송 시 USER 메시지로 매핑된다. */
    TOOL
}

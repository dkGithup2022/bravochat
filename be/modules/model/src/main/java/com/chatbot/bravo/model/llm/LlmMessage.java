package com.chatbot.bravo.model.llm;

/**
 * LLM 호출에 넣는 인메모리 대화 메시지 (persist용 TurnEvent와 별개).
 */
public record LlmMessage(
        LlmRole role,
        String content
) {
    public static LlmMessage user(String content) {
        return new LlmMessage(LlmRole.USER, content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(LlmRole.ASSISTANT, content);
    }

    /** 툴 실행 교환 블록(## tool start … ## tool end). */
    public static LlmMessage tool(String content) {
        return new LlmMessage(LlmRole.TOOL, content);
    }
}

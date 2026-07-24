package com.chatbot.bravo.model.llm;

/**
 * 툴 루프 한 스텝에서 모델이 내리는 결정 (구조화 JSON 응답 파싱 대상).
 *
 * <pre>
 * { "type": "FINAL",     "content": "최종 답변" }
 * { "type": "TOOL_CALL", "tool": { "name": "get_time", "arguments": { ... } } }
 * </pre>
 *
 * OpenAiClient.callSingleType(..., LlmAction.class)로 파싱한다.
 * type=FINAL이면 루프 종료(①), TOOL_CALL이면 툴 실행 후 재호출.
 */
public record LlmAction(
        LlmActionType type,
        String content,
        ToolInvocation tool
) {
    public boolean isFinal() {
        return type == LlmActionType.FINAL;
    }

    public boolean isToolCall() {
        return type == LlmActionType.TOOL_CALL;
    }

    public static LlmAction finalAnswer(String content) {
        return new LlmAction(LlmActionType.FINAL, content, null);
    }

    public static LlmAction toolCall(ToolInvocation tool) {
        return new LlmAction(LlmActionType.TOOL_CALL, null, tool);
    }
}

package com.chatbot.bravo.service.chat.agent.tool;

/**
 * 툴 실행 결과.
 *
 * @param contextToLeave 대화(messages)에 남길 표현 — ★ 각 툴이 스스로 결정(요약/일부/전체). 규약을 툴이 소유.
 * @param isError        실패 여부 (true면 모델에 오류로 되먹임)
 */
public record ToolOutcome(
        String contextToLeave,
        boolean isError
) {
    public static ToolOutcome ok(String contextToLeave) {
        return new ToolOutcome(contextToLeave, false);
    }

    public static ToolOutcome error(String contextToLeave) {
        return new ToolOutcome(contextToLeave, true);
    }
}

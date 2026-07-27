package com.chatbot.bravo.service.chat.agent.tool;

/**
 * 툴 실행 결과. 모든 툴 핸들러가 이 타입을 반환한다.
 * 되먹임(response)과 기록(turnMemo)의 분리 — 어느 쪽을 상세하게 할지는 툴마다 다르다.
 *
 * @param response 메인 루프 messages에 되먹일 내용 — 모델이 다음 판단에 쓰는 것
 * @param turnMemo turn_events(TOOL_RESULT)에 남길 기록 — transcript/감사용, 툴이 커스텀
 * @param success  실패면 response가 에러 설명으로 모델에 되먹여진다 (턴은 죽이지 않음)
 */
public record ToolResponse(
        String response,
        String turnMemo,
        boolean success
) {
    /** turnMemo를 따로 안 정하면 response 그대로 기록. */
    public static ToolResponse ok(String response) {
        return new ToolResponse(response, response, true);
    }

    public static ToolResponse ok(String response, String turnMemo) {
        return new ToolResponse(response, turnMemo, true);
    }

    public static ToolResponse fail(String reason) {
        return new ToolResponse(reason, "FAILED: " + reason, false);
    }
}

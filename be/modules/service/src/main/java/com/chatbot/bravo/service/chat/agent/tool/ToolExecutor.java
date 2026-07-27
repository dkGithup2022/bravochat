package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.model.llm.ToolInvocation;

/**
 * 모델이 요청한 툴 호출을 실행한다. (핸들러를 찾아 위임)
 * 계약: 어떤 실패(미등록/비활성/핸들러 예외)도 예외로 던지지 않고
 * {@link ToolResponse#fail}로 반환한다 — 에러도 모델에 되먹이는 컨텍스트다.
 */
public interface ToolExecutor {

    ToolResponse execute(ToolInvocation call, ToolContext ctx);
}

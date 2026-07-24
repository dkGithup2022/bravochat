package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.model.llm.ToolInvocation;

/**
 * 모델이 요청한 툴 호출을 실행한다. (핸들러를 찾아 위임)
 */
public interface ToolExecutor {

    ToolOutcome execute(ToolInvocation call, ToolContext ctx);
}

package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.model.llm.ToolInvocation;
import org.springframework.stereotype.Component;

/**
 * 임시 구현 — 등록된 툴이 없으므로 호출되면 예외. 활성 툴이 비어있는 한 루프에서 도달하지 않는다.
 * (실제 ToolCatalog 기반 실행 구현으로 대체 예정)
 */
@Component
class NoToolExecutor implements ToolExecutor {

    @Override
    public ToolOutcome execute(ToolInvocation call, ToolContext ctx) {
        throw new IllegalStateException("no tool registered: " + (call == null ? "null" : call.name()));
    }
}

package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.model.llm.ToolInvocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ToolManager에서 핸들러를 찾아 실행한다.
 * 미등록/핸들러 예외 전부 fail로 반환 — 예외를 밖으로 던지지 않는다
 * (에러도 모델에 되먹여 자가 수정을 유도. 턴을 죽이는 건 메인 LLM 호출 실패뿐).
 */
@Component
@RequiredArgsConstructor
@Slf4j
class DefaultToolExecutor implements ToolExecutor {

    private final ToolManager toolManager;

    @Override
    public ToolResponse execute(ToolInvocation call, ToolContext ctx) {
        return toolManager.find(call.name())
                .map(handler -> handleSafely(handler, call, ctx))
                .orElseGet(() -> {
                    log.warn("미등록 툴 호출: tool={}, turnId={}", call.name(), ctx.turnId());
                    return ToolResponse.fail("unknown tool: " + call.name());
                });
    }

    private ToolResponse handleSafely(ToolHandler handler, ToolInvocation call, ToolContext ctx) {
        try {
            return handler.handle(call, ctx);
        } catch (RuntimeException e) {
            log.error("툴 실행 실패: tool={}, turnId={}", call.name(), ctx.turnId(), e);
            return ToolResponse.fail("tool execution error: " + e.getMessage());
        }
    }
}

package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.model.llm.ToolInvocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultToolExecutorTest {

    @Mock private ToolManager toolManager;
    @Mock private ToolHandler handler;

    @InjectMocks
    private DefaultToolExecutor executor;

    private static final ToolInvocation CALL = new ToolInvocation("schedule", Map.of());
    private static final ToolContext CTX = new ToolContext(7L, 100L, List.of());

    @Test
    @DisplayName("[성공] 등록된 툴이면 핸들러 결과를 그대로 반환한다")
    void should_delegateToHandler_when_registered() {
        when(toolManager.find("schedule")).thenReturn(Optional.of(handler));
        when(handler.handle(CALL, CTX)).thenReturn(ToolResponse.ok("done"));

        ToolResponse response = executor.execute(CALL, CTX);

        assertThat(response.success()).isTrue();
        assertThat(response.response()).isEqualTo("done");
    }

    @Test
    @DisplayName("[실패] 미등록 툴이면 fail — 예외를 던지지 않는다")
    void should_fail_when_toolNotRegistered() {
        when(toolManager.find("schedule")).thenReturn(Optional.empty());

        ToolResponse response = executor.execute(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).contains("unknown tool");
    }

    @Test
    @DisplayName("[실패] 핸들러가 예외를 던져도 fail로 흡수한다 — 턴은 죽지 않는다")
    void should_absorbException_when_handlerThrows() {
        when(toolManager.find("schedule")).thenReturn(Optional.of(handler));
        when(handler.handle(CALL, CTX)).thenThrow(new IllegalStateException("boom"));

        ToolResponse response = executor.execute(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).contains("boom");
        assertThat(response.turnMemo()).startsWith("FAILED:");
    }
}

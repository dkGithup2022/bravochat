package com.chatbot.bravo.service.chat.agent.systemprompt;

import com.chatbot.bravo.service.chat.agent.tool.ToolManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatSystemPromptProviderTest {

    private ToolManager managerWith(String... toolNames) {
        ToolManager manager = mock(ToolManager.class);
        when(manager.toolNames()).thenReturn(new TreeSet<>(List.of(toolNames)));
        when(manager.renderToolSection()).thenReturn(toolNames.length == 0
                ? ""
                : "# 사용 가능한 도구\n\n- " + toolNames[0] + ": 설명");
        return manager;
    }

    @Test
    void 조립은_SystemPromptBuilder_구성을_통한다() {
        ChatSystemPromptProvider provider = new ChatSystemPromptProvider(managerWith());
        String prompt = provider.build();

        // 기존 구성(빌더)의 섹션들 + 응답형식이 모두 조립돼야 함 (usecase 문자열 이어붙이기 X)
        assertThat(prompt).contains("AI 어시스턴트");         // Core (기존 구성)
        assertThat(prompt).contains("# 당신은 누구인가");      // Persona (기존 구성)
        assertThat(prompt).contains("# 톤");                   // Style (기존 구성)
        assertThat(prompt).contains("# 응답 형식");            // response_format 섹션
        assertThat(prompt).contains("\"type\":\"FINAL\"");

        // 툴 없음 → 툴 관련 섹션 부재
        assertThat(prompt).doesNotContain("# 도구 사용").doesNotContain("# 사용 가능한 도구");
    }

    @Test
    void 툴이_등록되면_가이드와_툴목록이_함께_조립된다() {
        ChatSystemPromptProvider provider = new ChatSystemPromptProvider(managerWith("schedule"));
        String prompt = provider.build();

        assertThat(prompt).contains("# 도구 사용");            // tool_guidance (조건부 섹션)
        assertThat(prompt).contains("- schedule: 설명");        // tool_list (ToolManager 렌더)
        // 목록은 가이드 뒤, 응답 형식 앞
        assertThat(prompt.indexOf("# 도구 사용")).isLessThan(prompt.indexOf("- schedule"));
        assertThat(prompt.indexOf("- schedule")).isLessThan(prompt.indexOf("# 응답 형식"));
    }
}

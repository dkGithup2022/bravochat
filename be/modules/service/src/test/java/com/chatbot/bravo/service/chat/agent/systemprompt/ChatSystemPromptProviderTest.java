package com.chatbot.bravo.service.chat.agent.systemprompt;

import com.chatbot.bravo.service.chat.agent.tool.ToolCatalog;
import com.chatbot.bravo.service.chat.agent.tool.ToolHandler;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChatSystemPromptProviderTest {

    private final ToolCatalog emptyCatalog = new ToolCatalog() {
        @Override public String renderToolSection(Set<String> enabledTools) { return ""; }
        @Override public Optional<ToolHandler> find(String name) { return Optional.empty(); }
    };

    @Test
    void 조립은_SystemPromptBuilder_구성을_통한다() {
        ChatSystemPromptProvider provider = new ChatSystemPromptProvider(emptyCatalog);
        String prompt = provider.build(Set.of());

        // 기존 구성(빌더)의 섹션들 + 응답형식이 모두 조립돼야 함 (usecase 문자열 이어붙이기 X)
        assertThat(prompt).contains("AI 어시스턴트");         // Core (기존 구성)
        assertThat(prompt).contains("# 당신은 누구인가");      // Persona (기존 구성)
        assertThat(prompt).contains("# 톤");                   // Style (기존 구성)
        assertThat(prompt).contains("# 응답 형식");            // response_format 섹션
        assertThat(prompt).contains("\"type\":\"FINAL\"");
    }
}

package com.chatbot.bravo.service.chat.agent.systemprompt.sectionContributor;

import com.chatbot.bravo.service.chat.agent.systemprompt.PromptSection;

import java.util.Optional;
import java.util.SortedSet;

/**
 * [섹션3] 도구 선택 지침. 여기는 지침만 — 도구 정의는 ToolDefinitionProvider.
 * enabledTools는 세션 시작 시 확정·고정 (tools가 렌더 순서상 맨 앞이라 흔들리면 캐시 전부 붕괴).
 */
public interface ToolGuidanceContributor extends PromptSectionContributor {
    Optional<PromptSection> contribute(SortedSet<String> enabledTools);  // 3.1
}

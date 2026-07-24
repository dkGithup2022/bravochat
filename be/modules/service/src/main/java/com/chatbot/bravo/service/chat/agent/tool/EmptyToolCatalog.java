package com.chatbot.bravo.service.chat.agent.tool;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * 임시 구현 — 등록된 툴 없음. 목록은 빈 문자열, 핸들러 조회는 항상 empty.
 * (실제 툴이 생기면 name→ToolHandler 등록 구현으로 대체)
 */
@Component
class EmptyToolCatalog implements ToolCatalog {

    @Override
    public String renderToolSection(Set<String> enabledTools) {
        return "";
    }

    @Override
    public Optional<ToolHandler> find(String name) {
        return Optional.empty();
    }
}

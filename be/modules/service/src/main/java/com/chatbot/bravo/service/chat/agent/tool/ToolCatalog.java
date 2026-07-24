package com.chatbot.bravo.service.chat.agent.tool;

import java.util.Optional;
import java.util.Set;

/**
 * 활성 툴 목록 제공 + 실행 핸들러 라우팅.
 */
public interface ToolCatalog {

    /** 시스템 프롬프트에 넣을 툴 목록 섹션. 활성 툴이 없으면 빈 문자열. */
    String renderToolSection(Set<String> enabledTools);

    /** 이름으로 핸들러 조회. */
    Optional<ToolHandler> find(String name);
}

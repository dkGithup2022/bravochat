package com.chatbot.bravo.service.chat.agent.tool;

import java.util.Set;

/**
 * 요청에 활성화할 툴 이름 집합을 유저 컨텍스트 기준으로 해석한다.
 * 실제 구현(주입 규칙)은 별도 — 지금은 빈 집합 임시 구현만.
 */
public interface EnabledToolsResolver {

    Set<String> resolve(Long userId);
}

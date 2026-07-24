package com.chatbot.bravo.service.chat.agent.tool;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 임시 구현 — 항상 빈 집합. (실제 유저 컨텍스트 기반 주입은 별도로 대체 예정)
 * 활성 툴이 없으므로 시스템 프롬프트에 툴 목록이 없고, 모델은 항상 FINAL 로 답한다.
 */
@Component
class EmptyEnabledToolsResolver implements EnabledToolsResolver {

    @Override
    public Set<String> resolve(Long userId) {
        return Set.of();
    }
}

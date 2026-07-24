package com.chatbot.bravo.service.chat.agent.systemprompt;

import java.util.Objects;

/**
 * 시스템 프롬프트를 구성하는 한 조각.
 * 빌더는 합쳐진 문자열이 아니라 이 조각의 리스트를 반환한다 —
 * 캐시 브레이크포인트(cache_control)가 블록 단위로 찍히기 때문.
 *
 * @param name      섹션 식별자 (로깅·테스트·교체용). 예: "core", "tool_guidance"
 * @param text      렌더링된 본문
 * @param cacheable 이 섹션까지가 안정 구간인지. 전송 계층은 "마지막 cacheable 섹션"에
 *                  cache_control을 찍는다. 값이 사용자별로 갈리는 섹션이 false 후보.
 */
public record PromptSection(String name, String text, boolean cacheable) {
    public PromptSection {
        Objects.requireNonNull(name);
        Objects.requireNonNull(text);
    }
}

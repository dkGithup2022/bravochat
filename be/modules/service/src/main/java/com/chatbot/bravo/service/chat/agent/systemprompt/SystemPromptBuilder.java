package com.chatbot.bravo.service.chat.agent.systemprompt;

import java.util.List;
import java.util.SortedSet;

/**
 * 조립 총괄. 섹션 순서·조건부 생략·캐시경계 표시만 책임진다 (내용은 모름).
 * 불변식: 같은 (ctx, enabledTools)면 같은 결과 / 순서는 변경빈도 오름차순 /
 *         cacheable=true 구간은 앞쪽에 연속으로 몰릴 것.
 * 주의: Builder는 cache_control을 "찍지" 않는다. cacheable 플래그만 세팅, 부착은 전송 계층.
 */
public interface SystemPromptBuilder {
    List<PromptSection> build(RuntimeContext ctx, SortedSet<String> enabledTools);
}

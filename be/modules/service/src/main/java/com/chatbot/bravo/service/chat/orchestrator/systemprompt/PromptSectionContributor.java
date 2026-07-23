package com.chatbot.bravo.service.chat.orchestrator.systemprompt;

import java.util.Optional;

/**
 * 시스템 프롬프트 섹션 하나를 만드는 하위 빌더의 공통 계약.
 * 구현 지침:
 *  - 고정 문구는 구현체 안의 상수(SCREAMING_SNAKE).
 *  - 내용이 static이어도 메소드로 감싼다 (테스트·조건 분기).
 *  - contribute()가 Optional.empty()면 그 섹션은 프롬프트에서 통째로 빠진다.
 */
public interface PromptSectionContributor {
    String sectionName();

    /**
     * 섹션 본문 생성. 반드시 순수 함수 — 같은 입력이면 같은 바이트.
     * now()/randomUUID()/순서 불안정 Map 순회는 캐시를 조용히 깬다.
     */
    Optional<PromptSection> contribute();
}

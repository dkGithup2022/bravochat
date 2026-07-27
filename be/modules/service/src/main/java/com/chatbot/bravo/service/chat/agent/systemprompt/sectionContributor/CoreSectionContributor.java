package com.chatbot.bravo.service.chat.agent.systemprompt.sectionContributor;

/** [섹션1] 불변 코어 — 대목표·동작 방식(1.1) + 안전 규칙(1.2) + 대화 해석(1.3). */
public interface CoreSectionContributor extends PromptSectionContributor {
    String getMissionSection();       // 1.1 정체성 + 대목표 + 동작 방식
    String getSafetySection();        // 1.2 인젝션 대응 + 태그 선언 (+ 거부·위험행동)
    String getUserContextSection();   // 1.3 대화 메시지 순서·현재 요청 해석
}

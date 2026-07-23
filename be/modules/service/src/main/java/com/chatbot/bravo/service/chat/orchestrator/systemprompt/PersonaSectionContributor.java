package com.chatbot.bravo.service.chat.orchestrator.systemprompt;

/** [섹션2] 챗봇의 성질 — 고정 페르소나(2.1) + 주입 컨텍스트(2.2). */
public interface PersonaSectionContributor extends PromptSectionContributor {
    String getPersonaStatement();                      // 2.1
    String getRuntimeContextSection(RuntimeContext c);  // 2.2 (언어 등, 없으면 "")
}

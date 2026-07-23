package com.chatbot.bravo.service.chat.orchestrator.systemprompt;

/** [섹션4] 톤(4.1) / 출력 형식(4.2). */
public interface StyleSectionContributor extends PromptSectionContributor {
    String getToneSection();          // 4.1
    String getOutputFormatSection();  // 4.2
}

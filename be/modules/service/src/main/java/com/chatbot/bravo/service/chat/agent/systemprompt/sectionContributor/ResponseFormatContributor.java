package com.chatbot.bravo.service.chat.agent.systemprompt.sectionContributor;

/** [섹션5] 응답 형식 — 모델이 LlmAction JSON(FINAL | TOOL_CALL)으로만 응답하도록 지시. */
public interface ResponseFormatContributor extends PromptSectionContributor {
    String getResponseFormatSection();  // 5.1
}

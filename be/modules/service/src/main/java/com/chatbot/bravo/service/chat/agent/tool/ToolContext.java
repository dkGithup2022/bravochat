package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.model.llm.LlmMessage;

import java.util.List;

/**
 * 모든 툴에 동일하게 전달되는 이전 대화 컨텍스트.
 *
 * @param userId       요청 사용자
 * @param conversation 지금까지의 대화 메시지(읽기용)
 */
public record ToolContext(
        Long userId,
        List<LlmMessage> conversation
) {
}

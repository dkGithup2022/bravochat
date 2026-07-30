package com.chatbot.bravo.llm.openai;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * LLM 요청 전문을 로그로 남길 때의 렌더링. 테스트/디버깅용 —
 * 대화 내용이 로그에 그대로 남으므로 운영 전환 시 제거하거나 debug 레벨로 낮출 것.
 */
final class PromptLog {

    private PromptLog() {
    }

    static String render(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            sb.append("=== ").append(m.getMessageType()).append(" ===\n")
                    .append(m.getText()).append('\n');
        }
        return sb.toString();
    }
}

package com.chatbot.bravo.service.chat;

import com.chatbot.bravo.service.chat.dto.SendMessageCommand;
import com.chatbot.bravo.service.chat.dto.SendMessageResult;

/**
 * 사용자 메시지를 받아 LLM/툴 실행 루프를 수행하고 최종 응답을 반환한다.
 *
 * <p>구현 이연: Turn 생성 → USER_MESSAGE append → LLM 호출 → (TOOL_CALL/TOOL_RESULT 반복)
 * → ASSISTANT_MESSAGE append → Turn 완료 → 최종 응답. LLM 툴 루프 설계 + OpenAI 연동 후 구현.
 * 현재는 {@link com.chatbot.bravo.service.chat.impl.NotImplementedSendMessageUsecase} 스텁이 501을 반환.
 */
public interface SendMessageUsecase {

    SendMessageResult sendMessage(SendMessageCommand command);
}

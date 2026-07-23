package com.chatbot.bravo.service.chat.impl;

import com.chatbot.bravo.exception.chat.SendMessageNotImplementedException;
import com.chatbot.bravo.service.chat.SendMessageUsecase;
import com.chatbot.bravo.service.chat.dto.SendMessageCommand;
import com.chatbot.bravo.service.chat.dto.SendMessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SendMessageUsecase 스텁 — 오케스트레이션/LLM 연동 전까지 501을 반환한다.
 * 데이터 레이어(Turn/TurnEvent 모델·엔티티·쓰기 Repository)는 이미 준비돼 있으므로,
 * 실제 구현 시 이 클래스를 대체하고 TurnRepository/TurnEventRepository/LlmClient를 주입한다.
 */
@Service
@Slf4j
class NotImplementedSendMessageUsecase implements SendMessageUsecase {

    @Override
    public SendMessageResult sendMessage(SendMessageCommand command) {
        log.warn("SendMessage not implemented yet - userId={}", command.userId());
        throw new SendMessageNotImplementedException(
                "SendMessage orchestration not implemented: userId=" + command.userId());
    }
}

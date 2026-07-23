package com.chatbot.bravo.exception.chat;

import com.chatbot.bravo.exception.DomainException;
import com.chatbot.bravo.exception.HttpException;

/**
 * SendMessage 오케스트레이션 미구현 (501).
 * LLM 툴 루프 설계 + OpenAI 연동 완료 시 제거 예정.
 */
public class SendMessageNotImplementedException extends DomainException implements HttpException {

    public SendMessageNotImplementedException(String detail) {
        super(detail);
    }

    @Override public int httpStatusCode() { return 501; }
    @Override public String httpErrorMessage() { return "아직 구현되지 않은 기능입니다"; }
}

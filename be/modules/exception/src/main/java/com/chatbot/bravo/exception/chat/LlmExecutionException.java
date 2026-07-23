package com.chatbot.bravo.exception.chat;

import com.chatbot.bravo.exception.DomainException;
import com.chatbot.bravo.exception.HttpException;

/**
 * LLM 호출/처리 실패 (500).
 */
public class LlmExecutionException extends DomainException implements HttpException {

    public LlmExecutionException(String detail) {
        super(detail);
    }

    @Override public int httpStatusCode() { return 500; }
    @Override public String httpErrorMessage() { return "응답 생성에 실패했습니다"; }
}

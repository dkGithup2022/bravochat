package com.chatbot.bravo.exception.chat;

import com.chatbot.bravo.exception.DomainException;
import com.chatbot.bravo.exception.HttpException;

/**
 * 최근 대화 조회 크기가 허용 범위(1~20)를 벗어남 (400).
 */
public class InvalidRecentTurnSizeException extends DomainException implements HttpException {

    public InvalidRecentTurnSizeException(String detail) {
        super(detail);
    }

    @Override public int httpStatusCode() { return 400; }
    @Override public String httpErrorMessage() { return "조회 크기는 1 이상 20 이하만 허용됩니다"; }
}

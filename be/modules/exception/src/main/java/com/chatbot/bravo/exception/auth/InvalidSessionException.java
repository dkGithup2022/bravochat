package com.chatbot.bravo.exception.auth;

import com.chatbot.bravo.exception.DomainException;
import com.chatbot.bravo.exception.HttpException;

/**
 * 세션 무효 (401) — 세션 없음/만료/형식 오류.
 * 로그인 실패(LoginFailedException)와 구분: 이쪽은 "다시 로그인하라"는 신호.
 */
public class InvalidSessionException extends DomainException implements HttpException {

    public InvalidSessionException(String detail) {
        super(detail);
    }

    @Override public int httpStatusCode() { return 401; }
    @Override public String httpErrorMessage() { return "세션이 유효하지 않습니다. 다시 로그인해 주세요"; }
}

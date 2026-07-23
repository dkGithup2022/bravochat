package com.chatbot.bravo.exception.auth;

import com.chatbot.bravo.exception.DomainException;
import com.chatbot.bravo.exception.HttpException;

/**
 * 로그인 실패 (401).
 * 계정 없음/비밀번호 불일치를 구분하지 않는다 — 계정 존재 여부 누수 방지.
 * 상세 사유는 detail(서버 로그)에만 남기고 클라이언트 메시지는 동일하게 유지한다.
 */
public class LoginFailedException extends DomainException implements HttpException {

    public LoginFailedException(String detail) {
        super(detail);
    }

    @Override public int httpStatusCode() { return 401; }
    @Override public String httpErrorMessage() { return "아이디 또는 비밀번호가 올바르지 않습니다"; }
}

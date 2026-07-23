package com.chatbot.bravo.service.auth;

/**
 * 비밀번호 검증 계약. 저장된 해시와 평문의 일치 여부만 판단한다.
 */
public interface PasswordVerifier {

    /** 평문 비밀번호가 저장된 값(해시)과 일치하는지 검증한다. */
    boolean matches(String rawPassword, String storedPassword);
}

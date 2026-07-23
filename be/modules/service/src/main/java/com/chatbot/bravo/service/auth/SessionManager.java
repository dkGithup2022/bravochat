package com.chatbot.bravo.service.auth;

import com.chatbot.bravo.model.auth.LoginSession;

/**
 * 로그인 세션의 생성/검증/만료 계약.
 */
public interface SessionManager {

    /** 유저의 새 세션을 발급하고 저장한다. */
    LoginSession newOne(Long userId);

    /**
     * sessionKey로 세션을 검증한다.
     * 특이사항: 부수효과 있음 — 유효 세션이면 lastRequestedAt을 touch(갱신)한다.
     * 만료 판정은 절대만료(lastLoggedInAt + TTL, LoginSession.isExpired) 지연 판정.
     */
    LoginSession check(String sessionKey);

    /** sessionKey로 세션을 만료(soft-delete)한다. 로그아웃용. 멱등. */
    void revoke(String sessionKey);
}

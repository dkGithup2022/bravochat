package com.chatbot.bravo.infrastructure.auth.repository;

import com.chatbot.bravo.model.auth.LoginSession;

import java.util.Optional;

public interface LoginSessionRepository {

    /** 세션 저장. 신규 발급(id=null) 및 touch 갱신 모두 이 메서드로 처리. */
    LoginSession save(LoginSession session);

    /**
     * sessionKey로 세션 단건 조회. 요청 인증 시 사용.
     * 특이사항: 만료 판정은 저장소가 하지 않는다 — 호출자(SessionManager)가
     * LoginSession.isExpired(now)로 판정하는 지연 판정 규약.
     */
    Optional<LoginSession> findBySessionKey(String sessionKey);

    /**
     * sessionKey로 세션을 만료(soft-delete)한다. 로그아웃용.
     * 특이사항: 멱등 — 대상 세션이 없어도 예외 없이 통과한다.
     */
    void deleteBySessionKey(String sessionKey);
}

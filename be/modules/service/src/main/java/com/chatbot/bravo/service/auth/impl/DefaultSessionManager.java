package com.chatbot.bravo.service.auth.impl;

import com.chatbot.bravo.exception.auth.InvalidSessionException;
import com.chatbot.bravo.infrastructure.auth.repository.LoginSessionRepository;
import com.chatbot.bravo.model.auth.LoginSession;
import com.chatbot.bravo.service.auth.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
class DefaultSessionManager implements SessionManager {

    private final LoginSessionRepository sessionRepository;

    @Override
    @Transactional
    public LoginSession newOne(Long userId) {
        return sessionRepository.save(LoginSession.issue(userId));
    }

    @Override
    @Transactional
    public LoginSession check(String sessionKey) {
        LoginSession session = sessionRepository.findBySessionKey(sessionKey)
                .orElseThrow(() -> new InvalidSessionException("session not found"));

        if (session.isExpired(Instant.now())) {
            throw new InvalidSessionException(
                    "session expired: loginSessionId=" + session.getLoginSessionId());
        }

        return sessionRepository.save(session.touch());
    }

    @Override
    @Transactional
    public void revoke(String sessionKey) {
        sessionRepository.deleteBySessionKey(sessionKey);
        log.info("session revoked");
    }
}

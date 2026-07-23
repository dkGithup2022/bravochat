package com.chatbot.bravo.jdbc.auth.repository;

import com.chatbot.bravo.infrastructure.auth.repository.LoginSessionRepository;
import com.chatbot.bravo.model.auth.LoginSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class LoginSessionJdbcRepository implements LoginSessionRepository {

    private final LoginSessionEntityRepository entityRepository;

    @Override
    public LoginSession save(LoginSession session) {
        LoginSessionEntity saved = entityRepository.save(LoginSessionEntity.from(session));
        return saved.toDomain();
    }

    @Override
    public Optional<LoginSession> findBySessionKey(String sessionKey) {
        return entityRepository.findBySessionKeyAndIsDeletedFalse(sessionKey)
                .map(LoginSessionEntity::toDomain);
    }

    @Override
    public void deleteBySessionKey(String sessionKey) {
        // 멱등: 이미 없거나 만료된 세션이면 아무것도 하지 않는다.
        entityRepository.findBySessionKeyAndIsDeletedFalse(sessionKey)
                .ifPresent(entity -> entityRepository.save(entity.softDelete()));
    }
}

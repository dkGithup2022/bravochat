package com.chatbot.bravo.jdbc.auth.repository;

import com.chatbot.bravo.infrastructure.auth.repository.UserRepository;
import com.chatbot.bravo.model.auth.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class UserJdbcRepository implements UserRepository {

    private final UserEntityRepository entityRepository;

    @Override
    public Optional<User> findByUsername(String username) {
        return entityRepository.findByUsernameAndIsDeletedFalse(username)
                .map(UserEntity::toDomain);
    }
}

package com.chatbot.bravo.jdbc.auth.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface LoginSessionEntityRepository extends CrudRepository<LoginSessionEntity, Long> {

    Optional<LoginSessionEntity> findBySessionKeyAndIsDeletedFalse(String sessionKey);
}

package com.chatbot.bravo.jdbc.chat.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface TurnEntityRepository extends CrudRepository<TurnEntity, Long> {

    Optional<TurnEntity> findByIdAndIsDeletedFalse(Long id);
}

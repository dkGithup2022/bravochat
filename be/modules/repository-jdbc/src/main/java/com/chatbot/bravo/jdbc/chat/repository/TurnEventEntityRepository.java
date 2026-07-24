package com.chatbot.bravo.jdbc.chat.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface TurnEventEntityRepository extends CrudRepository<TurnEventEntity, Long> {

    List<TurnEventEntity> findByTurnIdAndIsDeletedFalseOrderByIdAsc(Long turnId);
}

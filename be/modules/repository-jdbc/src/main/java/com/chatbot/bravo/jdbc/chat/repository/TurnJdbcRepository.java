package com.chatbot.bravo.jdbc.chat.repository;

import com.chatbot.bravo.infrastructure.chat.repository.TurnRepository;
import com.chatbot.bravo.model.chat.Turn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class TurnJdbcRepository implements TurnRepository {

    private final TurnEntityRepository entityRepository;

    @Override
    public Turn save(Turn turn) {
        return entityRepository.save(TurnEntity.from(turn)).toDomain();
    }

    @Override
    public Optional<Turn> findById(Long turnId) {
        return entityRepository.findByIdAndIsDeletedFalse(turnId).map(TurnEntity::toDomain);
    }
}

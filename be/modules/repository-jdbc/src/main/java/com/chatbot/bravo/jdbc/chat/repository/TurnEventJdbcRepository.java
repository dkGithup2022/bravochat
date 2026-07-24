package com.chatbot.bravo.jdbc.chat.repository;

import com.chatbot.bravo.infrastructure.chat.repository.TurnEventRepository;
import com.chatbot.bravo.model.chat.TurnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
class TurnEventJdbcRepository implements TurnEventRepository {

    private final TurnEventEntityRepository entityRepository;

    @Override
    public TurnEvent append(TurnEvent event) {
        return entityRepository.save(TurnEventEntity.from(event)).toDomain();
    }

    @Override
    public List<TurnEvent> appendAll(List<TurnEvent> events) {
        List<TurnEventEntity> entities = new ArrayList<>();
        for (TurnEvent event : events) {
            entities.add(TurnEventEntity.from(event));
        }
        List<TurnEvent> saved = new ArrayList<>();
        entityRepository.saveAll(entities).forEach(e -> saved.add(e.toDomain()));
        return saved;
    }

    @Override
    public List<TurnEvent> findAllByTurnIdInOrder(Long turnId) {
        List<TurnEvent> events = new ArrayList<>();
        entityRepository.findByTurnIdAndIsDeletedFalseOrderByIdAsc(turnId)
                .forEach(e -> events.add(e.toDomain()));
        return events;
    }
}

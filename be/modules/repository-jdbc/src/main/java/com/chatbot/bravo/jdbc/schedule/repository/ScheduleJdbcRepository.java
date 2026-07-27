package com.chatbot.bravo.jdbc.schedule.repository;

import com.chatbot.bravo.infrastructure.schedule.repository.ScheduleRepository;
import com.chatbot.bravo.model.schedule.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class ScheduleJdbcRepository implements ScheduleRepository {

    private final ScheduleEntityRepository entityRepository;

    @Override
    public Schedule save(Schedule schedule) {
        return entityRepository.save(ScheduleEntity.from(schedule)).toDomain();
    }

    @Override
    public List<Schedule> findAllByUserIdInPeriod(Long userId, Instant from, Instant to) {
        return entityRepository
                .findByUserIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndIsDeletedFalseOrderByScheduledAtAscIdAsc(
                        userId, from, to)
                .stream()
                .map(ScheduleEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Schedule> findByIdAndUserId(Long scheduleId, Long userId) {
        return entityRepository.findByIdAndUserIdAndIsDeletedFalse(scheduleId, userId)
                .map(ScheduleEntity::toDomain);
    }
}

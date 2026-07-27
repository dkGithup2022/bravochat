package com.chatbot.bravo.jdbc.schedule.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
interface ScheduleEntityRepository extends CrudRepository<ScheduleEntity, Long> {

    /** 기간 조회 [from, to) — scheduled_at 오름차순, 동시각은 id 오름차순. */
    List<ScheduleEntity> findByUserIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThanAndIsDeletedFalseOrderByScheduledAtAscIdAsc(
            Long userId, Instant from, Instant to);

    Optional<ScheduleEntity> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);
}

package com.chatbot.bravo.jdbc.schedule.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
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

    /** soft delete — 소유권(user_id)과 미삭제 상태를 조건에 포함. 1행 변경 시 true. */
    @Modifying
    @Query("""
            UPDATE schedules SET is_deleted = TRUE, deleted_at = :now, updated_at = :now
            WHERE id = :id AND user_id = :userId AND is_deleted = FALSE""")
    boolean softDeleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId, @Param("now") Instant now);
}

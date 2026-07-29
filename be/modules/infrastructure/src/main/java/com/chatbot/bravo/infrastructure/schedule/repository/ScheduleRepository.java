package com.chatbot.bravo.infrastructure.schedule.repository;

import com.chatbot.bravo.model.schedule.Schedule;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {

    /** Schedule 저장. 신규 생성(id=null) 및 상태 변경(done 처리) 모두 이 메서드로 처리. */
    Schedule save(Schedule schedule);

    /**
     * 유저의 일정을 기간으로 조회. scheduled_at 오름차순.
     * from/to는 필수 — 기본값(예: 오늘~+7일) 해석은 호출자(usecase/툴 핸들러) 책임.
     * 특이사항: scheduled_at이 [from, to) 범위인 일정만. limit 없음 — 노출 개수 제한은 호출자가.
     */
    List<Schedule> findAllByUserIdInPeriod(Long userId, Instant from, Instant to);

    /**
     * 본인 소유 일정 단건 조회. userId를 쿼리 조건에 포함해 타 유저 일정 접근을 원천 차단.
     * (scheduleId는 LLM 툴 인자로 들어오는 값 — 소유권 검증을 쿼리 레벨에서 강제)
     */
    Optional<Schedule> findByIdAndUserId(Long scheduleId, Long userId);

    /**
     * 본인 소유 일정 soft delete (is_deleted=true, deleted_at=now).
     * 일정 변경은 새 row 추가 + 기존 row soft delete로 처리 — 이 메서드가 후자.
     * userId를 쿼리 조건에 포함해 소유권 검증. 성공(1행 변경) 시 true.
     */
    boolean softDelete(Long scheduleId, Long userId);
}

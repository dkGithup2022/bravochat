package com.chatbot.bravo.service.schedule;

import com.chatbot.bravo.exception.schedule.ScheduleNotFoundException;
import com.chatbot.bravo.infrastructure.schedule.repository.ScheduleRepository;
import com.chatbot.bravo.model.schedule.Schedule;
import com.chatbot.bravo.model.schedule.ScheduleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 일정 쓰기 usecase — 등록/변경/삭제 정책의 유일한 구현. 챗 툴과 일정 API가 공유한다.
 * 변경은 항상 "새 row 추가 + 기존 row soft delete" 교체 방식 — 반환 Schedule의 id가 바뀐다.
 * 소유권은 쿼리 레벨(userId 조건)로 강제된다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleWriter {

    private final ScheduleRepository scheduleRepository;

    /** 신규 등록. turnId null = API 발 생성. */
    @Transactional
    public Schedule create(Long userId, Long turnId, String title, String content,
                           ScheduleType scheduleType, Instant scheduledAt) {
        return scheduleRepository.save(
                Schedule.create(userId, turnId, title, content, scheduleType, scheduledAt));
    }

    /**
     * 변경(교체) — 새 row 저장 후 기존 row soft delete. null 파라미터는 기존 값 유지.
     * turnId는 변경을 일으킨 출처 (챗 경로=현재 턴, API 경로=null).
     */
    @Transactional
    public Schedule replace(Schedule old, Long turnId, String newTitle, String newContent,
                            ScheduleType newType, Instant newAt) {
        Schedule saved = scheduleRepository.save(Schedule.create(
                old.getUserId(), turnId,
                newTitle != null ? newTitle : old.getTitle(),
                newContent != null ? newContent : old.getContent(),
                newType != null ? newType : old.getScheduleType(),
                newAt != null ? newAt : old.getScheduledAt()));
        scheduleRepository.softDelete(old.getScheduleId(), old.getUserId());
        log.info("일정 교체: old={}, new={}, userId={}", old.getScheduleId(), saved.getScheduleId(), old.getUserId());
        return saved;
    }

    /** id 기반 변경(교체) — API 경로. 소유 일정이 아니면 404 (존재 은닉). */
    @Transactional
    public Schedule replaceById(Long userId, Long scheduleId, String newTitle, String newContent,
                                ScheduleType newType, Instant newAt) {
        Schedule old = scheduleRepository.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
        return replace(old, null, newTitle, newContent, newType, newAt);
    }

    /** 삭제(soft delete) — 소유 일정이 아니면 404 (존재 은닉). */
    @Transactional
    public void delete(Long userId, Long scheduleId) {
        if (!scheduleRepository.softDelete(scheduleId, userId)) {
            throw new ScheduleNotFoundException(scheduleId);
        }
    }
}

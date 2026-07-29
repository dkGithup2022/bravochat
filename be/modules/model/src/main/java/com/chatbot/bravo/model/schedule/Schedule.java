package com.chatbot.bravo.model.schedule;

import com.chatbot.bravo.model.audit.AuditFields;
import lombok.Value;

import java.time.Instant;

/**
 * 유저의 일정. 챗 툴 또는 일정 API로 생성 — turnId로 생성 출처 턴을 추적한다
 * (null = API 발 생성). 완료 여부는 doneAt으로 판정 (null = 미완료).
 */
@Value
public class Schedule implements AuditFields {
    Long scheduleId;
    Long userId;
    Long turnId;
    String title;
    String content;          // nullable — 제목만 있는 일정 허용
    ScheduleType scheduleType;
    Instant scheduledAt;     // UTC
    Instant doneAt;          // nullable — null이면 미완료
    Instant createdAt;
    Instant updatedAt;

    /** 신규 일정 생성 — 미완료 상태. */
    public static Schedule create(Long userId, Long turnId, String title, String content,
                                  ScheduleType scheduleType, Instant scheduledAt) {
        Instant now = Instant.now();
        return new Schedule(null, userId, turnId, title, content, scheduleType, scheduledAt,
                null, now, now);
    }

    /** 완료 처리 — doneAt 세팅. */
    public Schedule done() {
        Instant now = Instant.now();
        return new Schedule(scheduleId, userId, turnId, title, content, scheduleType, scheduledAt,
                now, createdAt, now);
    }

    public boolean isDone() {
        return doneAt != null;
    }
}
